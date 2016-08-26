package com.hello.suripu.core.insights;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceReadDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.insights.models.InsightModel;
import com.hello.suripu.core.insights.models.IntroductionInsights;
import com.hello.suripu.core.insights.schedulers.InsightScheduler;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.util.DateTimeUtil;
import com.librato.rollout.RolloutClient;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GlobalScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalScheduler.class);

    private static final int RECENT_DAYS = 7; // last 7 days
    private static final int NEW_ACCOUNT_THRESHOLD = 4;
    private static final int LAST_TWO_WEEKS = 13; //last 2 weeks

    private final InsightFactory factory;
    private final SchedulerFactory schedulerFactory;
    private final InsightsLastSeenDAO insightsLastSeenDAO;
    private final InsightsDAODynamoDB insightsDAODynamoDB;
    private final DeviceReadDAO deviceReadDAO;

    private final RolloutClient featureFlipper;

    private static final int NUM_INSIGHTS_ALLOWED_PER_TWO_WEEK = 4;

    private GlobalScheduler(
            final InsightFactory factory,
            final SchedulerFactory schedulerFactory,
            final InsightsLastSeenDAO insightsLastSeenDAO,
            final InsightsDAODynamoDB insightsDAODynamoDB,
            final DeviceReadDAO deviceReadDAO,
            final RolloutClient featureFlipper) {
        this.factory = factory;
        this.schedulerFactory = schedulerFactory;
        this.insightsLastSeenDAO = insightsLastSeenDAO;
        this.insightsDAODynamoDB = insightsDAODynamoDB;
        this.deviceReadDAO = deviceReadDAO;
        this.featureFlipper = featureFlipper;
    }

    public static GlobalScheduler create(
            final InsightFactory factory,
            final SchedulerFactory schedulerFactory,
            final InsightsLastSeenDAO insightsLastSeenDAO,
            final InsightsDAODynamoDB insightsDAODynamoDB,
            final DeviceReadDAO deviceReadDAO,
            final RolloutClient featureFlipper) {
        return new GlobalScheduler(factory, schedulerFactory, insightsLastSeenDAO, insightsDAODynamoDB, deviceReadDAO, featureFlipper);
    }


    public void process(final InsightProfile insightProfile) {
        final int accountAge = DateTimeUtil.getDateDiffFromNowInDays(insightProfile.accountCreated());

        if (accountAge < 1) {
            return; // not slept one night yet
        }

        if (accountAge <= NEW_ACCOUNT_THRESHOLD) {
            this.generateNewUserInsights(insightProfile.accountId(), accountAge);
            return;
        }

        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceReadDAO.getMostRecentSensePairByAccountId(insightProfile.accountId());
        if (!deviceAccountPairOptional.isPresent()) {
            return;
        }

        this.generateGeneralInsights(insightProfile);
        return;
    }

    /**
     * for new users, first 4 days
     */
    private Optional<InsightCard.Category> generateNewUserInsights(final Long accountId, final int accountAge) {
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

    private Optional<InsightCard.Category> generateGeneralInsights(final InsightProfile insightProfile) {
        Map<InsightCard.Category, DateTime> recentCategories;
        if (featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_LAST_SEEN, insightProfile.pill().accountId, Collections.EMPTY_LIST)) {
            final List<InsightsLastSeen> insightsLastSeenList = this.insightsLastSeenDAO.getAll(insightProfile.pill().accountId);
            recentCategories  = InsightsLastSeen.getLastSeenInsights(insightsLastSeenList);
        }else {
            recentCategories = this.getRecentInsightsCategories(insightProfile.pill().accountId);
        }
        final DateTime currentTime = DateTime.now(DateTimeZone.UTC);
        return generateGeneralInsights(insightProfile, recentCategories, currentTime);
    }

    /**
     * logic to determine what kind of insights to generate
     */
    public Optional<InsightCard.Category> generateGeneralInsights(final InsightProfile insightProfile,
                                                                  final Map<InsightCard.Category, DateTime> recentCategories, final DateTime currentTime) {

        final List<InsightScheduler> schedulers = schedulerFactory.all();
        for(final InsightScheduler scheduler : schedulers) {
            final Optional<InsightCard.Category> categoryOptional = scheduler.schedule(recentCategories, insightProfile);
            if(categoryOptional.isPresent()) {
                final Optional<InsightCard.Category> otherCategory = generateInsightsByCategory(insightProfile, categoryOptional.get());
                if(otherCategory.isPresent()) {
                    return otherCategory;
                }
            }

            if (InsightsLastSeen.getNumRecentInsights(recentCategories, LAST_TWO_WEEKS) > NUM_INSIGHTS_ALLOWED_PER_TWO_WEEK) {
                return Optional.absent();
            }
        }

        return Optional.absent();
    }

    public Optional<InsightCard.Category> generateInsightsByCategory(final InsightProfile insightProfile, final InsightCard.Category category) {

        final InsightModel insightModel = factory.fromCategory(category);
        final Optional<InsightCard> insightCard = insightModel.generate(insightProfile);
        if (insightCard.isPresent()) {
            schedulerFactory.marketingScheduler().update(insightProfile, category);

            // save to dynamo
            LOGGER.info("action=generated_insight_card category={} accountId={} next_action=insert_into_dynamo", insightCard.get(),insightProfile.accountId() );
            this.insightsDAODynamoDB.insertInsight(insightCard.get());
            final InsightsLastSeen newInsight = new InsightsLastSeen(insightProfile.accountId(), insightCard.get().category, insightProfile.utcnow());
            this.insightsLastSeenDAO.markLastSeen(newInsight);
            return Optional.of(category);
        }
        return Optional.absent();
        /*
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
            case SLEEP_QUALITY:
                insightCardOptional = SleepMotion.getInsights(accountId, sleepStatsDAODynamoDB, false);
                break;
            case SLEEP_SCORE:
                insightCardOptional = MarketingInsights.getMarketingSleepScoreInsight(accountId);
                break;
            case SLEEP_TIME:
                final InsightModel sleepAlarm = factory.sleepAlarm();
                insightCardOptional = sleepAlarm.generate(insightProfile);
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
                final InsightModel wakeVariance = factory.wakeVariance();
                insightCardOptional = wakeVariance.generate(insightProfile);
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
        */
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

    /*
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
    */
}

