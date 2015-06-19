package com.hello.suripu.service.models;

import com.hello.suripu.service.configuration.SenseUploadConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UploadSettings {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadSettings.class);

    private static final Long ALLOWABLE_TIME_BETWEEN_NOW_AND_NEXT_ALARM = 40*60*1000L;  // milliseconds
    private static final Long SUSTAINED_TIME_BETWEEN_NOW_AND_NEXT_ALARM = 60*60*1000L;  // milliseconds
    private static final Integer FASTEST_UPLOAD_INTERVAL = 1; // This must be 1 minutes or Sense will miss alarm.


    public static Integer computeUploadIntervalPerUserPerSetting(final DateTime userLocalDateTime, final SenseUploadConfiguration senseUploadConfiguration, final Boolean isReducedInterval) {

        final Integer hourOfDay = userLocalDateTime.getHourOfDay();

        //LOGGER.debug("User Current DateTime: {}", userLocalDateTime);

        // Non peak times are the times whose hours are within the range defined in configuration
        Boolean isNonPeak = hourOfDay >= senseUploadConfiguration.getNonPeakHourLowerBound() && hourOfDay <= senseUploadConfiguration.getNonPeakHourUpperBound();

        // If weekDaysOnly == true, we assume that users could sleep any time during weekends
        if (senseUploadConfiguration.getWeekDaysOnly()) {
            final Integer dayOfWeek = userLocalDateTime.getDayOfWeek();
            isNonPeak = isNonPeak && (dayOfWeek != DateTimeConstants.SATURDAY && dayOfWeek != DateTimeConstants.SUNDAY);
        }

        // Non peak hours trigger long upload interval, peak hours trigger short upload interval
        if (isReducedInterval) {
            return (isNonPeak) ? SenseUploadConfiguration.REDUCED_LONG_INTERVAL : SenseUploadConfiguration.REDUCED_SHORT_INTERVAL;
        }

        return (isNonPeak) ? senseUploadConfiguration.getLongInterval() : senseUploadConfiguration.getShortInterval();
    }

    public static Integer adjustUploadIntervalInMinutes(final Long currentTimestampMillis, final Integer standardUploadIntervalInMinutes, final Long userNextAlarmTimestampMillis) {

        if(userNextAlarmTimestampMillis - currentTimestampMillis < 0){
            return standardUploadIntervalInMinutes;
        }

        final Long timeUntilNextAlarmMillis = userNextAlarmTimestampMillis - currentTimestampMillis;

        if (timeUntilNextAlarmMillis <= ALLOWABLE_TIME_BETWEEN_NOW_AND_NEXT_ALARM) {
            return FASTEST_UPLOAD_INTERVAL;
        }
        if (timeUntilNextAlarmMillis >= SUSTAINED_TIME_BETWEEN_NOW_AND_NEXT_ALARM) {
            return standardUploadIntervalInMinutes;
        }
        return FASTEST_UPLOAD_INTERVAL;
    }

    public static Integer getFastestUploadInterval() {
        return FASTEST_UPLOAD_INTERVAL;
    }
}
