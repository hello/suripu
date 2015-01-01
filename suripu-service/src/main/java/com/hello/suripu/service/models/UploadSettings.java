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


    public final DateTimeZone userTimeZone;

    public UploadSettings(final DateTimeZone userTimeZone) {
        this.userTimeZone = userTimeZone;
    }

    public Integer getUploadInterval() {
        return computeUploadIntervalPerUserPerSetting(getUserCurrentDateTime(), config.getSenseUploadConfiguration());
    }

    private DateTime getUserCurrentDateTime() {
        final DateTime userCurrentDateTime = DateTime.now(this.userTimeZone);
        return userCurrentDateTime;
    }

    private SenseUploadConfiguration getConfig() {
        return config.getSenseUploadConfiguration();
    }
    private Integer computeUploadIntervalPerUserPerSetting(DateTime userCurrentDateTime, SenseUploadConfiguration senseUploadConfiguration) {
        final Integer hourOfDay = userCurrentDateTime.getHourOfDay();

        LOGGER.debug("{} ::: {}", userCurrentDateTime);
        LOGGER.debug("User Current DateTime - Hour of Day: {}", hourOfDay);

        // Non peak times are the times whose hours are within the range defined in configuration
        Boolean isNonPeak = hourOfDay >= senseUploadConfiguration.nonPeakHourLowerBound && hourOfDay <= senseUploadConfiguration.nonPeakHourUpperBound;

        if (senseUploadConfiguration.weekDaysOnly) {
            final Integer dayOfWeek = userCurrentDateTime.getDayOfWeek();
            LOGGER.debug("User Current DateTime - Day of Week: {}", userCurrentDateTime.dayOfWeek().getAsText());
            isNonPeak = isNonPeak && (dayOfWeek != DateTimeConstants.SATURDAY && dayOfWeek != DateTimeConstants.SUNDAY);
        }
        if (isNonPeak) {
            return senseUploadConfiguration.longInterval;
        }
        return senseUploadConfiguration.shortInterval;
    }

    public Integer computeUploadIntervalForTests(DateTime dateTime) {
        return computeUploadIntervalPerUserPerSetting(dateTime, new SenseUploadConfiguration());
    }
}
