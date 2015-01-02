package com.hello.suripu.service.models;

import com.hello.suripu.service.configuration.SenseUploadConfiguration;
import com.hello.suripu.service.configuration.SuripuConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UploadSettings {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadSettings.class);
    private static final SuripuConfiguration config = new SuripuConfiguration();

    private static final Long ALLOWABLE_TIME_BETWEEN_NOW_AND_NEXT_ALARM = 20*60*1000L;  // milliseconds
    private static final Long SUSTAINED_TIME_BETWEEN_NOW_AND_NEXT_ALARM = 60*60*1000L;  // milliseconds
    private static final Integer OVERRIDE_UPLOAD_INTERVAL = 1;

    public final DateTimeZone userTimeZone;
    public final Long userNextAlarmTimestamp;

    public UploadSettings(final DateTimeZone userTimeZone, final Long userNextAlarmTimestamp) {
        this.userTimeZone = userTimeZone;
        this.userNextAlarmTimestamp = userNextAlarmTimestamp;
    }

    public Integer getUploadInterval() {
        final Integer unadjustedUploadInterval =  computeUploadIntervalPerUserPerSetting(getUserCurrentDateTime(), config.getSenseUploadConfiguration());
        final Integer adjustedUploadInterval = adjustUploadInterval(unadjustedUploadInterval, userNextAlarmTimestamp);
        return adjustedUploadInterval;
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

        if (senseUploadConfiguration.getWeekDaysOnly()) {
            final Integer dayOfWeek = userCurrentDateTime.getDayOfWeek();
            LOGGER.debug("User Current DateTime - Day of Week: {}", userCurrentDateTime.dayOfWeek().getAsText());
            isNonPeak = isNonPeak && (dayOfWeek != DateTimeConstants.SATURDAY && dayOfWeek != DateTimeConstants.SUNDAY);
        }
        if (isNonPeak) {
            return senseUploadConfiguration.getLongInterval();
        }
        return senseUploadConfiguration.getShortInterval();
    }

    private Integer adjustUploadInterval(final Integer standardUploadInterval, final Long userNextAlarmTimestamp) {
        LOGGER.debug("diff in milliseconds {}", userNextAlarmTimestamp - DateTime.now(DateTimeZone.UTC).getMillis());
        if (userNextAlarmTimestamp - DateTime.now(DateTimeZone.UTC).getMillis() <= ALLOWABLE_TIME_BETWEEN_NOW_AND_NEXT_ALARM) {
            return OVERRIDE_UPLOAD_INTERVAL;
        }
        if (userNextAlarmTimestamp - DateTime.now(DateTimeZone.UTC).getMillis() >= SUSTAINED_TIME_BETWEEN_NOW_AND_NEXT_ALARM) {
            return standardUploadInterval;
        }
        return OVERRIDE_UPLOAD_INTERVAL;
    }

    public Integer computeUploadIntervalForTests(final DateTime dateTime) {
        return computeUploadIntervalPerUserPerSetting(dateTime, new SenseUploadConfiguration());
    }

    public Integer adjustUploadIntervalForTests(final Integer standardUploadInterval, final Long userNextAlarmTimestamp) {
        return adjustUploadInterval(standardUploadInterval, userNextAlarmTimestamp);
    }

    public Integer getOverrideUploadInterval() {
        return OVERRIDE_UPLOAD_INTERVAL;
    }
}
