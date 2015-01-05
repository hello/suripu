package com.hello.suripu.service.models;

import com.hello.suripu.service.configuration.SenseUploadConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;


public class UploadSettings {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadSettings.class);

    private static final Long ALLOWABLE_TIME_BETWEEN_NOW_AND_NEXT_ALARM = 20*60*1000L;  // milliseconds
    private static final Long SUSTAINED_TIME_BETWEEN_NOW_AND_NEXT_ALARM = 60*60*1000L;  // milliseconds
    private static final Integer FASTEST_UPLOAD_INTERVAL = 1; // minute(s)

    public final DateTimeZone userTimeZone;
    public final Long userNextAlarmTimestamp;
    public final SenseUploadConfiguration senseUploadConfiguration;

    public UploadSettings(final SenseUploadConfiguration senseUploadConfiguration, final DateTimeZone userTimeZone, final Long userNextAlarmTimestampMillis) {
        checkNotNull(userTimeZone, "userTimezone can not be null");
        checkNotNull(userNextAlarmTimestampMillis, "userNextAlarmTimestampMillis can not be null");
        checkNotNull(senseUploadConfiguration, "senseUploadConfiguration can not be null");
        this.userTimeZone = userTimeZone;
        this.userNextAlarmTimestamp = userNextAlarmTimestampMillis;
        this.senseUploadConfiguration = senseUploadConfiguration;
    }

    public Integer getUploadIntervalInMinutes() {
        final Integer unadjustedUploadIntervalInMinutes =  computeUploadIntervalPerUserPerSetting(getUserCurrentDateTime(), senseUploadConfiguration);
        final Integer adjustedUploadIntervalInMinutes = adjustUploadIntervalInMinutes(unadjustedUploadIntervalInMinutes, userNextAlarmTimestamp);
        return adjustedUploadIntervalInMinutes;
    }

    private DateTime getUserCurrentDateTime() {
        final DateTime userCurrentDateTime = DateTime.now(this.userTimeZone);
        return userCurrentDateTime;
    }

    private Integer computeUploadIntervalPerUserPerSetting(final DateTime userCurrentDateTime, final SenseUploadConfiguration senseUploadConfiguration) {
        final Integer hourOfDay = userCurrentDateTime.getHourOfDay();

        LOGGER.debug("User Current DateTime - Hour of Day: {}", hourOfDay);

        // Non peak times are the times whose hours are within the range defined in configuration
        Boolean isNonPeak = hourOfDay >= senseUploadConfiguration.getNonPeakHourLowerbound() && hourOfDay <= senseUploadConfiguration.getNonPeakHourUpperBound();

        // If weekDaysOnly == true, we assume that users could sleep any time during weekends
        if (senseUploadConfiguration.getWeekDaysOnly()) {
            final Integer dayOfWeek = userCurrentDateTime.getDayOfWeek();
            LOGGER.debug("User Current DateTime - Day of Week: {}", userCurrentDateTime.dayOfWeek().getAsText());
            isNonPeak = isNonPeak && (dayOfWeek != DateTimeConstants.SATURDAY && dayOfWeek != DateTimeConstants.SUNDAY);
        }

        // Non peak hours trigger short upload interval, peak hours trigger long upload interval
        if (isNonPeak) {
            return senseUploadConfiguration.getLongInterval();
        }
        return senseUploadConfiguration.getShortInterval();
    }

    private Integer adjustUploadIntervalInMinutes(final Integer standardUploadIntervalInMinutes, final Long userNextAlarmTimestampMillis) {
        if(userNextAlarmTimestampMillis == 0){
            return standardUploadIntervalInMinutes;
        }
        
        LOGGER.debug("diff in milliseconds {}", userNextAlarmTimestampMillis - DateTime.now(DateTimeZone.UTC).getMillis());
        if (userNextAlarmTimestampMillis - DateTime.now(DateTimeZone.UTC).getMillis() <= ALLOWABLE_TIME_BETWEEN_NOW_AND_NEXT_ALARM) {
            return FASTEST_UPLOAD_INTERVAL;
        }
        if (userNextAlarmTimestampMillis - DateTime.now(DateTimeZone.UTC).getMillis() >= SUSTAINED_TIME_BETWEEN_NOW_AND_NEXT_ALARM) {
            return standardUploadIntervalInMinutes;
        }
        return FASTEST_UPLOAD_INTERVAL;
    }

//    public Integer computeUploadIntervalForTests(final DateTime dateTime) {
//        return computeUploadIntervalPerUserPerSetting(dateTime, new SenseUploadConfiguration());
//    }
//
//    public Integer adjustUploadIntervalForTests(final Integer standardUploadInterval, final Long userNextAlarmTimestamp) {
//        return adjustUploadIntervalInMinutes(standardUploadInterval, userNextAlarmTimestamp);
//    }

    public Integer getFastestUploadInterval() {
        return FASTEST_UPLOAD_INTERVAL;
    }
}
