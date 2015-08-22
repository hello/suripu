package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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

        final Optional<TimeZoneHistory> timeZoneHistory = timeZoneHistoryDAODynamoDB.getCurrentTimeZone(accountId);
        if (timeZoneHistory.isPresent() == Boolean.FALSE) {
            return Optional.absent();
        }
        final Integer timeZoneOffset = timeZoneHistory.get().offsetMillis;

        final DateTime queryEndTime = DateTime.now(DateTimeZone.forOffsetMillis(timeZoneOffset)).withHourOfDay(NIGHT_START_HOUR_LOCAL);
        final DateTime queryStartTime = queryEndTime.minusDays(InsightCard.PAST_WEEK);

        //Grab all night-time data for past week
        final List<DeviceData> totalRows = deviceDataDAO.getLightByBetweenHourDateFast(accountId, deviceId, (int) LIGHT_LEVEL_WARNING, queryStartTime, queryEndTime, NIGHT_START_HOUR_LOCAL, NIGHT_END_HOUR_LOCAL);

        //List containing period light is on each night last week
        final List<Integer> lightOnList = Lists.newArrayList();

        final List<Integer> dayIndices = Lists.newArrayList();
        dayIndices.add(0);
        for (DeviceData deviceData : totalRows) {
            DeviceData previousDeviceData = totalRows.get(totalRows.indexOf(deviceData) - 1);
            boolean sameDay = sameDay(deviceData, previousDeviceData);
            if (sameDay) {
                continue;
            }
            final Integer startDayTomorrowIndex = totalRows.indexOf(deviceData);
            List<DeviceData> rows = totalRows.subList(Iterables.getLast(dayIndices), startDayTomorrowIndex);
            final Optional<Integer> lightOnDuration = processLightDataOneDay(rows, OFF_MINUTES_THRESHOLD);
            if (lightOnDuration.isPresent()) {
                lightOnList.add(lightOnDuration.get());
            }
        }

        final Optional<InsightCard> card = processLightData(lightOnList, accountId);
        return card;
    }

    public static Boolean sameDay(final DeviceData currentDeviceData, final DeviceData previousDeviceData) {
        final Integer elapsedTime = new Period(previousDeviceData.dateTimeUTC, currentDeviceData.dateTimeUTC).getMinutes();
        final Integer comparisonPeriod = new Period(OFFLINE_HOURS).getMinutes();
        if (elapsedTime > comparisonPeriod) {
            return Boolean.FALSE;
        }
        else {
            return Boolean.TRUE;
        }
    }

    public static Optional<Integer> processLightDataOneDay(final List<DeviceData> data, final int offMinutesThreshold) {
        if (data.size() <= 1) {
            return Optional.absent();
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
                return Optional.of(onMinutesTruncated);
            }
        }

        final DateTime firstLightOnTimeStamp = data.get(0).dateTimeUTC;
        final Period onTime = new Period(firstLightOnTimeStamp, lastLightOnTimeStamp);
        final Integer onMinutes = onTime.getMinutes();
        return Optional.of(onMinutes);
    }

    public static Optional<InsightCard> processLightData(final List<Integer> lightOnList, final Long accountId) {

        if (lightOnList.size() == 0) {
            LOGGER.debug("Light was on for less than 1 second each day for accountId {}", accountId);
            return Optional.absent();
        }

        // compute average value
        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final Integer lightOn : lightOnList) {
            stats.addValue(lightOn);
        }

        int avgLightOn = (int) stats.getMean();

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
