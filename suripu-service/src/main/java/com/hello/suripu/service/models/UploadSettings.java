package com.hello.suripu.service.models;

import com.hello.suripu.service.configuration.SenseUploadConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UploadSettings {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadSettings.class);

    private static final Long ALLOWABLE_TIME_BETWEEN_NOW_AND_NEXT_ALARM = 20*60*1000L;  // milliseconds
    private static final Long SUSTAINED_TIME_BETWEEN_NOW_AND_NEXT_ALARM = 60*60*1000L;  // milliseconds
    private static final Integer FASTEST_UPLOAD_INTERVAL = 1; // minute(s)

    public static Integer getUploadInterval(final DateTime userLocalDateTime, final SenseUploadConfiguration senseUploadConfiguration, final Long userNextAlarmTimestampMillis) {
        final Integer adjustedUploadIntervalInMinutes = adjustUploadIntervalInMinutes(
            DateTime.now(DateTimeZone.UTC).getMillis(),
            computeUploadIntervalPerUserPerSetting(userLocalDateTime, senseUploadConfiguration),
            userNextAlarmTimestampMillis
        );
        LOGGER.debug("Adjusted Upload Interval in Minutes: {}", adjustedUploadIntervalInMinutes);
        return adjustedUploadIntervalInMinutes;
    }

    public static Integer computeUploadIntervalPerUserPerSetting(final DateTime userLocalDateTime, final SenseUploadConfiguration senseUploadConfiguration) {

        final Integer hourOfDay = userLocalDateTime.getHourOfDay();

        LOGGER.debug("User Current DateTime: {}", userLocalDateTime);

        // Non peak times are the times whose hours are within the range defined in configuration
        Boolean isNonPeak = hourOfDay >= senseUploadConfiguration.getNonPeakHourLowerBound() && hourOfDay <= senseUploadConfiguration.getNonPeakHourUpperBound();

        // If weekDaysOnly == true, we assume that users could sleep any time during weekends
        if (senseUploadConfiguration.getWeekDaysOnly()) {
            final Integer dayOfWeek = userLocalDateTime.getDayOfWeek();
            isNonPeak = isNonPeak && (dayOfWeek != DateTimeConstants.SATURDAY && dayOfWeek != DateTimeConstants.SUNDAY);
        }

        // Non peak hours trigger short upload interval, peak hours trigger long upload interval
        if (isNonPeak) {
            return senseUploadConfiguration.getLongInterval();
        }
        return senseUploadConfiguration.getShortInterval();
    }

    public static Integer adjustUploadIntervalInMinutes(final Long currentTimestampMillis, final Integer standardUploadIntervalInMinutes, final Long userNextAlarmTimestampMillis) {
        Long timeUntilNextAlarmMillis = userNextAlarmTimestampMillis - currentTimestampMillis;

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
