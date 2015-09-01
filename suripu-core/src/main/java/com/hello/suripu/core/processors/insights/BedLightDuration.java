package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.BedLightDurationMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import com.hello.suripu.core.util.DateTimeUtil;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by jyfan on 8/5/15.
 */
public class BedLightDuration {
    private static final Logger LOGGER = LoggerFactory.getLogger(BedLightDuration.class);

    private static final int NIGHT_START_HOUR_LOCAL = 21; // 9pm
    private static final int NIGHT_END_HOUR_LOCAL = 4; // 4am

    private static final float LIGHT_ON_LEVEL = 5.0f;  // in lux

    private static final int OFFLINE_HOURS = 17; // num hours after night end and before next night start. If set OFFLINE_HOURS<length of night hours, sameDay function will need to change
    private static final int OFF_MINUTES_THRESHOLD = 45; //If lights are off for more than 45 minutes, we discard preceding data

    public static Optional<InsightCard> getInsights(final Long accountId, final Long deviceId, final DeviceDataDAO deviceDataDAO, final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {

        final Optional<Integer> timeZoneOffsetOptional = getTimeZoneOffsetOptional(sleepStatsDAODynamoDB, accountId, DateTime.now());
        if (!timeZoneOffsetOptional.isPresent()) {
            return Optional.absent();
        }
        final Integer timeZoneOffset = timeZoneOffsetOptional.get();

        final List<DeviceData> deviceDatas = getDeviceData(accountId, deviceId, deviceDataDAO, timeZoneOffset);
        if (deviceDatas.isEmpty()) {
            return Optional.absent();
        }

        final int avgLightOn = getInsightsHelper(deviceDatas);
        return scoreCardBedLightDuration(avgLightOn, accountId);
    }

    public static Integer getInsightsHelper(final List<DeviceData> deviceDatas) {

        final List<List<DeviceData>> deviceDataByDay = splitDeviceDataByDay(deviceDatas);

        final List<Integer> lightOnDurationList = Lists.newArrayList();

        for (final List<DeviceData> deviceDataDay : deviceDataByDay) {
            lightOnDurationList.add(findLightOnDurationForDay(deviceDataDay, OFF_MINUTES_THRESHOLD));
        }

        final Integer avgLightOn = computeAverage(lightOnDurationList);
        return avgLightOn;
    }

    public static final int computeAverage(final List<Integer> data) {
        // compute average value
        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final Integer lightOn : data) {
            stats.addValue(lightOn);
        }
        return (int) stats.getMean();
    }

    public static Integer findLightOnDurationForDay(final List<DeviceData> data, final int offMinutesThreshold) {
        /**
         * Splits timeseries into buckets, where each bucket contains no light-off times exceeding offMinutesThreshold. Gets length of time of last bucket which is light on before bed.
         */
        if (data.size() <= 1) {
            return 0;
        }

        final DateTime lastLightOnTimeStamp = data.get(data.size() - 1).dateTimeUTC;
        for (int i = data.size() - 2; i >= 0; i--) {
            final DateTime currentLightOnTimeStamp = data.get(i).dateTimeUTC;
            final DateTime previousLightOnTimeStamp = data.get(i + 1).dateTimeUTC;
            //assertTrue(currentLightOnTimeStamp < previousLightOnTimeStamp);
            final Integer offMinutes = new Period(currentLightOnTimeStamp, previousLightOnTimeStamp, PeriodType.minutes()).getMinutes();

            if (offMinutes >= offMinutesThreshold) {
                final Period onTimeTruncated = new Period(previousLightOnTimeStamp, lastLightOnTimeStamp);
                final Integer onMinutesTruncated = onTimeTruncated.getMinutes();
                return onMinutesTruncated;
            }
        }

        final DateTime firstLightOnTimeStamp = data.get(0).dateTimeUTC;
        final Period onTime = new Period(firstLightOnTimeStamp, lastLightOnTimeStamp, PeriodType.minutes());
        return onTime.getMinutes();
    }

    public static final List<List<DeviceData>> splitDeviceDataByDay(List<DeviceData> data) {
        /**
         * Input is list of DeviceData for 1 week. Split each day into individual list of DeviceData to get output list of lists
         */
        final List<List<DeviceData>> res = Lists.newArrayList();
        int beg = 0;
        for (int i = 1; i < data.size(); i++) {
            if (!sameDay(data.get(i), data.get(i-1))) {
                res.add(data.subList(beg, i));
                beg = i;
            }
            else if (i == data.size() - 1) { //edge case for last day
                res.add(data.subList(beg, data.size()));
            }
        }
        LOGGER.debug("Split deviceData and got {} days", res.size());
        return res;
    }

    public static Boolean sameDay(final DeviceData currentDeviceData, final DeviceData previousDeviceData) {
        /**
         * Input is two consecutive timestamps. Because getDeviceData only pulls from 9pm-4am, if two timestamps are from different days, must have difference of at least 17 hrs (time elapsed from 4am-9pm)
         */
        final Integer elapsedMinutes = new Period(previousDeviceData.dateTimeUTC, currentDeviceData.dateTimeUTC, PeriodType.minutes()).getMinutes();
        final Integer comparisonPeriodMinutes = OFFLINE_HOURS * 60;
        return elapsedMinutes < comparisonPeriodMinutes;
    }

    public static final List<DeviceData> getDeviceData(final Long accountId, final Long deviceId, final DeviceDataDAO deviceDataDAO, final Integer timeZoneOffset) {

        final DateTime queryEndTime = DateTime.now(DateTimeZone.forOffsetMillis(timeZoneOffset)).withHourOfDay(NIGHT_START_HOUR_LOCAL);
        final DateTime queryStartTime = queryEndTime.minusDays(InsightCard.PAST_WEEK);

        //Grab all night-time data for past week
        return deviceDataDAO.getLightByBetweenHourDateByTS(accountId, deviceId, (int) LIGHT_ON_LEVEL, queryStartTime, queryEndTime, NIGHT_START_HOUR_LOCAL, NIGHT_END_HOUR_LOCAL);
    }

    public static final Optional<Integer> getTimeZoneOffsetOptional(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final Long accountId, final DateTime queryEndDate) {
        final String sleepStatsQueryEndDate = DateTimeUtil.dateToYmdString(queryEndDate);
        final String sleepStatsQueryStartDate = DateTimeUtil.dateToYmdString(queryEndDate.minusDays(1));

        final List<AggregateSleepStats> sleepStats = sleepStatsDAODynamoDB.getBatchStats(accountId, sleepStatsQueryStartDate, sleepStatsQueryEndDate);
        if (!sleepStats.isEmpty()) {
            return Optional.of(sleepStats.get(0).offsetMillis);
        }
        LOGGER.debug("SleepStats empty, fail to retrieve timeZoneOffset for accountId {} from {} to {}", accountId, sleepStatsQueryStartDate, sleepStatsQueryEndDate);
        return Optional.absent();
    }

    public static Optional<InsightCard> scoreCardBedLightDuration(final int avgLightOn, final Long accountId) {
        final Text text;
        if (avgLightOn <= 60) {
            return Optional.absent();
//            text = BedLightDurationMsgEN.getLittleLight();
        }

        if (avgLightOn <= 120) {
            text = BedLightDurationMsgEN.getMediumLight();
        } else {
            text = BedLightDurationMsgEN.getHighLight();
        }

        return Optional.of(new InsightCard(accountId, text.title, text.message,
                InsightCard.Category.BED_LIGHT_DURATION, InsightCard.TimePeriod.MONTHLY,
                DateTime.now(DateTimeZone.UTC)));
    }
}
