package com.hello.suripu.core.processors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.DeviceDataDAODynamoDB;
import com.hello.suripu.core.db.DeviceDataInsightQueryDAO;
import com.hello.suripu.core.db.DeviceReadDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.models.AccountInfo;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Insights.InfoInsightCards;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.preferences.PreferenceName;
import com.hello.suripu.core.preferences.TemperatureUnit;
import com.hello.suripu.core.processors.insights.BedLightDuration;
import com.hello.suripu.core.processors.insights.BedLightIntensity;
import com.hello.suripu.core.processors.insights.Humidity;
import com.hello.suripu.core.processors.insights.IntroductionInsights;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.core.processors.insights.Lights;
import com.hello.suripu.core.processors.insights.Particulates;
import com.hello.suripu.core.processors.insights.PartnerMotionInsight;
import com.hello.suripu.core.processors.insights.SleepMotion;
import com.hello.suripu.core.processors.insights.SoundDisturbance;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by kingshy on 10/24/14.
 */
public class InsightProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(InsightProcessor.class);

    private static final int RECENT_DAYS = 7; // last 7 days
    private static final int NEW_ACCOUNT_THRESHOLD = 4;
    private static final int DAYS_ONE_WEEK = 7;
    private static final int NUM_INSIGHTS_ALLOWED_PER_WEEK = 2;

    private final DeviceDataDAO deviceDataDAO;
    private final DeviceDataDAODynamoDB deviceDataDAODynamoDB;
    private final DeviceReadDAO deviceReadDAO;
    private final TrendsInsightsDAO trendsInsightsDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB;
    private final InsightsDAODynamoDB insightsDAODynamoDB;
    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private final AccountPreferencesDAO preferencesDAO;
    private final LightData lightData;
    private final WakeStdDevData wakeStdDevData;
    private final AccountInfoProcessor accountInfoProcessor;
    private final CalibrationDAO calibrationDAO;

    public InsightProcessor(@NotNull final DeviceDataDAO deviceDataDAO,
                            @NotNull final DeviceDataDAODynamoDB deviceDataDAODynamoDB,
                            @NotNull final DeviceReadDAO deviceReadDAO,
                            @NotNull final TrendsInsightsDAO trendsInsightsDAO,
                            @NotNull final TrackerMotionDAO trackerMotionDAO,
                            @NotNull final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB,
                            @NotNull final InsightsDAODynamoDB insightsDAODynamoDB,
                            @NotNull final SleepStatsDAODynamoDB sleepStatsDAODynamoDB,
                            @NotNull final AccountPreferencesDAO preferencesDAO,
                            @NotNull final AccountInfoProcessor accountInfoProcessor,
                            @NotNull final LightData lightData,
                            @NotNull final WakeStdDevData wakeStdDevData,
                            @NotNull final CalibrationDAO calibrationDAO
                            ) {
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDataDAODynamoDB = deviceDataDAODynamoDB;
        this.deviceReadDAO = deviceReadDAO;
        this.trendsInsightsDAO = trendsInsightsDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.scoreDAODynamoDB = scoreDAODynamoDB;
        this.insightsDAODynamoDB = insightsDAODynamoDB;
        this.preferencesDAO = preferencesDAO;
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.lightData = lightData;
        this.wakeStdDevData = wakeStdDevData;
        this.accountInfoProcessor = accountInfoProcessor;
        this.calibrationDAO = calibrationDAO;
    }

    public void generateInsights(final Long accountId, final DateTime accountCreated, final RolloutClient featureFlipper) {
        final int accountAge = DateTimeUtil.getDateDiffFromNowInDays(accountCreated);
        if (accountAge < 1) {
            return; // not slept one night yet
        }

        final Optional<Long> deviceIdOptional = deviceReadDAO.getMostRecentSenseByAccountId(accountId);
        if (!deviceIdOptional.isPresent()) {
            return;
        }

        if (accountAge <= NEW_ACCOUNT_THRESHOLD) {
            this.generateNewUserInsights(accountId, accountAge);
            return;
        }

        final Long internalDeviceId = deviceIdOptional.get();

        final DeviceId deviceId = DeviceId.create(internalDeviceId);

        if (featureFlipper.userFeatureActive(FeatureFlipper.DYNAMODB_DEVICE_DATA_INSIGHTS, accountId, Collections.EMPTY_LIST)) {
            LOGGER.info("Generating insights with DynamoDB for account {}", accountId);
            //This try never succeeds because deviceId was created with internalDeviceId above, and the below requires the externalId type of deviceId
            try {
                this.generateGeneralInsights(accountId, deviceId, deviceDataDAODynamoDB, featureFlipper);
                return;
            } catch (Exception ex) {
                LOGGER.error("Caught exception generating insight for account using DynamoDB {}. {}", accountId, ex);
            }
        }

        this.generateGeneralInsights(accountId, deviceId, deviceDataDAO, featureFlipper);
    }

    /**
     * for new users, first 4 days
     * @param accountId
     * @param accountAge
     */
    private Optional<InsightCard.Category> generateNewUserInsights(final Long accountId, final int accountAge) {
        final Set<InsightCard.Category> recentCategories = this.getRecentInsightsCategories(accountId);
        return generateNewUserInsights(accountId, accountAge, recentCategories);
    }

    @VisibleForTesting
    public Optional<InsightCard.Category> generateNewUserInsights(final Long accountId, final int accountAge, final Set<InsightCard.Category> recentCategories) {

        InsightCard card;
        switch (accountAge) {
            case 1:
                card = IntroductionInsights.getIntroductionCard(accountId);
                break;
            case 2:
                card = IntroductionInsights.getIntroSleepTipsCard(accountId);
                break;
            case 3:
                card = IntroductionInsights.getIntroSleepDurationCard(accountId);
                break;
            default:
                return Optional.absent();
        }

        if (recentCategories.contains(card.category)) {
            return Optional.absent();
        }

        //insert to DynamoDB
        LOGGER.debug("Inserting {} new user insight for accountId {}", card.category, accountId);
        this.insightsDAODynamoDB.insertInsight(card);
        return Optional.of(card.category);
    }

    private Optional<InsightCard.Category> generateGeneralInsights(final Long accountId, final DeviceId deviceId, final DeviceDataInsightQueryDAO deviceDataInsightQueryDAO, final RolloutClient featureFlipper) {
        final Set<InsightCard.Category> recentCategories  = this.getRecentInsightsCategories(accountId);
        final DateTime currentTime = DateTime.now();
        return generateGeneralInsights(accountId, deviceId, deviceDataInsightQueryDAO, recentCategories, currentTime, featureFlipper);
    }

    /**
     * logic to determine what kind of insights to generate
     * @param accountId
     */
    @VisibleForTesting
    public Optional<InsightCard.Category> generateGeneralInsights(final Long accountId, final DeviceId deviceId, final DeviceDataInsightQueryDAO deviceDataInsightQueryDAO,
                                                                  final Set<InsightCard.Category> recentCategories, final DateTime currentTime, final RolloutClient featureFlipper) {

        final Optional<InsightCard.Category> toGenerateWeeklyCategory = selectWeeklyInsightsToGenerate(accountId, recentCategories, currentTime);

        if (toGenerateWeeklyCategory.isPresent()) {
            LOGGER.debug("Trying to generate {} category insight for accountId {}", toGenerateWeeklyCategory.get(), accountId);
            final Optional<InsightCard.Category> generatedWeeklyCategory = this.generateInsightsByCategory(accountId, deviceId, deviceDataInsightQueryDAO, toGenerateWeeklyCategory.get());
            if (generatedWeeklyCategory.isPresent()) {
                LOGGER.debug("Successfully generated {} category insight for accountId {}", generatedWeeklyCategory.get(), accountId);
                return generatedWeeklyCategory;
            }
            //else try to generate an old Random Insight
        }

        if (recentCategories.size() > NUM_INSIGHTS_ALLOWED_PER_WEEK) {
            return Optional.absent();
        }

        //logic for generating current high-priority Insight
        final Optional<InsightCard.Category> toGenerateHighPriorityCategory = selectHighPriorityInsightToGenerate(accountId, recentCategories, currentTime, featureFlipper);
        if (toGenerateHighPriorityCategory.isPresent()) {
            LOGGER.debug("Trying to generate {} category insight for accountId {}", toGenerateHighPriorityCategory.get(), accountId);
            final Optional<InsightCard.Category> generatedHighPriorityCategory = this.generateInsightsByCategory(accountId, deviceId, deviceDataInsightQueryDAO, toGenerateHighPriorityCategory.get());
            if (generatedHighPriorityCategory.isPresent()) {
                LOGGER.debug("Successfully generated {} category insight for accountId {}", generatedHighPriorityCategory.get(), accountId);
                return generatedHighPriorityCategory;
            }
        }

        //logic for generating old random insight
        final Optional<InsightCard.Category> toGenerateRandomCategory = selectRandomOldInsightsToGenerate(accountId, recentCategories, currentTime);
        if (!toGenerateRandomCategory.isPresent()) {
            return Optional.absent();
        }
        LOGGER.debug("Trying to generate {} category insight for accountId {}", toGenerateRandomCategory.get(), accountId);

        final Optional<InsightCard.Category> generatedRandomCategory = this.generateInsightsByCategory(accountId, deviceId, deviceDataInsightQueryDAO, toGenerateRandomCategory.get());
        if (generatedRandomCategory.isPresent()) {
            LOGGER.debug("Successfully generated {} category insight for accountId {}", generatedRandomCategory.get(), accountId);
            return generatedRandomCategory;
        }

        return Optional.absent();
    }


    @VisibleForTesting
    public Optional<InsightCard.Category> selectWeeklyInsightsToGenerate(final Long accountId, final Set<InsightCard.Category> recentCategories, final DateTime currentTime) {

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
    public Optional<InsightCard.Category> selectHighPriorityInsightToGenerate(final Long accountId, final Set<InsightCard.Category> recentCategories, final DateTime currentTime, final RolloutClient featureFlipper) {

        //Generate high priority Insights based on day of month
        final Integer dayOfMonth = currentTime.getDayOfMonth();
        LOGGER.debug("The day of the month is {}", dayOfMonth);

        switch (dayOfMonth) {
            case 1:
                if (!featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_HUMIDITY, accountId, Collections.EMPTY_LIST)) {
                    return Optional.absent();
                }
                if (recentCategories.contains(InsightCard.Category.HUMIDITY)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.HUMIDITY);
            case 14:
                if (!featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_BED_LIGHT_DURATION, accountId, Collections.EMPTY_LIST)) {
                    return Optional.absent();
                }
                if (recentCategories.contains(InsightCard.Category.BED_LIGHT_DURATION)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.BED_LIGHT_DURATION);
            case 21:
                if (!featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_BED_LIGHT_INTENSITY_RATIO, accountId, Collections.EMPTY_LIST)) {
                    return Optional.absent();
                }
                if (recentCategories.contains(InsightCard.Category.BED_LIGHT_INTENSITY_RATIO)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.BED_LIGHT_INTENSITY_RATIO);
            case 29:
                if (!featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_AIR_QUALITY, accountId, Collections.EMPTY_LIST)) {
                    return Optional.absent();
                }
                if (recentCategories.contains(InsightCard.Category.AIR_QUALITY)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.AIR_QUALITY);
            default:
                return Optional.absent();
        }
    }

    @VisibleForTesting
    public Optional<InsightCard.Category> selectRandomOldInsightsToGenerate(final Long accountId, final Set<InsightCard.Category> recentCategories, final DateTime currentTime) {

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
            case 14:
                if (!recentCategories.contains(InsightCard.Category.TEMPERATURE)) {
                    return Optional.of(InsightCard.Category.TEMPERATURE);
                }
                break;
            case 21:
                if (!recentCategories.contains(InsightCard.Category.SLEEP_QUALITY)) {
                    return Optional.of(InsightCard.Category.TEMPERATURE);
                }
                break;
            default:
                return Optional.absent();
        }

        return Optional.absent();
    }

    @VisibleForTesting
    public Optional<InsightCard.Category> generateInsightsByCategory(final Long accountId, final DeviceId deviceId, final DeviceDataInsightQueryDAO deviceDataInsightQueryDAO, final InsightCard.Category category) {

        Optional<InsightCard> insightCardOptional = Optional.absent();
        switch (category) {
            case AIR_QUALITY:
                insightCardOptional = Particulates.getInsights(accountId, deviceId, sleepStatsDAODynamoDB, deviceDataInsightQueryDAO, calibrationDAO);
                break;
            case BED_LIGHT_DURATION:
                insightCardOptional = BedLightDuration.getInsights(accountId, deviceId, deviceDataInsightQueryDAO, sleepStatsDAODynamoDB);
                break;
            case BED_LIGHT_INTENSITY_RATIO:
                insightCardOptional = BedLightIntensity.getInsights(accountId, deviceId, deviceDataInsightQueryDAO, sleepStatsDAODynamoDB);
                break;
            case HUMIDITY:
                insightCardOptional = Humidity.getInsights(accountId, deviceId, deviceDataInsightQueryDAO, sleepStatsDAODynamoDB);
                break;
            case LIGHT:
                insightCardOptional = Lights.getInsights(accountId, deviceId, deviceDataInsightQueryDAO, lightData, sleepStatsDAODynamoDB);
                break;
            case PARTNER_MOTION:
                insightCardOptional = PartnerMotionInsight.getInsights(accountId, deviceReadDAO, sleepStatsDAODynamoDB);
                break;
            case SLEEP_QUALITY:
                insightCardOptional = SleepMotion.getInsights(accountId, deviceId, trendsInsightsDAO, sleepStatsDAODynamoDB, false);
                break;
            case SOUND:
                insightCardOptional = SoundDisturbance.getInsights(accountId, deviceId, deviceDataDAO, sleepStatsDAODynamoDB);
                break;
            case TEMPERATURE:
                final AccountInfo.SleepTempType tempPref = this.accountInfoProcessor.checkTemperaturePreference(accountId);
                final TemperatureUnit tempUnit = this.getTemperatureUnitString(accountId);
                insightCardOptional = TemperatureHumidity.getInsights(accountId, deviceId, deviceDataInsightQueryDAO, tempPref, tempUnit, sleepStatsDAODynamoDB);
                break;
            case WAKE_VARIANCE:
                final DateTime queryEndDate = DateTime.now().withTimeAtStartOfDay();
                insightCardOptional = WakeVariance.getInsights(sleepStatsDAODynamoDB, accountId, wakeStdDevData, queryEndDate, DAYS_ONE_WEEK);
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

    public static Optional<String> getInsightPreviewForCategory(final InsightCard.Category category,
                                                                final TrendsInsightsDAO trendsInsightsDAO)
    {
        final Map<String, String> insightInfoPreview = Maps.newHashMap();
        final ImmutableList<InfoInsightCards> infoInsightCards = trendsInsightsDAO.getAllGenericInsightCards();

        for (final InfoInsightCards card : infoInsightCards) {
            // only grab the first title for a category, if multiple exists
            final String categoryString = card.category.toCategoryString();
            if (!insightInfoPreview.containsKey(categoryString)) {
                insightInfoPreview.put(categoryString, card.title);
            }
        }
        return Optional.fromNullable(insightInfoPreview.get(category.toCategoryString()));
    }

    public Optional<String> getInsightPreviewForCategory(final InsightCard.Category category) {
        return getInsightPreviewForCategory(category, trendsInsightsDAO);
    }

    public static ImmutableMap<InsightCard.Category, String> categoryNames(final TrendsInsightsDAO trendsInsightsDAO) {
        final Map<InsightCard.Category, String> categoryNames = Maps.newHashMap();

        final ImmutableList<InfoInsightCards> infoInsightCards = trendsInsightsDAO.getAllGenericInsightCards();

        for (final InfoInsightCards card : infoInsightCards) {
            categoryNames.put(card.category, card.categoryName);
        }
        return ImmutableMap.copyOf(categoryNames);
    }

    public ImmutableMap<InsightCard.Category, String> categoryNames() {
        return categoryNames(trendsInsightsDAO);
    }

    private Set<InsightCard.Category> getRecentInsightsCategories(final Long accountId) {
        // get all insights from the two weeks
        final DateTime twoWeeksAgo = DateTime.now(DateTimeZone.UTC).minusDays(13);
        final Boolean chronological = true;

        final List<InsightCard> cards = this.insightsDAODynamoDB.getInsightsByDate(accountId, twoWeeksAgo, chronological, RECENT_DAYS);

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
        private @Nullable DeviceDataDAODynamoDB deviceDataDAODynamoDB;
        private @Nullable DeviceReadDAO deviceReadDAO;
        private @Nullable TrendsInsightsDAO trendsInsightsDAO;
        private @Nullable TrackerMotionDAO trackerMotionDAO;
        private @Nullable AggregateSleepScoreDAODynamoDB scoreDAODynamoDB;
        private @Nullable InsightsDAODynamoDB insightsDAODynamoDB;
        private @Nullable SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
        private @Nullable AccountPreferencesDAO preferencesDAO;
        private @Nullable LightData lightData;
        private @Nullable WakeStdDevData wakeStdDevData;
        private @Nullable AccountInfoProcessor accountInfoProcessor;
        private @Nullable CalibrationDAO calibrationDAO;

        public Builder withSenseDAOs(final DeviceDataDAO deviceDataDAO, final DeviceDataDAODynamoDB deviceDataDAODynamoDB, final DeviceReadDAO deviceReadDAO) {
            this.deviceReadDAO = deviceReadDAO;
            this.deviceDataDAO = deviceDataDAO;
            this.deviceDataDAODynamoDB = deviceDataDAODynamoDB;
            return this;
        }

        public Builder withTrackerMotionDAO(final TrackerMotionDAO trackerMotionDAO) {
            this.trackerMotionDAO = trackerMotionDAO;
            return this;
        }

        public Builder withInsightsDAO(final TrendsInsightsDAO trendsInsightsDAO) {
            this.trendsInsightsDAO = trendsInsightsDAO;
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

        public Builder withCalibrationDAO(final CalibrationDAO calibrationDAO) {
            this.calibrationDAO = calibrationDAO;
            return this;
        }

        public InsightProcessor build() {
            checkNotNull(deviceDataDAO, "deviceDataDAO can not be null");
            checkNotNull(deviceReadDAO, "deviceReadDAO can not be null");
            checkNotNull(trendsInsightsDAO, "trendsInsightsDAO can not be null");
            checkNotNull(trackerMotionDAO, "trackerMotionDAO can not be null");
            checkNotNull(scoreDAODynamoDB, "scoreDAODynamoDB can not be null");
            checkNotNull(insightsDAODynamoDB, "insightsDAODynamoDB can not be null");
            checkNotNull(sleepStatsDAODynamoDB, "sleepStatsDAODynamoDB can not be null");
            checkNotNull(preferencesDAO, "preferencesDAO can not be null");
            checkNotNull(accountInfoProcessor, "accountInfoProcessor can not be null");
            checkNotNull(lightData, "lightData can not be null");
            checkNotNull(wakeStdDevData, "wakeStdDevData cannot be null");
            checkNotNull(calibrationDAO, "calibrationDAO cannot be null");

            return new InsightProcessor(deviceDataDAO, deviceDataDAODynamoDB, deviceReadDAO,
                    trendsInsightsDAO,
                    trackerMotionDAO,
                    scoreDAODynamoDB, insightsDAODynamoDB,
                    sleepStatsDAODynamoDB,
                    preferencesDAO,
                    accountInfoProcessor,
                    lightData,
                    wakeStdDevData,
                    calibrationDAO);
        }
    }
}
