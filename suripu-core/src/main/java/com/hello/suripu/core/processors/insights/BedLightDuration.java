package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.BedLightDurationMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import com.hello.suripu.core.models.TimeZoneHistory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jyfan on 8/5/15.
 */
public class BedLightDuration {
    private static final Logger LOGGER = LoggerFactory.getLogger(BedLightDuration.class);

    private static final int NIGHT_START_HOUR_LOCAL = 21; // 9pm
    private static final int NIGHT_END_HOUR_LOCAL = 4; // 4am

    private static final int OFFLINE_HOURS = 17; // number of hours after night end and before next night start

    public static final float LIGHT_LEVEL_WARNING = 5.0f;  // in lux
    public static final float LIGHT_LEVEL_ALERT = 35.0f;  // in lux

    public static final int OFF_MINUTES_THRESHOLD = 45; //If lights are off for more than 45 minutes, we discard preceding data


    public static Optional<InsightCard> getInsights(final Long accountId, final Long deviceId, final DeviceDataDAO deviceDataDAO, final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB) {

        final List<DeviceData> deviceData = getDeviceData(accountId, deviceId, deviceDataDAO, timeZoneHistoryDAODynamoDB);
        if (deviceData == null) {
            return Optional.absent();
        }

        final List<List<DeviceData>> deviceDataByDay = splitDeviceDataByDay(deviceData);

        final List<Integer> lightOnDurationList = new ArrayList<Integer>(deviceDataByDay.size());

        for (int i = 0; i < deviceDataByDay.size(); i++) {
            lightOnDurationList.set(i, findLightOnDurationForDay(deviceDataByDay.get(i), OFF_MINUTES_THRESHOLD));
        }

        int avgLightOn;
        if (lightOnDurationList.size() == 0) {
            avgLightOn = 0;
        }
        else {
            avgLightOn = computeAverage(lightOnDurationList);
        }

        return scoreCardBedLightDuration(avgLightOn, accountId);
    }

    public static final List<DeviceData> getDeviceData(final Long accountId, final Long deviceId, final DeviceDataDAO deviceDataDAO, final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB) {
        final Optional<TimeZoneHistory> timeZoneHistory = timeZoneHistoryDAODynamoDB.getCurrentTimeZone(accountId);
        if (!timeZoneHistory.isPresent()) {
            return null;
        }
        final Integer timeZoneOffset = timeZoneHistory.get().offsetMillis;

        final DateTime queryEndTime = DateTime.now(DateTimeZone.forOffsetMillis(timeZoneOffset)).withHourOfDay(NIGHT_START_HOUR_LOCAL);
        final DateTime queryStartTime = queryEndTime.minusDays(InsightCard.PAST_WEEK);

        //Grab all night-time data for past week
        return deviceDataDAO.getLightByBetweenHourDateFast(accountId, deviceId, (int) LIGHT_LEVEL_WARNING, queryStartTime, queryEndTime, NIGHT_START_HOUR_LOCAL, NIGHT_END_HOUR_LOCAL);
    }

    public static final List<List<DeviceData>> splitDeviceDataByDay(List<DeviceData> data) {
        List<List<DeviceData>> res = new ArrayList<>();
        int beg = 0;
        for (int i = 0; i < data.size(); i++) {
            if (i > 0 && !sameDay(data.get(i), data.get(i-1))) {
                res.add(data.subList(beg, i));
                beg = i;
            }
        }
        return res;
    }

    public static Boolean sameDay(final DeviceData currentDeviceData, final DeviceData previousDeviceData) {
        final Integer elapsedTime = new Period(previousDeviceData.dateTimeUTC, currentDeviceData.dateTimeUTC).getMinutes();
        final Integer comparisonPeriod = new Period(OFFLINE_HOURS).getMinutes();
        return elapsedTime < comparisonPeriod;
    }

    public static Integer findLightOnDurationForDay(final List<DeviceData> data, final int offMinutesThreshold) {
        if (data.size() <= 1) {
            return 0;
        }

        final DateTime lastLightOnTimeStamp = data.get(data.size() - 1).dateTimeUTC;
        for (int i = data.size() - 2; i >= 0; i--) {
            final DateTime currentLightOnTimeStamp = data.get(i).dateTimeUTC;
            final DateTime previousLightOnTimeStamp = data.get(i + 1).dateTimeUTC;
            //assertTrue(currentLightOnTimeStamp < previousLightOnTimeStamp);
            final Period offTime = new Period(currentLightOnTimeStamp, previousLightOnTimeStamp);
            final int offMinutes = offTime.getMinutes();

            if (offMinutes > offMinutesThreshold) {
                final Period onTimeTruncated = new Period(previousLightOnTimeStamp, lastLightOnTimeStamp);
                final Integer onMinutesTruncated = onTimeTruncated.getMinutes();
                LOGGER.debug("on Minutes truncated is {}", onMinutesTruncated);
                return onMinutesTruncated;
            }
        }

        final DateTime firstLightOnTimeStamp = data.get(0).dateTimeUTC;
        final Period onTime = new Period(firstLightOnTimeStamp, lastLightOnTimeStamp);
        return onTime.getMinutes();
    }

    public static final int computeAverage(List<Integer> data) {
        // compute average value
        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final Integer lightOn : data) {
            stats.addValue(lightOn);
        }
        return (int) stats.getMean();
    }

    public static Optional<InsightCard> scoreCardBedLightDuration(final int avgLightOn, final Long accountId) {
        Text text;
        if (avgLightOn <= 60) {
            return Optional.absent();
//            text = BedLightDurationMsgEN.getLittleLight();
        } else if (avgLightOn <= 120) {
            text = BedLightDurationMsgEN.getMediumLight();
        } else {
            text = BedLightDurationMsgEN.getHighLight();
        }

        return Optional.of(new InsightCard(accountId, text.title, text.message,
                InsightCard.Category.BED_LIGHT_DURATION, InsightCard.TimePeriod.MONTHLY,
                DateTime.now(DateTimeZone.UTC)));
    }
}
