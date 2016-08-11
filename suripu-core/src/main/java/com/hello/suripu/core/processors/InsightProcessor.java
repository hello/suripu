package com.hello.suripu.core.processors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.AccountReadDAO;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.DeviceDataDAODynamoDB;
import com.hello.suripu.core.db.DeviceDataInsightQueryDAO;
import com.hello.suripu.core.db.DeviceReadDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.MarketingInsightsSeenDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.insights.InsightsLastSeen;
import com.hello.suripu.core.insights.InsightsLastSeenDAO;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.Insights.InfoInsightCards;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.MarketingInsightsSeen;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.preferences.PreferenceName;
import com.hello.suripu.core.preferences.TemperatureUnit;
import com.hello.suripu.core.processors.insights.BedLightDuration;
import com.hello.suripu.core.processors.insights.BedLightIntensity;
import com.hello.suripu.core.processors.insights.CaffeineAlarm;
import com.hello.suripu.core.processors.insights.GoalsInsights;
import com.hello.suripu.core.processors.insights.Humidity;
import com.hello.suripu.core.processors.insights.IntroductionInsights;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.core.processors.insights.Lights;
import com.hello.suripu.core.processors.insights.MarketingInsights;
import com.hello.suripu.core.processors.insights.Particulates;
import com.hello.suripu.core.processors.insights.PartnerMotionInsight;
import com.hello.suripu.core.processors.insights.SleepAlarm;
import com.hello.suripu.core.processors.insights.SleepDeprivation;
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
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Created by kingshy on 10/24/14
 */
public class InsightProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(InsightProcessor.class);

    private static final int RECENT_DAYS = 7; // last 7 days
    private static final int LAST_TWO_WEEKS = 13; //last 2 weeks
    private static final int NEW_ACCOUNT_THRESHOLD = 4;
    private static final int DAYS_ONE_WEEK = 7;
    private static final int NUM_INSIGHTS_ALLOWED_PER_TWO_WEEK = 4;
    private static final int HIGH_PRIORITY_START_TIME = 13; //1 pm local time
    private static final int HIGH_PRIORITY_END_TIME = 19; //6 pm local time
    private static final int INSIGHT_FREQ_SLEEP_DEPRIVATION = 83; // Max frequency: once every 12 weeks

    private static final Random RANDOM = new Random();

    private final DeviceDataDAODynamoDB deviceDataDAODynamoDB;
    private final DeviceReadDAO deviceReadDAO;
    private final TrendsInsightsDAO trendsInsightsDAO;
    private final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB;
    private final InsightsDAODynamoDB insightsDAODynamoDB;
    private final InsightsLastSeenDAO insightsLastSeenDAO;
    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private final AccountPreferencesDAO preferencesDAO;
    private final LightData lightData;
    private final WakeStdDevData wakeStdDevData;
    private final AccountInfoProcessor accountInfoProcessor;
    private final AccountReadDAO accountReadDAO;
    private final CalibrationDAO calibrationDAO;
    private final MarketingInsightsSeenDAODynamoDB marketingInsightsSeenDAODynamoDB;

    private static final ImmutableSet<InsightCard.Category> marketingInsightPool = ImmutableSet.copyOf(Sets.newHashSet(InsightCard.Category.DRIVE,
            InsightCard.Category.EAT,
            InsightCard.Category.LEARN,
            InsightCard.Category.LOVE,
            InsightCard.Category.PLAY,
            InsightCard.Category.RUN,
            InsightCard.Category.SWIM,
            InsightCard.Category.WORK));

    public InsightProcessor(@NotNull final DeviceDataDAODynamoDB deviceDataDAODynamoDB,
                            @NotNull final DeviceReadDAO deviceReadDAO,
                            @NotNull final TrendsInsightsDAO trendsInsightsDAO,
                            @NotNull final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB,
                            @NotNull final InsightsDAODynamoDB insightsDAODynamoDB,
                            @NotNull final InsightsLastSeenDAO insightsLastSeenDAO,
                            @NotNull final SleepStatsDAODynamoDB sleepStatsDAODynamoDB,
                            @NotNull final AccountPreferencesDAO preferencesDAO,
                            @NotNull final AccountInfoProcessor accountInfoProcessor,
                            @NotNull final AccountReadDAO accountReadDAO,
                            @NotNull final LightData lightData,
                            @NotNull final WakeStdDevData wakeStdDevData,
                            @NotNull final CalibrationDAO calibrationDAO,
                            @NotNull final MarketingInsightsSeenDAODynamoDB marketingInsightsSeenDAODynamoDB
    ) {
        this.deviceDataDAODynamoDB = deviceDataDAODynamoDB;
        this.deviceReadDAO = deviceReadDAO;
        this.trendsInsightsDAO = trendsInsightsDAO;
        this.scoreDAODynamoDB = scoreDAODynamoDB;
        this.insightsDAODynamoDB = insightsDAODynamoDB;
        this.insightsLastSeenDAO = insightsLastSeenDAO;
        this.preferencesDAO = preferencesDAO;
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.lightData = lightData;
        this.wakeStdDevData = wakeStdDevData;
        this.accountInfoProcessor = accountInfoProcessor;
        this.accountReadDAO = accountReadDAO;
        this.calibrationDAO = calibrationDAO;
        this.marketingInsightsSeenDAODynamoDB = marketingInsightsSeenDAODynamoDB;
    }

    public void generateInsights(final Long accountId, final DateTime accountCreated, final RolloutClient featureFlipper) {
        final int accountAge = DateTimeUtil.getDateDiffFromNowInDays(accountCreated);

        if (accountAge < 1) {
            return; // not slept one night yet
        }

        if (accountAge <= NEW_ACCOUNT_THRESHOLD) {
            this.generateNewUserInsights(accountId, accountAge, featureFlipper);
            return;
        }

        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceReadDAO.getMostRecentSensePairByAccountId(accountId);
        if (!deviceAccountPairOptional.isPresent()) {
            return;
        }

        this.generateGeneralInsights(accountId, deviceAccountPairOptional.get(), deviceDataDAODynamoDB, featureFlipper);
        return;
    }

    /**
     * for new users, first 4 days
     */
    private Optional<InsightCard.Category> generateNewUserInsights(final Long accountId, final int accountAge,  final RolloutClient featureFlipper) {
        Map<InsightCard.Category, DateTime> recentCategories;
        if (featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_LAST_SEEN, accountId, Collections.EMPTY_LIST)) {
            final List<InsightsLastSeen> insightsLastSeenList = this.insightsLastSeenDAO.getAll(accountId);
            recentCategories = InsightsLastSeen.getLastSeenInsights(insightsLastSeenList);
        }else {
            recentCategories = this.getRecentInsightsCategories(accountId);
        }
        return generateNewUserInsights(accountId, accountAge, recentCategories);
    }

    @VisibleForTesting
    public Optional<InsightCard.Category> generateNewUserInsights(final Long accountId, final int accountAge, final Map<InsightCard.Category, DateTime> recentCategories) {

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

        if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, card.category, LAST_TWO_WEEKS)) {
            return Optional.absent();
        }

        //insert to DynamoDB
        LOGGER.debug("Inserting {} new user insight for accountId {}", card.category, accountId);
        this.insightsDAODynamoDB.insertInsight(card);
        final InsightsLastSeen newInsight = new InsightsLastSeen(accountId, card.category, DateTime.now(DateTimeZone.UTC));
        this.insightsLastSeenDAO.markLastSeen(newInsight);
        return Optional.of(card.category);
    }

    private Optional<InsightCard.Category> generateGeneralInsights(final Long accountId, final DeviceAccountPair deviceAccountPair, final DeviceDataInsightQueryDAO deviceDataInsightQueryDAO, final RolloutClient featureFlipper) {
        Map<InsightCard.Category, DateTime> recentCategories;
        if (featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_LAST_SEEN, accountId, Collections.EMPTY_LIST)) {
            final List<InsightsLastSeen> insightsLastSeenList = this.insightsLastSeenDAO.getAll(accountId);
            recentCategories  = InsightsLastSeen.getLastSeenInsights(insightsLastSeenList);
        }else {
            recentCategories = this.getRecentInsightsCategories(accountId);
        }
        final DateTime currentTimeUTC = DateTime.now(DateTimeZone.UTC);
        return generateGeneralInsights(accountId, deviceAccountPair, deviceDataInsightQueryDAO, recentCategories, currentTimeUTC, featureFlipper);
    }

    /**
     * logic to determine what kind of insights to generate
     */
    @VisibleForTesting
    public Optional<InsightCard.Category> generateGeneralInsights(final Long accountId, final DeviceAccountPair deviceAccountPair, final DeviceDataInsightQueryDAO deviceDataInsightQueryDAO,
                                                                  final Map<InsightCard.Category, DateTime> recentCategories, final DateTime currentTimeUTC, final RolloutClient featureFlipper) {

        final Optional<Integer> timeZoneOffsetOptional = sleepStatsDAODynamoDB.getTimeZoneOffset(accountId);
        final Integer timeZoneOffset = (timeZoneOffsetOptional.isPresent()) ? timeZoneOffsetOptional.get() : 0; //defaults to utc if no timezone present
        final DateTime currentTimeLocal = currentTimeUTC.plusMillis(timeZoneOffset);

        final Optional<InsightCard.Category> toGenerateWeeklyCategory = selectWeeklyInsightsToGenerate(recentCategories, currentTimeLocal);

        if (toGenerateWeeklyCategory.isPresent()) {
            LOGGER.debug("Trying to generate {} category insight for accountId {}", toGenerateWeeklyCategory.get(), accountId);
            final Optional<InsightCard.Category> generatedWeeklyCategory = this.generateInsightsByCategory(accountId, deviceAccountPair, deviceDataInsightQueryDAO, toGenerateWeeklyCategory.get());
            if (generatedWeeklyCategory.isPresent()) {
                LOGGER.debug("Successfully generated {} category insight for accountId {}", generatedWeeklyCategory.get(), accountId);
                return generatedWeeklyCategory;
            }
            //else try to generate an old Random Insight
        }
        if (InsightsLastSeen.getNumRecentInsights(recentCategories, LAST_TWO_WEEKS) > NUM_INSIGHTS_ALLOWED_PER_TWO_WEEK) {
            return Optional.absent();
        }

        //logic for generating current high-priority Insight
        final Optional<InsightCard.Category> toGenerateHighPriorityCategory = selectHighPriorityInsightToGenerate(accountId, recentCategories, currentTimeLocal, featureFlipper);
        if (toGenerateHighPriorityCategory.isPresent()) {
            LOGGER.debug("Trying to generate {} category insight for accountId {}", toGenerateHighPriorityCategory.get(), accountId);
            final Optional<InsightCard.Category> generatedHighPriorityCategory = this.generateInsightsByCategory(accountId, deviceAccountPair, deviceDataInsightQueryDAO, toGenerateHighPriorityCategory.get());
            if (generatedHighPriorityCategory.isPresent()) {
                LOGGER.debug("Successfully generated {} category insight for accountId {}", generatedHighPriorityCategory.get(), accountId);
                return generatedHighPriorityCategory;
            }
        }

        //logic for generating old random insight
        final Optional<InsightCard.Category> toGenerateRandomCategory = selectRandomOldInsightsToGenerate(accountId, recentCategories, currentTimeUTC, featureFlipper);
        if (toGenerateRandomCategory.isPresent()) {
            LOGGER.debug("Trying to generate {} category insight for accountId {}", toGenerateRandomCategory.get(), accountId);
            final Optional<InsightCard.Category> generatedRandomCategory = this.generateInsightsByCategory(accountId, deviceAccountPair, deviceDataInsightQueryDAO, toGenerateRandomCategory.get());
            if (generatedRandomCategory.isPresent()) {
                LOGGER.debug("Successfully generated {} category insight for accountId {}", generatedRandomCategory.get(), accountId);
                return generatedRandomCategory;
            }
        }

        //Generate random marketing insight here
        final Optional<InsightCard.Category> toGenerateOneTimeCategory;
        if (!featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_MARKETING_SCHEDULE, accountId, Collections.EMPTY_LIST)) {
            toGenerateOneTimeCategory = Optional.absent();
        } else {
            toGenerateOneTimeCategory = selectMarketingInsightToGenerate(accountId, currentTimeUTC);
        }

        if (toGenerateOneTimeCategory.isPresent()) {
            LOGGER.debug("Trying to generate {} category insight for accountId {}", toGenerateOneTimeCategory.get(), accountId);
            final Optional<InsightCard.Category> generatedRandomOneTimeInsight = this.generateInsightsByCategory(accountId, deviceAccountPair, deviceDataInsightQueryDAO, toGenerateOneTimeCategory.get());
            if (generatedRandomOneTimeInsight.isPresent()) {
                LOGGER.debug("Successfully generated {} category insight for accountId {}", generatedRandomOneTimeInsight.get(), accountId);
                return generatedRandomOneTimeInsight;
            }
        }

        return Optional.absent();
    }


    @VisibleForTesting
    public Optional<InsightCard.Category> selectWeeklyInsightsToGenerate(final Map<InsightCard.Category, DateTime> recentCategories, final DateTime currentTimeLocal) {

        //Generate some Insights weekly
        final Integer dayOfWeek = currentTimeLocal.getDayOfWeek();
        LOGGER.debug("The day of week is {}", dayOfWeek);

        switch (dayOfWeek) {
            case 6:
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.WAKE_VARIANCE, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.WAKE_VARIANCE);
        }
        return Optional.absent();
    }

    public Optional<InsightCard.Category> selectHighPriorityInsightToGenerate(final Long accountId, final Map<InsightCard.Category, DateTime> recentCategories, final DateTime currentTimeLocal, final RolloutClient featureFlipper) {
        //Limit insight check time window
        if (currentTimeLocal.getHourOfDay() >= HIGH_PRIORITY_START_TIME && currentTimeLocal.getHourOfDay() <= HIGH_PRIORITY_END_TIME ){
            //SLEEP_DEPRIVATION
            if (featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_SLEEP_DEPRIVATION, accountId, Collections.EMPTY_LIST)){
                //check for eligibility - insight not recently delivered and insight will be generated - avoids over-riding all other insights during window
                if(SleepDeprivation.checkEligiblity(accountId,recentCategories, INSIGHT_FREQ_SLEEP_DEPRIVATION,sleepStatsDAODynamoDB, accountReadDAO, currentTimeLocal)){
                    return Optional.of(InsightCard.Category.SLEEP_DEPRIVATION);
                }
            }
        }

        //TODO: Read category to generate off of an external file to allow for most flexibility
        return Optional.absent();
    }

    private Optional<InsightCard.Category> selectMarketingInsightToGenerate(final Long accountId, final DateTime currentTimeUTC) {
        //Get all historical insight categories
        final Optional<MarketingInsightsSeen> marketingInsightsSeenOptional = marketingInsightsSeenDAODynamoDB.getSeenCategories(accountId);
        if (!marketingInsightsSeenOptional.isPresent()) {
            return selectMarketingInsightToGenerate(currentTimeUTC, new HashSet<InsightCard.Category>(), RANDOM, currentTimeUTC.minusDays(1));
        }

        return selectMarketingInsightToGenerate(currentTimeUTC, marketingInsightsSeenOptional.get().seenCategories, RANDOM, marketingInsightsSeenOptional.get().updated);
    }

    @VisibleForTesting
    public Optional<InsightCard.Category> selectMarketingInsightToGenerate(final DateTime currentTimeUTC, final Set<InsightCard.Category> marketingSeenCategories, final Random random, final DateTime lastUpdate) {
        final DateTime today = currentTimeUTC.withTimeAtStartOfDay(); //currentTime is DateTime.now() - UTC
        final DateTime lastMarketingUpdate = lastUpdate.withTimeAtStartOfDay(); //parameter is updated_utc

        //Already generated marketing insight today. skip
        if (today.isEqual(lastMarketingUpdate)) {
            return Optional.absent();
        }

        final Integer dayOfMonth = currentTimeUTC.getDayOfMonth();
        LOGGER.debug("The day of the month is {}", dayOfMonth);

        //Check date condition
        switch (dayOfMonth) {
            case 1:
            case 4:
            case 7:
            case 10:
            case 13:
            case 16:
            case 19:

                //Pull random insight out of set of allowed marketing insights
                final Optional<InsightCard.Category> pickedRandomInsight = pickRandomInsightCategory(marketingInsightPool, marketingSeenCategories, random);
                return pickedRandomInsight;
        }

        return Optional.absent();
    }

    @VisibleForTesting
    public Optional<InsightCard.Category> pickRandomInsightCategory(final Set<InsightCard.Category> insightPool, final Set<InsightCard.Category> seenPool, final Random random) {
        //For category in seen pool, if it is in insight pool, remove from insight pool
        final Set<InsightCard.Category> allowedPool = Sets.newHashSet();

        for (InsightCard.Category category : insightPool) {
            if (!seenPool.contains(category)) {
                allowedPool.add(category);
            }
        }

        //Pick random category out of allowed pool
        if (allowedPool.isEmpty()) {
            return Optional.absent();
        }

        final InsightCard.Category[] allowedPoolList = allowedPool.toArray(new InsightCard.Category[allowedPool.size()]);
        final Integer randomIndex = random.nextInt(allowedPool.size());

        return Optional.of(allowedPoolList[randomIndex]);
    }

    @VisibleForTesting
    public Optional<InsightCard.Category> selectRandomOldInsightsToGenerate(final Long accountId, final Map<InsightCard.Category, DateTime> recentCategories, final DateTime currentTimeUTC, final RolloutClient featureFlipper) {

        /* randomly select a card that hasn't been generated recently - TODO when we have all categories
        final List<InsightCard.Category> eligibleCatgories = new ArrayList<>();
        for (final InsightCard.Category category : InsightCard.Category.values()) {
            if (!recentCategories.contains(category)) {
                eligibleCategories.add(category);
            }
        }
        */

        //Generate some Insights based on day of month - once every 9 days TODO: randomly generate old Insight on day of week if has not been generated in a while
        final Integer dayOfMonth = currentTimeUTC.getDayOfMonth();
        LOGGER.debug("The day of the month is {}", dayOfMonth);

        switch (dayOfMonth) {
            case 1:
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.HUMIDITY, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.HUMIDITY);
            case 4:
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.BED_LIGHT_DURATION, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.BED_LIGHT_DURATION);
            case 7:
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.BED_LIGHT_INTENSITY_RATIO, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.BED_LIGHT_INTENSITY_RATIO);
            case 10:
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.TEMPERATURE, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.TEMPERATURE);

            case 13:
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.LIGHT, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.LIGHT);
            case 18:
                if (!featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_CAFFEINE, accountId, Collections.EMPTY_LIST)) {
                    return Optional.absent();
                }
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.CAFFEINE, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.CAFFEINE);
            case 19:
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.SLEEP_QUALITY, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.SLEEP_QUALITY);
            case 22:
                if (!featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_SLEEP_TIME, accountId, Collections.EMPTY_LIST)) {
                    return Optional.absent();
                }
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.SLEEP_TIME, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.SLEEP_TIME);
            case 25:
                if (!featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_AIR_QUALITY, accountId, Collections.EMPTY_LIST)) {
                    return Optional.absent();
                }
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.AIR_QUALITY, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.AIR_QUALITY);
        }
        return Optional.absent();
    }

    @VisibleForTesting
    public Optional<InsightCard.Category> generateInsightsByCategory(final Long accountId, final DeviceAccountPair deviceAccountPair, final DeviceDataInsightQueryDAO deviceDataInsightQueryDAO, final InsightCard.Category category) {

        final DateTimeFormatter timeFormat;
        final TemperatureUnit tempUnit;

        Optional<InsightCard> insightCardOptional = Optional.absent();
        switch (category) {
            case AIR_QUALITY:
                insightCardOptional = Particulates.getInsights(accountId, deviceAccountPair, sleepStatsDAODynamoDB, deviceDataInsightQueryDAO, calibrationDAO);
                break;
            case BED_LIGHT_DURATION:
                insightCardOptional = BedLightDuration.getInsights(accountId, deviceAccountPair, deviceDataInsightQueryDAO, sleepStatsDAODynamoDB);
                break;
            case BED_LIGHT_INTENSITY_RATIO:
                insightCardOptional = BedLightIntensity.getInsights(accountId, deviceAccountPair, deviceDataInsightQueryDAO, sleepStatsDAODynamoDB);
                break;
            case CAFFEINE:
                timeFormat = this.getTimeFormat(accountId);
                insightCardOptional = CaffeineAlarm.getInsights(accountInfoProcessor, sleepStatsDAODynamoDB, accountId, timeFormat);
                break;
            case DRIVE:
                insightCardOptional = MarketingInsights.getDriveInsight(accountId);
                break;
            case EAT:
                insightCardOptional = MarketingInsights.getEatInsight(accountId);
                break;
            case GOAL_COFFEE:
                insightCardOptional = GoalsInsights.getCoffeeInsight(accountId);
                break;
            case GOAL_GO_OUTSIDE:
                insightCardOptional = GoalsInsights.getGoOutsideInsight(accountId);
                break;
            case GOAL_SCHEDULE_THOUGHTS:
                insightCardOptional = GoalsInsights.getScheduleThoughtsInsight(accountId);
                break;
            case GOAL_SCREENS:
                insightCardOptional = GoalsInsights.getScreensInsight(accountId);
                break;
            case GOAL_WAKE_VARIANCE:
                insightCardOptional = GoalsInsights.getWakeVarianceInsight(accountId);
                break;
            case HUMIDITY:
                insightCardOptional = Humidity.getInsights(accountId, deviceAccountPair, deviceDataInsightQueryDAO, sleepStatsDAODynamoDB);
                break;
            case LEARN:
                insightCardOptional = MarketingInsights.getLearnInsight(accountId);
                break;
            case LIGHT:
                insightCardOptional = Lights.getInsights(accountId, deviceAccountPair, deviceDataInsightQueryDAO, lightData, sleepStatsDAODynamoDB);
                break;
            case LOVE:
                insightCardOptional = MarketingInsights.getLoveInsight(accountId);
                break;
            case PARTNER_MOTION:
                insightCardOptional = PartnerMotionInsight.getInsights(accountId, deviceReadDAO, sleepStatsDAODynamoDB);
                break;
            case PLAY:
                insightCardOptional = MarketingInsights.getPlayInsight(accountId);
                break;
            case RUN:
                insightCardOptional = MarketingInsights.getRunInsight(accountId);
                break;
            case SLEEP_DEPRIVATION:
                final DateTime queryDate = DateTime.now().withTimeAtStartOfDay();
                insightCardOptional = SleepDeprivation.getInsights(sleepStatsDAODynamoDB, accountReadDAO, accountId, queryDate);
                break;
            case SLEEP_QUALITY:
                insightCardOptional = SleepMotion.getInsights(accountId, sleepStatsDAODynamoDB, false);
                break;
            case SLEEP_SCORE:
                insightCardOptional = MarketingInsights.getMarketingSleepScoreInsight(accountId);
                break;
            case SLEEP_TIME:
                timeFormat = this.getTimeFormat(accountId);
                insightCardOptional = SleepAlarm.getInsights(sleepStatsDAODynamoDB, accountReadDAO, accountId, timeFormat);
                break;
            case SOUND:
                insightCardOptional = SoundDisturbance.getInsights(accountId, deviceAccountPair, deviceDataDAODynamoDB, sleepStatsDAODynamoDB);
                break;
            case SWIM:
                insightCardOptional = MarketingInsights.getSwimInsight(accountId);
                break;
            case TEMPERATURE:
                tempUnit = this.getTemperatureUnitString(accountId);
                insightCardOptional = TemperatureHumidity.getInsights(accountId, deviceAccountPair, deviceDataInsightQueryDAO, tempUnit, sleepStatsDAODynamoDB);
                break;
            case WAKE_VARIANCE:
                final DateTime queryEndDate = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay();
                insightCardOptional = WakeVariance.getInsights(sleepStatsDAODynamoDB, accountId, wakeStdDevData, queryEndDate, DAYS_ONE_WEEK);
                break;
            case WORK:
                insightCardOptional = MarketingInsights.getWorkInsight(accountId);
                break;
        }

        if (insightCardOptional.isPresent()) {
            if (marketingInsightPool.contains(category)) {
                marketingInsightsSeenDAODynamoDB.updateSeenCategories(accountId, category);
            }

            // save to dynamo
            LOGGER.info("action=generated_insight_card category={} accountId={} next_action=insert_into_dynamo", insightCardOptional.get(), accountId);
            this.insightsDAODynamoDB.insertInsight(insightCardOptional.get());
            final InsightsLastSeen newInsight = new InsightsLastSeen(accountId, insightCardOptional.get().category, DateTime.now(DateTimeZone.UTC));
            this.insightsLastSeenDAO.markLastSeen(newInsight);
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

    public Map<InsightCard.Category, DateTime> getRecentInsightsCategories(final Long accountId) {
        // get all insights from the two weeks
        final DateTime twoWeeksAgo = DateTime.now(DateTimeZone.UTC).minusDays(LAST_TWO_WEEKS);
        final Boolean chronological = true;

        final List<InsightCard> cards = this.insightsDAODynamoDB.getInsightsByDate(accountId, twoWeeksAgo, chronological, RECENT_DAYS);

        final Map<InsightCard.Category, DateTime> seenCategories = new HashMap<>();
        for (InsightCard card : cards) {
            // sets all datetime for categories in the time window to now
            seenCategories.put(card.category, DateTime.now(DateTimeZone.UTC));
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

    private DateTimeFormatter getTimeFormat(final Long accountId) {
        final Map<PreferenceName, Boolean> preferences = this.preferencesDAO.get(accountId);
        if (preferences.containsKey(PreferenceName.TIME_TWENTY_FOUR_HOUR)) {
            final Boolean isMilitary = preferences.get(PreferenceName.TIME_TWENTY_FOUR_HOUR);
            if (isMilitary) {
                return DateTimeFormat.forPattern("HH:mm");
            }
        }
        // default is 12-hour time format. USA!
        return DateTimeFormat.forPattern("h:mm aa");
    }

    /**
     * Builder class, too many variables to initialize in the constructor
     */
    public static class Builder {
        private @Nullable DeviceDataDAODynamoDB deviceDataDAODynamoDB;
        private @Nullable DeviceReadDAO deviceReadDAO;
        private @Nullable TrendsInsightsDAO trendsInsightsDAO;
        private @Nullable AggregateSleepScoreDAODynamoDB scoreDAODynamoDB;
        private @Nullable InsightsDAODynamoDB insightsDAODynamoDB;
        private @Nullable InsightsLastSeenDAO insightsLastSeenDAO;
        private @Nullable SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
        private @Nullable AccountPreferencesDAO preferencesDAO;
        private @Nullable AccountReadDAO accountReadDAO;
        private @Nullable LightData lightData;
        private @Nullable WakeStdDevData wakeStdDevData;
        private @Nullable AccountInfoProcessor accountInfoProcessor;
        private @Nullable CalibrationDAO calibrationDAO;
        private @Nullable MarketingInsightsSeenDAODynamoDB marketingInsightsSeenDAODynamoDB;

        public Builder withMarketingInsightsSeenDAO(final MarketingInsightsSeenDAODynamoDB marketingInsightsSeenDAO) {
            this.marketingInsightsSeenDAODynamoDB = marketingInsightsSeenDAO;
            return this;
        }

        public Builder withSenseDAOs(final DeviceDataDAODynamoDB deviceDataDAODynamoDB, final DeviceReadDAO deviceReadDAO) {
            this.deviceReadDAO = deviceReadDAO;
            this.deviceDataDAODynamoDB = deviceDataDAODynamoDB;
            return this;
        }

        public Builder withInsightsDAO(final TrendsInsightsDAO trendsInsightsDAO) {
            this.trendsInsightsDAO = trendsInsightsDAO;
            return this;
        }

        public Builder withDynamoDBDAOs(final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB, final InsightsDAODynamoDB insightsDAODynamoDB, final InsightsLastSeenDAO insightsLastSeenDAO, final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {
            this.scoreDAODynamoDB = scoreDAODynamoDB;
            this.insightsDAODynamoDB = insightsDAODynamoDB;
            this.insightsLastSeenDAO = insightsLastSeenDAO;
            this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
            return this;
        }

        public Builder withPreferencesDAO(final AccountPreferencesDAO preferencesDAO) {
            this.preferencesDAO = preferencesDAO;
            return this;
        }

        public Builder withAccountReadDAO(final AccountReadDAO accountReadDAO) {
            this.accountReadDAO = accountReadDAO;
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
            checkNotNull(deviceReadDAO, "deviceReadDAO can not be null");
            checkNotNull(trendsInsightsDAO, "trendsInsightsDAO can not be null");
            checkNotNull(scoreDAODynamoDB, "scoreDAODynamoDB can not be null");
            checkNotNull(insightsDAODynamoDB, "insightsDAODynamoDB can not be null");
            checkNotNull(insightsLastSeenDAO, "insightsLastSeenDAO can not be null");
            checkNotNull(sleepStatsDAODynamoDB, "sleepStatsDAODynamoDB can not be null");
            checkNotNull(preferencesDAO, "preferencesDAO can not be null");
            checkNotNull(accountInfoProcessor, "accountInfoProcessor can not be null");
            checkNotNull(accountReadDAO, "accountReadDAO can not be null");
            checkNotNull(lightData, "lightData can not be null");
            checkNotNull(wakeStdDevData, "wakeStdDevData cannot be null");
            checkNotNull(calibrationDAO, "calibrationDAO cannot be null");
            checkNotNull(marketingInsightsSeenDAODynamoDB, "marketInsightsSeenDAO cannot be null");

            return new InsightProcessor(deviceDataDAODynamoDB,
                    deviceReadDAO,
                    trendsInsightsDAO,
                    scoreDAODynamoDB,
                    insightsDAODynamoDB,
                    insightsLastSeenDAO,
                    sleepStatsDAODynamoDB,
                    preferencesDAO,
                    accountInfoProcessor,
                    accountReadDAO,
                    lightData,
                    wakeStdDevData,
                    calibrationDAO,
                    marketingInsightsSeenDAODynamoDB);
        }
    }
}

