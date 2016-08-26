package com.hello.suripu.core.insights.models;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountReadDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.insights.InsightProfile;
import com.hello.suripu.core.insights.models.text.SleepAlarmMsgEN;
import com.hello.suripu.core.insights.models.text.Text;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.insights.InsightCard;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.preferences.PreferenceName;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.InsightUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Years;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by jyfan on 5/18/16.
 */
public class SleepAlarm implements InsightModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepAlarm.class);

    public static final Integer PRE_SLEEP_TIME = 30;

    public static final Integer NUM_DAYS = 14;
    public static final Integer MAX_ALLOWED_RANGE = 6 * 60; //6 hrs
    public static final Integer LATEST_ALLOWED_WAKE_TIME = (11) * 60; //11 AM
    public static final Integer EARLIEST_ALLOWED_WAKE_TIME = 4 * 60; //4 AM

    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private final AccountReadDAO accountReadDAO;
    private final AccountPreferencesDAO preferencesDAO;

    private SleepAlarm(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final AccountReadDAO accountReadDAO, final AccountPreferencesDAO preferencesDAO) {
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.accountReadDAO = accountReadDAO;
        this.preferencesDAO = preferencesDAO;
    }

    public static SleepAlarm create(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final AccountReadDAO accountReadDAO, final AccountPreferencesDAO preferencesDAO) {
        return new SleepAlarm(sleepStatsDAODynamoDB, accountReadDAO, preferencesDAO);
    }

    @VisibleForTesting
    public static Optional<InsightCard>  processSleepAlarm(final Long accountId, final List<Integer> wakeTimeList, final Integer userAge, final DateTimeFormatter timeFormat) {

        if (wakeTimeList.isEmpty()) {
            LOGGER.info("account_id={} insight=sleep-alarm action=wake-time-list-empty", accountId);
            return Optional.absent();
        }
        else if (wakeTimeList.size() <= 2) {
            LOGGER.info("account_id={} insight=sleep-alarm action=wake-time-list-too-small", accountId);
            return Optional.absent(); //not big enough to calculate mean meaningfully har har
        }

        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final int wakeTime : wakeTimeList) {
            stats.addValue(wakeTime);
        }

        final Boolean passSafeGuards = checkSafeGuards(stats);
        if (!passSafeGuards) {
            LOGGER.info("insight=sleep-alarm account_id={} action=fail-safe-guard");
            return Optional.absent();
        }

        final Double wakeMedDouble = stats.getPercentile(50);
        final int wakeMed = (int) Math.round(wakeMedDouble);

        final int recSleepDurationMins = getRecommendedSleepDurationMinutes(userAge);
        final int recommendedSleepMinutesTime = wakeMed - recSleepDurationMins;

        final String wakeTime = InsightUtils.timeConvertRound(wakeMed, timeFormat);
        final String preSleepTime = InsightUtils.timeConvertRound((recommendedSleepMinutesTime - PRE_SLEEP_TIME), timeFormat);
        final String sleepTime = InsightUtils.timeConvertRound(recommendedSleepMinutesTime, timeFormat);

        final Text text = SleepAlarmMsgEN.getSleepAlarmMessage(wakeTime, recSleepDurationMins, preSleepTime, sleepTime);

        return Optional.of(InsightCard.createBasicInsightCard(accountId, text.title, text.message,
                InsightCard.Category.SLEEP_TIME, InsightCard.TimePeriod.MONTHLY,
                DateTime.now(DateTimeZone.UTC), InsightCard.InsightType.DEFAULT));
    }

    @VisibleForTesting
    public static Boolean checkSafeGuards(final DescriptiveStatistics stats) {

        //Safeguard for anomalous sleeptime encountered last week
        final Double wakeMax = stats.getMax();
        final Double wakeMin = stats.getMin();
        final Double wakeRange = wakeMax - wakeMin;
        if (wakeRange > MAX_ALLOWED_RANGE) { //If Range in sleep times is too big, average sleep time is less meaningful
            return Boolean.FALSE;
        }

        //Wake time sanity check, should be between 4AM and 11AM
        final Double wakeMed = stats.getPercentile(50);
        if (wakeMed > LATEST_ALLOWED_WAKE_TIME) {
            return Boolean.FALSE;
        }
        if (wakeMed < EARLIEST_ALLOWED_WAKE_TIME) {
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    @VisibleForTesting
    public static Integer getRecommendedSleepDurationMinutes(final Integer userAge) {
        final SleepDuration.recommendation sleepDurationRecommendation = SleepDuration.getSleepDurationRecommendation(userAge);
        final Integer recommendation = (sleepDurationRecommendation.maxHours * 60 + sleepDurationRecommendation.minHours * 60) / 2;

        return recommendation;
    }

    @Override
    public Optional<InsightCard> generate(InsightProfile insightProfile) {
        final Long accountId = insightProfile.pill().accountId;
        //get sleep variance data for the past NUM_DAYS
        final DateTime queryEndDate = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay();
        final DateTime queryStartDate = queryEndDate.minusDays(NUM_DAYS);

        final String queryEndDateString = DateTimeUtil.dateToYmdString(queryEndDate);
        final String queryStartDateString = DateTimeUtil.dateToYmdString(queryStartDate);

        final List<AggregateSleepStats> sleepStats = sleepStatsDAODynamoDB.getBatchStats(accountId, queryStartDateString, queryEndDateString);
        LOGGER.debug("insight=sleep-alarm account_id={} sleep_stat_len={}", accountId, sleepStats.size());
        final List<Integer> wakeTimeList = Lists.newArrayList();
        for (final AggregateSleepStats stat : sleepStats) {

            final Integer dayOfWeek = stat.dateTime.getDayOfWeek(); //Do not pull weekends (Sat & Sun)
            if (dayOfWeek == 6) {
                continue;
            } else if (dayOfWeek == 7) {
                continue;
            }

            final Long wakeTimeStamp = stat.sleepStats.wakeTime;
            final DateTime wakeTimeDateTime = new DateTime(wakeTimeStamp, DateTimeZone.forOffsetMillis(stat.offsetMillis));
            final int wakeTime = wakeTimeDateTime.getMinuteOfDay();
            wakeTimeList.add(wakeTime);
        }

        final DateTime dob;
        final Optional<Account> account = accountReadDAO.getById(accountId);
        if (account.isPresent()) {
            dob = account.get().DOB;
        } else {
            dob = DateTime.now(DateTimeZone.UTC);
        }

        final Integer userAge = Years.yearsBetween(dob, DateTime.now(DateTimeZone.UTC)).toPeriod().getYears();

        final Optional<InsightCard> card = processSleepAlarm(accountId, wakeTimeList, userAge, getTimeFormat(accountId));
        return card;
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
}
