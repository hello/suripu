package com.hello.suripu.service.models;

import com.hello.suripu.service.configuration.SenseUploadConfiguration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class UploadSettingsTest {

    private SenseUploadConfiguration senseUploadConfiguration;

    @Before
    public void setUp() {
        senseUploadConfiguration = new SenseUploadConfiguration();

    }

    @Test
    public void testWeekdayNonPeakLowerBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 2, 11, 0, 0);
        final Boolean isUploadIntervalLong = UploadSettings.computeUploadIntervalPerUserPerSetting(userDateTime, senseUploadConfiguration, false) == senseUploadConfiguration.getLongInterval();
        assertThat(isUploadIntervalLong, is(true));
    }

    @Test
    public void testWeekdayNonPeakUpperBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 2, 22, 59, 59);
        final Boolean isUploadIntervalLong = UploadSettings.computeUploadIntervalPerUserPerSetting(userDateTime, senseUploadConfiguration, false) == senseUploadConfiguration.getLongInterval();
        assertThat(isUploadIntervalLong, is(true));
    }

    @Test
    public void testWeekdayPeakLowerBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 1, 10, 0, 0);
        final Boolean isUploadIntervalShort = UploadSettings.computeUploadIntervalPerUserPerSetting(userDateTime, senseUploadConfiguration, false) == senseUploadConfiguration.getShortInterval();
        assertThat(isUploadIntervalShort, is(true));
    }

    @Test
    public void testWeekdayPeakUpperBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 1, 10, 59, 59);
        final Boolean isUploadIntervalShort = UploadSettings.computeUploadIntervalPerUserPerSetting(userDateTime, senseUploadConfiguration, false) == senseUploadConfiguration.getShortInterval();
        assertThat(isUploadIntervalShort, is(true));
    }

    @Test
    public void testWeekendLowerBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 3, 0, 0, 0);
        final Boolean isUploadIntervalShort = UploadSettings.computeUploadIntervalPerUserPerSetting(userDateTime, senseUploadConfiguration, false) == senseUploadConfiguration.getShortInterval();
        assertThat(isUploadIntervalShort, is(true));
    }

    @Test
    public void testWeekendUpperBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 4, 23, 59, 59);
        final Boolean isUploadIntervalShort = UploadSettings.computeUploadIntervalPerUserPerSetting(userDateTime, senseUploadConfiguration, false) == senseUploadConfiguration.getShortInterval();
        assertThat(isUploadIntervalShort, is(true));
    }

    @Test
    public void testExpediteUploadInterval() {
        final DateTime userDateTime = new DateTime(2015, 1, 2, 12, 12, 12);
        final Long simulateCurrentTimestamp = new DateTime(2015, 1, 2, 12, 11, 12).getMillis();
        final Long nextUserAlarmTimestamp = new DateTime(2015, 1, 2, 12, 14, 12).getMillis();

        final Integer unadjustedUploadInteval = UploadSettings.computeUploadIntervalPerUserPerSetting(userDateTime, senseUploadConfiguration, false);
        final Integer adjustedUploadInterval = UploadSettings.adjustUploadIntervalInMinutes(simulateCurrentTimestamp, unadjustedUploadInteval, nextUserAlarmTimestamp);
        final Boolean isUploadIntervalOverwritten = adjustedUploadInterval == UploadSettings.getFastestUploadInterval();
        assertThat(isUploadIntervalOverwritten, is(true));
    }

    @Test
    public void testRestoreStandardUploadInterval() {
        final DateTime userDateTime = new DateTime(2015, 1, 2, 12, 12, 12);
        final Long simulateCurrentTimestamp = new DateTime(2015, 1, 12, 12, 15, 12).getMillis();
        final Long nextUserAlarmTimestamp = new DateTime(2015, 1, 12, 14, 14, 12).getMillis();

        final Integer unadjustedUploadInterval = UploadSettings.computeUploadIntervalPerUserPerSetting(userDateTime, senseUploadConfiguration, false);
        final Integer adjustedUploadInterval = UploadSettings.adjustUploadIntervalInMinutes(simulateCurrentTimestamp, unadjustedUploadInterval, nextUserAlarmTimestamp);
        final Boolean isUploadIntervalOverwritten = adjustedUploadInterval == UploadSettings.getFastestUploadInterval();
        assertThat(isUploadIntervalOverwritten, is(false));
    }
}
