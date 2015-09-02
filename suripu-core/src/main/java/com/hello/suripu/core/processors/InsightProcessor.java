package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.models.AccountInfo;
import com.hello.suripu.core.models.Insights.InfoInsightCards;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;

import com.hello.suripu.core.preferences.PreferenceName;
import com.hello.suripu.core.preferences.TemperatureUnit;
import com.hello.suripu.core.processors.insights.BedLightDuration;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.core.processors.insights.Lights;
import com.hello.suripu.core.processors.insights.SleepMotion;
import com.hello.suripu.core.processors.insights.TemperatureHumidity;
import com.hello.suripu.core.processors.insights.WakeStdDevData;
import com.hello.suripu.core.processors.insights.WakeVariance;
import com.hello.suripu.core.util.DateTimeUtil;
import com.librato.rollout.RolloutClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by kingshy on 10/24/14.
 */
public class InsightProcessor {

    private RolloutClient featureFlipper;

    private static final Logger LOGGER = LoggerFactory.getLogger(InsightProcessor.class);

    private static final int RECENT_DAYS = 7; // last 7 days
    private static final int NEW_ACCOUNT_THRESHOLD = 2;
    private static final int DAYS_ONE_WEEK = 7;
    private static final int NUM_INSIGHTS_ALLOWED_PER_WEEK = 2;

    private final DeviceDataDAO deviceDataDAO;
    private final DeviceDAO deviceDAO;
    private final TrendsInsightsDAO trendsInsightsDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB;
    private final InsightsDAODynamoDB insightsDAODynamoDB;
    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private final AccountPreferencesDAO preferencesDAO;
    private final LightData lightData;
    private final WakeStdDevData wakeStdDevData;
    private final AccountInfoProcessor accountInfoProcessor;
    private final Map<String, String> insightInfoPreview;

    public InsightProcessor(@NotNull final DeviceDataDAO deviceDataDAO,
                            @NotNull final DeviceDAO deviceDAO,
                            @NotNull final TrendsInsightsDAO trendsInsightsDAO,
                            @NotNull final TrackerMotionDAO trackerMotionDAO,
                            @NotNull final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB,
                            @NotNull final InsightsDAODynamoDB insightsDAODynamoDB,
                            @NotNull final SleepStatsDAODynamoDB sleepStatsDAODynamoDB,
                            @NotNull final AccountPreferencesDAO preferencesDAO,
                            @NotNull final AccountInfoProcessor accountInfoProcessor,
                            @NotNull final LightData lightData,
                            @NotNull final WakeStdDevData wakeStdDevData,
                            @NotNull final Map<String, String> insightInfoPreview
                            ) {
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;
        this.trendsInsightsDAO = trendsInsightsDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.scoreDAODynamoDB = scoreDAODynamoDB;
        this.insightsDAODynamoDB = insightsDAODynamoDB;
        this.preferencesDAO = preferencesDAO;
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.lightData = lightData;
        this.wakeStdDevData = wakeStdDevData;
        this.accountInfoProcessor = accountInfoProcessor;
        this.insightInfoPreview = insightInfoPreview;
    }

    public void generateInsights(final Long accountId, final DateTime accountCreated, final RolloutClient featureFlipper) {
        this.featureFlipper = featureFlipper;
        final int accountAge = DateTimeUtil.getDateDiffFromNowInDays(accountCreated);
        if (accountAge < 1) {
            return; // not slept one night yet
        }

        final Optional<Long> deviceIdOptional = deviceDAO.getMostRecentSenseByAccountId(accountId);
        if (!deviceIdOptional.isPresent()) {
            return;
        }

        final Long deviceId = deviceIdOptional.get();

        //generate insight for test users - completely controlled by FF on/off
        if (featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_TESTING, accountId, Collections.EMPTY_LIST)) {
            generateTestInsights(accountId, deviceId);
        }

        //generate insight for all users
        if (accountAge <= NEW_ACCOUNT_THRESHOLD) {
            generateNewUserInsights(accountId, deviceId, accountAge);
        } else {
            generateGeneralWeeklyInsights(accountId, deviceId);
            generateGeneralInsights(accountId, deviceId);
        }
    }

    /**
     * for new users, first 7 days
     * @param accountId
     * @param accountAge
     */
    private void generateNewUserInsights(final Long accountId, final Long deviceId, final int accountAge) {

        final Set<InsightCard.Category> recentCategories = this.getRecentInsightsCategories(accountId);

        InsightCard.Category categoryToGenerate;
        switch (accountAge) {
            case 1:
                categoryToGenerate = InsightCard.Category.LIGHT;
                break;
            case 2:
                categoryToGenerate = InsightCard.Category.TEMPERATURE;
                break;
            case 3:
                categoryToGenerate = InsightCard.Category.SOUND;
                break;
            case 4:
                categoryToGenerate = InsightCard.Category.SLEEP_QUALITY;
                break;
            default:
                return; // TODO: rm debug, lights insight is all we have
        }

        if (!recentCategories.contains(categoryToGenerate)) {
            LOGGER.debug("Generating NEW user category {} insight for account id {}", categoryToGenerate, accountId);
            generateInsightsByCategory(accountId, deviceId, categoryToGenerate);
        }

    }

    private void generateGeneralWeeklyInsights(final Long accountId, final Long deviceId) {
        //Generate some Insights weekly
        final Integer dayOfWeek = DateTime.now().getDayOfWeek();
        LOGGER.debug("The day of week is {}", dayOfWeek);

        switch (dayOfWeek) {
            case 6:
                if (!featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_WAKE_VARIANCE, accountId, Collections.EMPTY_LIST)) {
                    return;
                }
                else if (!generateWeeklyFeatureAllowed(accountId, InsightCard.Category.WAKE_VARIANCE)) {
                    return;
                }
                LOGGER.debug("generating insight wake variance for accountid {}", accountId);
                this.generateInsightsByCategory(accountId, deviceId, InsightCard.Category.WAKE_VARIANCE);
                break;
            default:
                return;
        }
    }

    /**
     * logic to determine what kind of insights to generate
     * @param accountId
     */
    private void generateGeneralInsights(final Long accountId, final Long deviceId) {
        final Set<InsightCard.Category> recentCategories = this.getRecentInsightsCategories(accountId);
        if (recentCategories.size() >= NUM_INSIGHTS_ALLOWED_PER_WEEK) {
            return;
        }

        //logic for generating current Insight
        if (featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_BED_LIGHT_DURATION, accountId, Collections.EMPTY_LIST)) {
            if (!recentCategories.contains(InsightCard.Category.BED_LIGHT_DURATION)) {
                LOGGER.debug("generating insight bed light duration for accountid {}", accountId);
                this.generateInsightsByCategory(accountId, deviceId, InsightCard.Category.BED_LIGHT_DURATION);
                final Integer numCategoriesGeneratedLastWeek = recentCategories.size() + 1;
                generateRandomOldInsights(accountId, deviceId, numCategoriesGeneratedLastWeek);
                return;
            }
        }

        generateRandomOldInsights(accountId, deviceId, recentCategories.size());
    }

    private void generateRandomOldInsights(final Long accountId, final Long deviceId, final Integer numCategoriesGeneratedLastWeek) {
        if (numCategoriesGeneratedLastWeek >= NUM_INSIGHTS_ALLOWED_PER_WEEK) {
            return;
        }

        final Set<InsightCard.Category> recentCategories = this.getRecentInsightsCategories(accountId);

        /* randomly select a card that hasn't been generated recently - TODO when we have all categories
        final List<InsightCard.Category> eligibleCatgories = new ArrayList<>();
        for (final InsightCard.Category category : InsightCard.Category.values()) {
            if (!recentCategories.contains(category)) {
                eligibleCategories.add(category);
            }
        }
        */

        //Generate some Insights based on day of month - once every 9 days TODO: randomly generate old Insight on day of week if has not been generated in a while

        final Integer dayOfMonth = DateTime.now().getDayOfMonth();
        LOGGER.debug("The day of the month is {}", dayOfMonth);

        if (dayOfMonth == 1) {
            if (!recentCategories.contains(InsightCard.Category.LIGHT)) {
                LOGGER.debug("generating insight light for accountid {}", accountId);
                this.generateInsightsByCategory(accountId, deviceId, InsightCard.Category.LIGHT);
            }
        }
        else if (dayOfMonth == 10) {
            if (!recentCategories.contains(InsightCard.Category.TEMPERATURE)) {
                LOGGER.debug("generating insight temperature for accountid {}", accountId);
                this.generateInsightsByCategory(accountId, deviceId, InsightCard.Category.TEMPERATURE);
            }
        }
        else if (dayOfMonth == 19) {
            if (!recentCategories.contains(InsightCard.Category.SLEEP_QUALITY)) {
                LOGGER.debug("generating insight sleep quality (movement) for accountid {}", accountId);
                this.generateInsightsByCategory(accountId, deviceId, InsightCard.Category.SLEEP_QUALITY);
            }
        }
    }

    private void generateTestInsights(final Long accountId, final Long deviceId) {
        //Allows for generation of Insight even if it has been recently generated
        LOGGER.debug("generating insight bed light duration for accountid {}", accountId);
        this.generateInsightsByCategory(accountId, deviceId, InsightCard.Category.BED_LIGHT_DURATION);
    }

    public void generateInsightsByCategory(final Long accountId, final Long deviceId, final InsightCard.Category category) {

        Optional<InsightCard> insightCardOptional = Optional.absent();

        if (category == InsightCard.Category.LIGHT) {
            LOGGER.debug("for light calling getInsights with accountId {}", accountId);
            insightCardOptional = Lights.getInsights(accountId, deviceId, deviceDataDAO, lightData);
        }

        else if (category == InsightCard.Category.TEMPERATURE) {
            final AccountInfo.SleepTempType tempPref = this.accountInfoProcessor.checkTemperaturePreference(accountId);
            final TemperatureUnit tempUnit = this.getTemperatureUnitString(accountId);
            LOGGER.debug("for temperature calling getInsights with accountId {}", accountId);
            insightCardOptional = TemperatureHumidity.getInsights(accountId, deviceId, deviceDataDAO, tempPref, tempUnit);
        }

        else if (category == InsightCard.Category.SLEEP_QUALITY) {
            LOGGER.debug("for sleep quality (movement) calling getInsights with accountId {}", accountId);
            insightCardOptional = SleepMotion.getInsights(accountId, deviceId, trendsInsightsDAO, sleepStatsDAODynamoDB, false);
        }

        else if (category == InsightCard.Category.WAKE_VARIANCE) {
            final DateTime queryEndDate = DateTime.now().withTimeAtStartOfDay();
            LOGGER.debug("for wake variance calling getInsights with accountId {} and date {} with numDays {}", accountId, queryEndDate, DAYS_ONE_WEEK);
            insightCardOptional = WakeVariance.getInsights(sleepStatsDAODynamoDB, accountId, wakeStdDevData, queryEndDate, DAYS_ONE_WEEK);
        }

        else if (category == InsightCard.Category.BED_LIGHT_DURATION) {
            LOGGER.debug("for bed light duration calling getInsights with accountId {}", accountId);
            insightCardOptional = BedLightDuration.getInsights(accountId, deviceId, deviceDataDAO, sleepStatsDAODynamoDB);
        }


        if (insightCardOptional.isPresent()) {
            // save to dynamo
            LOGGER.debug("Category {} insight card present for account {}, Inserting insight into DynamoDB", category, accountId);
            this.insightsDAODynamoDB.insertInsight(insightCardOptional.get());
        }
    }

    public Boolean generateWeeklyFeatureAllowed(final Long accountId, final InsightCard.Category insightCategory) {
        final Set<InsightCard.Category> recentCategories = this.getRecentInsightsCategories(accountId);

        if (recentCategories.contains(insightCategory)) {
            LOGGER.debug("Accountid {} already generated insight {} in past week. Now not generating new Insight", accountId, insightCategory);
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    public Optional<String> getInsightPreviewForCategory(final InsightCard.Category category) {
        return Optional.fromNullable(this.insightInfoPreview.get(category.toCategoryString()));
    }

    private Set<InsightCard.Category> getRecentInsightsCategories(final Long accountId) {
        // get all insights from the past week
        final DateTime aWeekAgo = DateTime.now(DateTimeZone.UTC).minusDays(6);
        final Boolean chronological = true;

        final List<InsightCard> cards = this.insightsDAODynamoDB.getInsightsByDate(accountId, aWeekAgo, chronological, RECENT_DAYS);

        final Set<InsightCard.Category> seenCategories = new HashSet<>();
        for (InsightCard card : cards) {
            seenCategories.add(card.category);
        }
        return seenCategories;
    }

    private TemperatureUnit getTemperatureUnitString(final Long accountId) {
        final Map<PreferenceName, Boolean> preferences = this.preferencesDAO.get(accountId);
        if (preferences.containsKey(PreferenceName.TEMP_CELSIUS)) {
            final Boolean isCelsius = preferences.get(PreferenceName.TEMP_CELSIUS);
            if (isCelsius) {
                return TemperatureUnit.CELSIUS;
            }
        }
        // set default to fahrenheit for now. TODO: Use location
        return TemperatureUnit.FAHRENHEIT;
    }

    /**
     * Builder class, too many variables to initialize in the constructor
     */
    public static class Builder {
        private @Nullable DeviceDataDAO deviceDataDAO;
        private @Nullable DeviceDAO deviceDAO;
        private @Nullable TrendsInsightsDAO trendsInsightsDAO;
        private @Nullable TrackerMotionDAO trackerMotionDAO;
        private @Nullable AggregateSleepScoreDAODynamoDB scoreDAODynamoDB;
        private @Nullable InsightsDAODynamoDB insightsDAODynamoDB;
        private @Nullable SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
        private @Nullable AccountPreferencesDAO preferencesDAO;
        private @Nullable LightData lightData;
        private @Nullable WakeStdDevData wakeStdDevData;
        private @Nullable AccountInfoProcessor accountInfoProcessor;
        private @Nullable Map<String, String> insightInfoPreview;

        public Builder withSenseDAOs(final DeviceDataDAO deviceDataDAO, final DeviceDAO deviceDAO) {
            this.deviceDAO = deviceDAO;
            this.deviceDataDAO = deviceDataDAO;
            return this;
        }

        public Builder withTrackerMotionDAO(final TrackerMotionDAO trackerMotionDAO) {
            this.trackerMotionDAO = trackerMotionDAO;
            return this;
        }

        public Builder withInsightsDAO(final TrendsInsightsDAO trendsInsightsDAO) {
            this.trendsInsightsDAO = trendsInsightsDAO;
            this.insightInfoPreview = new HashMap<>();

            final ImmutableList<InfoInsightCards> infoInsightCards = trendsInsightsDAO.getAllGenericInsightCards();
            for (InfoInsightCards card : infoInsightCards) {
                // only grab the first title for a category, if multiple exists
                final String categoryString = card.category.toCategoryString();
                if (!this.insightInfoPreview.containsKey(categoryString)) {
                    this.insightInfoPreview.put(categoryString, card.title);
                }
            }

            return this;
        }

        public Builder withDynamoDBDAOs(final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB,
                                        final InsightsDAODynamoDB insightsDAODynamoDB,
                                        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {
            this.scoreDAODynamoDB = scoreDAODynamoDB;
            this.insightsDAODynamoDB = insightsDAODynamoDB;
            this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
            return this;
        }

        public Builder withPreferencesDAO(final AccountPreferencesDAO preferencesDAO) {
            this.preferencesDAO = preferencesDAO;
            return this;
        }

        public Builder withAccountInfoProcessor(final AccountInfoProcessor processor) {
            this.accountInfoProcessor = processor;
            return this;
        }

        public Builder withLightData(final LightData lightData) {
            this.lightData = lightData;
            return this;
        }

        public Builder withWakeStdDevData(final WakeStdDevData wakeStdDevData) {
            this.wakeStdDevData = wakeStdDevData;
            return this;
        }

        public InsightProcessor build() {
            checkNotNull(deviceDataDAO, "deviceDataDAO can not be null");
            checkNotNull(deviceDAO, "deviceDAO can not be null");
            checkNotNull(trendsInsightsDAO, "trendsInsightsDAO can not be null");
            checkNotNull(trackerMotionDAO, "trackerMotionDAO can not be null");
            checkNotNull(scoreDAODynamoDB, "scoreDAODynamoDB can not be null");
            checkNotNull(insightsDAODynamoDB, "insightsDAODynamoDB can not be null");
            checkNotNull(sleepStatsDAODynamoDB, "sleepStatsDAODynamoDB can not be null");
            checkNotNull(preferencesDAO, "preferencesDAO can not be null");
            checkNotNull(accountInfoProcessor, "accountInfoProcessor can not be null");
            checkNotNull(lightData, "lightData can not be null");
            checkNotNull(wakeStdDevData, "wakeStdDevData cannot be null");
            checkNotNull(insightInfoPreview, "insight info preview can not be null");

            return new InsightProcessor(deviceDataDAO,
                    deviceDAO,
                    trendsInsightsDAO,
                    trackerMotionDAO,
                    scoreDAODynamoDB,
                    insightsDAODynamoDB,
                    sleepStatsDAODynamoDB,
                    preferencesDAO,
                    accountInfoProcessor,
                    lightData,
                    wakeStdDevData,
                    insightInfoPreview);
        }
    }
}
