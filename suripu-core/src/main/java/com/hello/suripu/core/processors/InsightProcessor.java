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
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.preferences.AccountPreference;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.processors.insights.*;
import com.hello.suripu.core.util.DateTimeUtil;
import com.librato.rollout.RolloutClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by kingshy on 10/24/14.
 */
public class InsightProcessor {
    @Inject
    protected RolloutClient featureFlipper;

    private static final Logger LOGGER = LoggerFactory.getLogger(InsightProcessor.class);

    private static final int RECENT_DAYS = 10; // last 10 days
    private static final int NEW_ACCOUNT_THRESHOLD = 2;
    private static final int DAYS_ONE_WEEK = 7;

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
    private final TimelineProcessor timelineProcessor;
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
                            @NotNull final TimelineProcessor timelineProcessor,
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
        this.timelineProcessor = timelineProcessor;
        this.accountInfoProcessor = accountInfoProcessor;
        this.insightInfoPreview = insightInfoPreview;
    }

    public void generateInsights(final Long accountId, final DateTime accountCreated) {
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
            generateNewUserInsights(accountId, deviceId, accountAge);
        } else {
            generateGeneralInsights(accountId, deviceId);
        }
    }

    /**
     * for new users, first 10 days
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
            generateInsightsByCategory(accountId, deviceId, categoryToGenerate);
        }

    }

    /**
     * logic to determine what kind of insights to generate
     * @param accountId
     */
    private void generateGeneralInsights(final Long accountId, final Long deviceId) {

        final Set<InsightCard.Category> recentCategories = this.getRecentInsightsCategories(accountId);

        // randomly select a card that hasn't been generated recently -- TODO when we have all categories
        /*
        final List<InsightCard.Category> eligibleCategories = new ArrayList<>();
        for (final InsightCard.Category category : InsightCard.Category.values()) {
            if (!recentCategories.contains(category)) {
                eligibleCategories.add(category);
            }
        }

        if (eligibleCategories.isEmpty()) {
            LOGGER.debug("No new insights generated: {}", accountId);
            return;
        }

        // for now, we only have these two categories
        if (!recentCategories.contains(InsightCard.Category.LIGHT)) {
            LOGGER.debug("Light has not been generated recently, will now generate by category");
            this.generateInsightsByCategory(accountId, deviceId, InsightCard.Category.LIGHT); //TODO: what's going on with this "this"?
        } else if (!recentCategories.contains(InsightCard.Category.TEMPERATURE)) {
            this.generateInsightsByCategory(accountId, deviceId, InsightCard.Category.TEMPERATURE);
        } else if (!recentCategories.contains(InsightCard.Category.SLEEP_QUALITY)) { //movement
            this.generateInsightsByCategory(accountId, deviceId, InsightCard.Category.SLEEP_QUALITY);
        }
        */

        Integer dayOfWeek = Integer.parseInt(DateTime.now().dayOfWeek().getAsString());
        LOGGER.debug("The day of week is {}", dayOfWeek);
        InsightCard.Category categoryToGenerate;

        switch (dayOfWeek) {
            case 4: //let's make this thursday for now, but I'd like it to be Saturday morning
                if (featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_TESTING, accountId, Collections.EMPTY_LIST)) {
                    LOGGER.debug("setting category to generate as wake variance");
                    categoryToGenerate = InsightCard.Category.WAKE_VARIANCE;
                    break;
            }
                else {
                    return;
                }
            default:
                return;
        }

        if (!recentCategories.contains(categoryToGenerate)) {
            generateInsightsByCategory(accountId, deviceId, categoryToGenerate);
        }

    }

    public void generateInsightsByCategory(final Long accountId, final Long deviceId, final InsightCard.Category category) {

        Optional<InsightCard> insightCardOptional = Optional.absent();

        if (category == InsightCard.Category.LIGHT) {
            insightCardOptional = Lights.getInsights(accountId, deviceId, deviceDataDAO, lightData);

        } else if (category == InsightCard.Category.TEMPERATURE) {
            final AccountInfo.SleepTempType tempPref = this.accountInfoProcessor.checkTemperaturePreference(accountId);
            final AccountPreference.TemperatureUnit tempUnit = this.getTemperatureUnitString(accountId);
            insightCardOptional = TemperatureHumidity.getInsights(accountId, deviceId, deviceDataDAO, tempPref, tempUnit);

        } else if (category == InsightCard.Category.SLEEP_QUALITY) {
            insightCardOptional = SleepMotion.getInsights(accountId, deviceId, trendsInsightsDAO, sleepStatsDAODynamoDB, false);
        } else if (category == InsightCard.Category.WAKE_VARIANCE) {
            DateTime queryEndDate = DateTime.now().withTimeAtStartOfDay();
            TimelineProcessor timelineProcessor = this.timelineProcessor;
            int numDays = DAYS_ONE_WEEK;
            insightCardOptional = WakeVariance.getInsights(timelineProcessor, accountId, wakeStdDevData, queryEndDate, numDays);
        }

        if (insightCardOptional.isPresent()) {
            // save to dynamo
            LOGGER.debug("Insight card present, Inserting insight into DynamoDB");
            this.insightsDAODynamoDB.insertInsight(insightCardOptional.get());
        }
    }

    public Optional<String> getInsightPreviewForCategory(final InsightCard.Category category) {
        return Optional.fromNullable(this.insightInfoPreview.get(category.toCategoryString()));
    }

    private Set<InsightCard.Category> getRecentInsightsCategories(final Long accountId) {
        // get all insights from the past week
        final DateTime aWeekAgo = DateTime.now(DateTimeZone.UTC).minus(7);
        final Boolean chronological = false;

        final List<InsightCard> cards = this.insightsDAODynamoDB.getInsightsByDate(accountId, aWeekAgo, chronological, RECENT_DAYS);

        final Set<InsightCard.Category> seenCategories = new HashSet<>();
        for (InsightCard card : cards) {
            seenCategories.add(card.category);
        }
        return seenCategories;
    }

    private AccountPreference.TemperatureUnit getTemperatureUnitString(final Long accountId) {
        final Map<AccountPreference.EnabledPreference, Boolean> preferences = this.preferencesDAO.get(accountId);
        if (preferences.containsKey(AccountPreference.EnabledPreference.TEMP_CELSIUS)) {
            final Boolean isCelsius = preferences.get(AccountPreference.EnabledPreference.TEMP_CELSIUS);
            if (isCelsius) {
                return AccountPreference.TemperatureUnit.CELSIUS;
            }
        }
        // set default to fahrenheit for now. TODO: Use location
        return AccountPreference.TemperatureUnit.FAHRENHEIT;
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
        private @Nullable TimelineProcessor timelineProcessor;
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

        public Builder withSleepStatsDAODynamoDB(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {
            this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
            return this;
        }

        public Builder withDynamoDBDAOs(final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB, final InsightsDAODynamoDB insightsDAODynamoDB) {
            this.scoreDAODynamoDB = scoreDAODynamoDB;
            this.insightsDAODynamoDB = insightsDAODynamoDB;
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

        public Builder withTimelineProcessor(final TimelineProcessor timelineProcessor) {
            this.timelineProcessor = timelineProcessor;
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
            checkNotNull(timelineProcessor, "timelineProcessor cannot be null");
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
                    timelineProcessor,
                    insightInfoPreview);
        }
    }
}
