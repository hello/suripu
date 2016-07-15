package com.hello.suripu.core.processors.insights;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.DeviceDataInsightQueryDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.SoundDisturbanceMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import com.hello.suripu.core.util.DateTimeUtil;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by jyfan on 9/25/15.
 */
public class SoundDisturbance {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoundDisturbance.class);

    //See https://github.com/hello/research/tree/master/Jingyun_LabBooks/Book10
    private static final Integer NORMAL_SUM_DISTURBANCE = 1000;
    private static final Integer HIGH_SUM_DISTURBANCE = 2000;

    private static final Integer DATA_START_HOUR_LOCAL = 16;
    private static final Integer DATA_END_HOUR_LOCAL = 12;

    public static Optional<InsightCard> getInsights(final Long accountId,
                                                    final DeviceAccountPair deviceAccountPair,
                                                    final DeviceDataInsightQueryDAO deviceDataDAO,
                                                    final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {

        final Optional<Integer> timeZoneOffsetOptional = getTimeZoneOffsetOptional(sleepStatsDAODynamoDB, accountId, DateTime.now(DateTimeZone.UTC));
        if (!timeZoneOffsetOptional.isPresent()) {
            LOGGER.debug("action=insight-absent insight=sound reason=timezoneoffset-absent account_id={}", accountId);
            return Optional.absent();
        }
        final Integer timeZoneOffset = timeZoneOffsetOptional.get();

        final DateTime nowTime = DateTime.now(DateTimeZone.forOffsetMillis(timeZoneOffset));
        final DateTime queryEndTime = getDeviceDataQueryDate(nowTime);

        final List<DeviceData> deviceDatas = getDeviceData(accountId, deviceAccountPair, deviceDataDAO, queryEndTime, timeZoneOffset);
        if (deviceDatas.isEmpty()) {
            LOGGER.debug("action=insight-absent insight=bed-light-intensity reason=devicedata-empty account_id={}", accountId);
            return Optional.absent();
        }

        final Integer sumDisturbance = getSumDisturbance(deviceDatas);
        return processData(accountId, sumDisturbance);
    }

    @VisibleForTesting
    public static Optional<InsightCard> processData(final Long accountId, final Integer sumDisturbance) {

        final Text text;
        if (sumDisturbance < NORMAL_SUM_DISTURBANCE) {
            LOGGER.debug("action=insight-absent insight=bed-light-intensity reason=sumdisturbance-low account_id={}", accountId);
            return Optional.absent();
        } else if (sumDisturbance < HIGH_SUM_DISTURBANCE) {
            text = SoundDisturbanceMsgEN.getHighSumDisturbance();
        } else {
            text = SoundDisturbanceMsgEN.getVeryHighSumDisturbance();
        }

        return Optional.of(InsightCard.createBasicInsightCard(accountId, text.title, text.message,
                InsightCard.Category.SOUND, InsightCard.TimePeriod.MONTHLY,
                DateTime.now(DateTimeZone.UTC), InsightCard.InsightType.DEFAULT));
    }

    @VisibleForTesting
    public static Integer getSumDisturbance(final List<DeviceData> data) {

        final DescriptiveStatistics soundStats = new DescriptiveStatistics();
        for (final DeviceData deviceData : data) {
            soundStats.addValue(deviceData.audioNumDisturbances);
        }

        final Integer sumDisturbance = (int) soundStats.getSum();
        return sumDisturbance;
    }

    @VisibleForTesting
    public static final DateTime getDeviceDataQueryDate(final DateTime date) {
/*
    We want to get "last night's" data so if we are in a current night bucket (haven't finished the night) we get *last* night by doing minusDays(1), whereas if we did finish the night, we do not need to do minusDays()

                    today
         |-----------------------------------------------------------------------|

                                                                                   tomorrow
                                                                                 |---------|
er
                    no data collection
    hour |-----|__________________________________________|----------------------|---------|
         0     12                                         16                     0         12
               ^                                      ^         ^
               |                                      |         |
            minus day                              do not subtract day
  */

        final Integer queryHour = date.hourOfDay().get();

        if (queryHour <= 12) {
            return date.minusDays(1).withHourOfDay(DATA_END_HOUR_LOCAL);
        }

        return date.withHourOfDay(DATA_END_HOUR_LOCAL);
    }

    private static final List<DeviceData> getDeviceData(final Long accountId, final DeviceAccountPair deviceAccountPair, final DeviceDataInsightQueryDAO deviceDataDAO, final DateTime queryEndTime, final Integer timeZoneOffset) {

        final DateTime queryStartTime = queryEndTime.minusDays(1);

        final DateTime queryEndTimeLocal = queryEndTime.plusMillis(timeZoneOffset);
        final DateTime queryStartTimeLocal = queryStartTime.plusMillis(timeZoneOffset);

        //Grab all pre-bed data for past week
        final DeviceId deviceId = DeviceId.create(deviceAccountPair.externalDeviceId);
        Response<ImmutableList<DeviceData>> response = deviceDataDAO.getBetweenHourDateByTS(accountId, deviceId, queryStartTime, queryEndTime, queryStartTimeLocal, queryEndTimeLocal, DATA_START_HOUR_LOCAL, DATA_END_HOUR_LOCAL);
        if (response.status == Response.Status.SUCCESS) {
            return response.data;
        }
        return Lists.newArrayList();
    }

    private static final Optional<Integer> getTimeZoneOffsetOptional(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final Long accountId, final DateTime queryEndDate) {
        final String sleepStatsQueryEndDate = DateTimeUtil.dateToYmdString(queryEndDate);
        final String sleepStatsQueryStartDate = DateTimeUtil.dateToYmdString(queryEndDate.minusDays(1));

        final List<AggregateSleepStats> sleepStats = sleepStatsDAODynamoDB.getBatchStats(accountId, sleepStatsQueryStartDate, sleepStatsQueryEndDate);

        if (!sleepStats.isEmpty()) {
            return Optional.of(sleepStats.get(0).offsetMillis);
        }

        LOGGER.debug("action=insight-absent insight=bed-light-intensity reason=sleepstats-empty account_id={} query_start={} query_end={}", accountId, sleepStatsQueryStartDate, sleepStatsQueryEndDate);
        return Optional.absent();
    }
}
