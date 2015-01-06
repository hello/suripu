package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Insights.GenericInsightCards;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.core.processors.insights.Lights;
import com.hello.suripu.core.processors.insights.TemperatureHumidity;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by kingshy on 10/24/14.
 */
public class InsightProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(InsightProcessor.class);

    private final DeviceDataDAO deviceDataDAO;
    private final DeviceDAO deviceDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB;
    private final InsightsDAODynamoDB insightsDAODynamoDB;
    private final LightData lightData;

    public InsightProcessor(final DeviceDataDAO deviceDataDAO,
                            final DeviceDAO deviceDAO,
                            final TrackerMotionDAO trackerMotionDAO,
                            final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB,
                            final InsightsDAODynamoDB insightsDAODynamoDB,
                            final LightData lightData) {
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.scoreDAODynamoDB = scoreDAODynamoDB;
        this.insightsDAODynamoDB = insightsDAODynamoDB;
        this.lightData = lightData;
    }

    @Timed
    public static List<InsightCard> getInsights(final Long accountId) {
        Random rnd = new Random();
        LOGGER.debug("Generating insights for account {}", accountId);
        final DateTime createdUTC = new DateTime(DateTime.now(), DateTimeZone.UTC).withSecondOfMinute(0).withMillisOfSecond(0);
        final DateTime localUTC = createdUTC.withTimeAtStartOfDay();
        final int offset = -28800000;

        final List<InsightCard> generated = new ArrayList<>();
        final List<InsightCard> availableInsights = new ArrayList<>();
        final List<GenericInsightCards> genericInsightCards = Collections.emptyList();

        availableInsights.add(new InsightCard(accountId, "Lately", "[placeholder] You've been going to bed way **too late**.", InsightCard.Category.SLEEP_TIME, InsightCard.TimePeriod.RECENTLY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(accountId, "Lately", "[placeholder] Your sleep has improved since you started working out.\n\nGood job!", InsightCard.Category.WORKOUT, InsightCard.TimePeriod.RECENTLY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(accountId, "Lately", "[placeholder] You've been drinking **too much** coffee, it's affecting your sleep.", InsightCard.Category.CAFFEINE, InsightCard.TimePeriod.RECENTLY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(accountId, "Lately", "[placeholder] You're not drinking enough juice, it's affecting your sleep.", InsightCard.Category.DIET, InsightCard.TimePeriod.RECENTLY, createdUTC, genericInsightCards));
        int pick = rnd.nextInt(4);
        generated.add(availableInsights.get(pick));

        availableInsights.add(new InsightCard(accountId, "Last night", "[placeholder] You slept soundly for **4 hours**.", InsightCard.Category.SLEEP_DURATION, InsightCard.TimePeriod.DAILY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(accountId, "Last night", "[placeholder] You fell asleep quicker than usual.", InsightCard.Category.SLEEP_TIME, InsightCard.TimePeriod.DAILY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(accountId, "Last night", "[placeholder] You went to bed **1 hour later** than usual", InsightCard.Category.SLEEP_TIME, InsightCard.TimePeriod.DAILY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(accountId, "Last night", "[placeholder] Your sleep was more agitated than usual.", InsightCard.Category.SLEEP_QUALITY, InsightCard.TimePeriod.DAILY, createdUTC, genericInsightCards));
        pick = rnd.nextInt(4) + 4;
        generated.add(availableInsights.get(pick));

        availableInsights.add(new InsightCard(accountId, "This week", "[placeholder] You've been sleeping one hour less on average.", InsightCard.Category.SLEEP_DURATION, InsightCard.TimePeriod.WEEKLY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(accountId, "This week", "[placeholder] You've been sleeping earlier than usual.", InsightCard.Category.SLEEP_TIME, InsightCard.TimePeriod.WEEKLY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(accountId, "This week", "[placeholder] You've been getting up 30 minutes earlier than usual.", InsightCard.Category.WAKE_TIME, InsightCard.TimePeriod.WEEKLY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(accountId, "This week", "[placeholder] Your sleep score is 10% lower than last week's.", InsightCard.Category.SLEEP_QUALITY, InsightCard.TimePeriod.WEEKLY, createdUTC, genericInsightCards));
        pick = rnd.nextInt(4) + 8;
        generated.add(availableInsights.get(pick));

        return generated;
    }

    public void generateInsights(final Account account) {
        final int accountAge = this.getAccountAgeInDays(account.created);
        if (accountAge < 1) {
            return; // not slept one night yet
        }

        final Long accountId = account.id.get();
        final Optional<Long> deviceIdOptional = deviceDAO.getMostRecentSenseByAccountId(accountId);
        if (!deviceIdOptional.isPresent()) {
            return;
        }

        final Long deviceId = deviceIdOptional.get();

        if (accountAge <= 10) {
            generateNewUserInsights(accountId, deviceId, accountAge);
        } else {
            generateGeneralInsights(accountId, deviceId);
        }
    }

    public void generateInsights(final Account account, final InsightCard.Category category) {
        final Long accountId = account.id.get();
        final Optional<Long> deviceIdOptional = deviceDAO.getMostRecentSenseByAccountId(accountId);
        if (!deviceIdOptional.isPresent()) {
            return;
        }

        final Long deviceId = deviceIdOptional.get();

        Optional<InsightCard> insightCardOptional = Optional.absent();
        if (category == InsightCard.Category.LIGHT) {
            insightCardOptional = Lights.getInsights(accountId, deviceId, deviceDataDAO, lightData);
        }

        if (insightCardOptional.isPresent()) {
            // save to dynamo
            this.insightsDAODynamoDB.insertInsight(insightCardOptional.get());
        }
    }
    /**
     * for new users, first 10 days
     * @param accountId
     * @param accountAge
     */
    private void generateNewUserInsights(final Long accountId, final Long deviceId, final int accountAge) {

        Optional<InsightCard> insightCardOptional = Optional.absent();
        switch (accountAge) {
            case 1:
                insightCardOptional = Lights.getInsights(accountId, deviceId, deviceDataDAO, lightData);
                break;
            case 2:
                insightCardOptional = TemperatureHumidity.getInsights(accountId, deviceDAO, deviceDataDAO);
                break;
            default:
                insightCardOptional = Lights.getInsights(accountId, deviceId, deviceDataDAO, lightData);
                break; // TODO: rm debug, lights insight is all we have
        }

        if (insightCardOptional.isPresent()) {
            // save to dynamo
            this.insightsDAODynamoDB.insertInsight(insightCardOptional.get());
        }
    }

    /**
     * logic to determine what kind of insights to generate
     * @param accountId
     */
    private void generateGeneralInsights(final Long accountId, final Long deviceId) {
    // TODO
    }

    private int getAccountAgeInDays(final DateTime accountCreated) {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Duration duration = new Duration(now, accountCreated);
        return duration.toStandardDays().getDays();
    }
}
