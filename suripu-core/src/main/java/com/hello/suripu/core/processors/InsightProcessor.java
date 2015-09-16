package com.hello.suripu.core.processors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
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
    private static final int NEW_ACCOUNT_THRESHOLD = 4;
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

        if (accountAge <= NEW_ACCOUNT_THRESHOLD) {
            this.generateNewUserInsights(accountId, deviceId, accountAge);
            return;
        }

        this.generateGeneralInsights(accountId, deviceId);

    }

    /**
     * for new users, first 7 days
     * @param accountId
     * @param accountAge
     */
    private Optional<InsightCard.Category> generateNewUserInsights(final Long accountId, final Long deviceId, final int accountAge) {

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
                return Optional.absent();
        }

        if (recentCategories.contains(categoryToGenerate)) {
            return Optional.absent();
        }

        LOGGER.debug("Trying to generate {} category insight for new user accountId {}", categoryToGenerate, accountId);
        final Optional<InsightCard.Category> generatedNewUserCategory = this.generateInsightsByCategory(accountId, deviceId, categoryToGenerate);
        if (generatedNewUserCategory.isPresent()) {
            LOGGER.debug("Successfully generated {} category insight for new user accountId {}", generatedNewUserCategory.get(), accountId);
            return generatedNewUserCategory;
        }

        return Optional.absent();
    }

    private Optional<InsightCard.Category> generateGeneralInsights(final Long accountId, final Long deviceId) {
        final Set<InsightCard.Category> recentCategories  = this.getRecentInsightsCategories(accountId);
        final DateTime currentTime = DateTime.now();
        return generateGeneralInsights(accountId, deviceId, recentCategories, currentTime);
    }

    /**
     * logic to determine what kind of insights to generate
     * @param accountId
     */
    @VisibleForTesting
    public Optional<InsightCard.Category> generateGeneralInsights(final Long accountId, final Long deviceId, final Set<InsightCard.Category> recentCategories, final DateTime currentTime) {

        final Optional<InsightCard.Category> toGenerateWeeklyCategory = selectWeeklyInsightsToGenerate(accountId, recentCategories, currentTime);

        if (toGenerateWeeklyCategory.isPresent()) {
            LOGGER.debug("Trying to generate {} category insight for accountId {}", toGenerateWeeklyCategory, accountId);
            final Optional<InsightCard.Category> generatedWeeklyCategory = this.generateInsightsByCategory(accountId, deviceId, toGenerateWeeklyCategory.get());
            if (generatedWeeklyCategory.isPresent()) {
                LOGGER.debug("Successfully generated {} category insight for accountId {}", generatedWeeklyCategory, accountId);
                return generatedWeeklyCategory;
            }
            //else try to generate an old Random Insight
        }

        //logic for generating current Insight
        //TODO: add admin endpoint to replace this function
        //TODO: uncomment below once Bed_Light_Duration Insight gets merged, will also need to change listGeneratedInsightCardCategories()
        /*
        if (recentCategories.size() > NUM_INSIGHTS_ALLOWED_PER_WEEK) {
            return Optional.absent();
        }
        if (featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_BED_LIGHT_DURATION, accountId, Collections.EMPTY_LIST)) {
            if (!recentCategories.contains(InsightCard.Category.BED_LIGHT_DURATION)) {
                LOGGER.debug("generating insight bed light duration for accountId {}", accountId);
                final Optional<InsightCard.Category> generatedBedLightDurationCategory = generateInsightsByCategory(accountId, deviceId, InsightCard.Category.BED_LIGHT_DURATION);
            }
        }
        if (generatedBedLightDurationCategory.isPresent()) {
            return generatedBedLightDurationCategory;
        }
        */

        //If not enough Insight generated recently e.g. does not fulfill conditions of current Insight - generate a random old one
        if (recentCategories.size() > NUM_INSIGHTS_ALLOWED_PER_WEEK) {
            return Optional.absent();
        }

        final Optional<InsightCard.Category> toGenerateRandomCategory = selectRandomOldInsightsToGenerate(accountId, recentCategories, currentTime);
        if (!toGenerateRandomCategory.isPresent()) {
            return Optional.absent();
        }
        LOGGER.debug("Trying to generate {} category insight for accountId {}", toGenerateRandomCategory.get(), accountId);

        final Optional<InsightCard.Category> generatedRandomCategory = this.generateInsightsByCategory(accountId, deviceId, toGenerateRandomCategory.get());
        if (generatedRandomCategory.isPresent()) {
            LOGGER.debug("Successfully generated {} category insight for accountId {}", generatedRandomCategory.get(), accountId);
            return generatedRandomCategory;
        }

        return Optional.absent();
    }


    @VisibleForTesting
    public static Optional<InsightCard.Category> selectWeeklyInsightsToGenerate(final Long accountId, final Set<InsightCard.Category> recentCategories, final DateTime currentTime) {

        //Generate some Insights weekly
        final Integer dayOfWeek = currentTime.getDayOfWeek();
        LOGGER.debug("The day of week is {}", dayOfWeek);

        switch (dayOfWeek) {
            case 6:
                if (recentCategories.contains(InsightCard.Category.WAKE_VARIANCE)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.WAKE_VARIANCE);
        }
        return Optional.absent();
    }

    @VisibleForTesting
    public static Optional<InsightCard.Category> selectRandomOldInsightsToGenerate(final Long accountId, final Set<InsightCard.Category> recentCategories, final DateTime currentTime) {

        /* randomly select a card that hasn't been generated recently - TODO when we have all categories
        final List<InsightCard.Category> eligibleCatgories = new ArrayList<>();
        for (final InsightCard.Category category : InsightCard.Category.values()) {
            if (!recentCategories.contains(category)) {
                eligibleCategories.add(category);
            }
        }
        */

        //Generate some Insights based on day of month - once every 9 days TODO: randomly generate old Insight on day of week if has not been generated in a while
        final Integer dayOfMonth = currentTime.getDayOfMonth();
        LOGGER.debug("The day of the month is {}", dayOfMonth);

        switch (dayOfMonth) {
            case 1:
                if (!recentCategories.contains(InsightCard.Category.LIGHT)) {
                    return Optional.of(InsightCard.Category.LIGHT);
                }
                break;
            case 10:
                if (!recentCategories.contains(InsightCard.Category.TEMPERATURE)) {
                    return Optional.of(InsightCard.Category.TEMPERATURE);
                }
                break;
            case 19:
                if (!recentCategories.contains(InsightCard.Category.SLEEP_QUALITY)) {
                    return Optional.of(InsightCard.Category.TEMPERATURE);
                }
                break;
            /* TODO: uncomment me once bed_light_duration is sufficiently tested to make it generate randomly
            case 25:
                if (!recentCategories.contains(InsightCard.Category.BED_LIGHT_DURATION)) {
                    return Optional.of(InsightCard.Category.BED_LIGHT_DURATION);
                }
                break;
                */
            default:
                return Optional.absent();
        }

        return Optional.absent();
    }

    @VisibleForTesting
    public Optional<InsightCard.Category> generateInsightsByCategory(final Long accountId, final Long deviceId, final InsightCard.Category category) {

        Optional<InsightCard> insightCardOptional = Optional.absent();
        switch (category.toString()) {
            case "LIGHT":
                insightCardOptional = Lights.getInsights(accountId, deviceId, deviceDataDAO, lightData);
                break;
            case "TEMPERATURE":
                final AccountInfo.SleepTempType tempPref = this.accountInfoProcessor.checkTemperaturePreference(accountId);
                final TemperatureUnit tempUnit = this.getTemperatureUnitString(accountId);
                insightCardOptional = TemperatureHumidity.getInsights(accountId, deviceId, deviceDataDAO, tempPref, tempUnit);
                break;
            case "SLEEP_QUALITY":
                insightCardOptional = SleepMotion.getInsights(accountId, deviceId, trendsInsightsDAO, sleepStatsDAODynamoDB, false);
                break;
            case "WAKE_VARIANCE":
                final DateTime queryEndDate = DateTime.now().withTimeAtStartOfDay();
                insightCardOptional = WakeVariance.getInsights(sleepStatsDAODynamoDB, accountId, wakeStdDevData, queryEndDate, DAYS_ONE_WEEK);
                break;
            case "BED_LIGHT_DURATION":
                insightCardOptional = BedLightDuration.getInsights(accountId, deviceId, deviceDataDAO, sleepStatsDAODynamoDB);
                break;
        }

        if (insightCardOptional.isPresent()) {
            // save to dynamo
            LOGGER.debug("Successfully generated {} category insight card for accountId {}, now inserting into DynamoDB", insightCardOptional.get(), accountId);
            this.insightsDAODynamoDB.insertInsight(insightCardOptional.get());
            return Optional.of(category);
        }

        return Optional.absent();
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
        private @Nullable
        Map<String, String> insightInfoPreview;

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

        public Builder withDynamoDBDAOs(final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB, final InsightsDAODynamoDB insightsDAODynamoDB, final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {
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

            return new InsightProcessor(deviceDataDAO, deviceDAO,
                    trendsInsightsDAO,
                    trackerMotionDAO,
                    scoreDAODynamoDB, insightsDAODynamoDB,
                    sleepStatsDAODynamoDB,
                    preferencesDAO,
                    accountInfoProcessor,
                    lightData,
                    wakeStdDevData,
                    insightInfoPreview);
        }
    }
}
