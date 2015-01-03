package com.hello.suripu.service.models;

import com.hello.suripu.service.configuration.SenseUploadConfiguration;

public class UploadSettingsTest {

    private final static SenseUploadConfiguration senseUploadConfiguration = new SenseUploadConfiguration();

    /*
    @Test
    public void testWeekdayNonPeakLowerBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 2, 11, 0, 0);
        final Long nextUserAlarmTimestamp = new DateTime(2015, 1, 2, 14, 0, 0).getMillis();
        final UploadSettings uploadSettings = new UploadSettings(DateTimeZone.getDefault(), nextUserAlarmTimestamp);
        final Boolean isUploadIntervalLong = uploadSettings.computeUploadIntervalForTests(userDateTime) == senseUploadConfiguration.getLongInterval();
        assertThat(isUploadIntervalLong, is(true));
    }

    @Test
    public void testWeekdayNonPeakUpperBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 2, 20, 59, 59);
        final Long nextUserAlarmTimestamp = new DateTime(2015, 1, 2, 23, 0, 0).getMillis();
        final UploadSettings uploadSettings = new UploadSettings(DateTimeZone.getDefault(), nextUserAlarmTimestamp);
        final Boolean isUploadIntervalLong = uploadSettings.computeUploadIntervalForTests(userDateTime) == senseUploadConfiguration.getLongInterval();
        assertThat(isUploadIntervalLong, is(true));
    }

    @Test
    public void testWeekdayPeakLowerBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 1, 10, 0, 0);
        final Long nextUserAlarmTimestamp = new DateTime(2015, 1, 1, 12, 30, 0).getMillis();
        final UploadSettings uploadSettings = new UploadSettings(DateTimeZone.getDefault(), nextUserAlarmTimestamp);
        final Boolean isUploadIntervalShort = uploadSettings.computeUploadIntervalForTests(userDateTime) == senseUploadConfiguration.getShortInterval();
        assertThat(isUploadIntervalShort, is(true));
    }

    @Test
    public void testWeekdayPeakUpperBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 1, 10, 59, 59);
        final Long nextUserAlarmTimestamp = new DateTime(2015, 1, 1, 16, 0, 0).getMillis();
        final UploadSettings uploadSettings = new UploadSettings(DateTimeZone.getDefault(), nextUserAlarmTimestamp);
        final Boolean isUploadIntervalShort = uploadSettings.computeUploadIntervalForTests(userDateTime) == senseUploadConfiguration.getShortInterval();
        assertThat(isUploadIntervalShort, is(true));
    }

    @Test
    public void testWeekendLowerBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 3, 0, 0, 0);
        final Long nextUserAlarmTimestamp = new DateTime(2015, 1, 3, 5, 0, 0).getMillis();
        final UploadSettings uploadSettings = new UploadSettings(DateTimeZone.getDefault(), nextUserAlarmTimestamp);
        final Boolean isUploadIntervalShort = uploadSettings.computeUploadIntervalForTests(userDateTime) == senseUploadConfiguration.getShortInterval();
        assertThat(isUploadIntervalShort, is(true));
    }

    @Test
    public void testWeekendUpperBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 4, 23, 59, 59);
        final Long nextUserAlarmTimestamp = new DateTime(2015, 1, 2, 14, 0, 0).getMillis();
        final UploadSettings uploadSettings = new UploadSettings(DateTimeZone.getDefault(), nextUserAlarmTimestamp);
        final Boolean isUploadIntervalShort = uploadSettings.computeUploadIntervalForTests(userDateTime) == senseUploadConfiguration.getShortInterval();
        assertThat(isUploadIntervalShort, is(true));
    }

    @Test
    public void testExpediteUploadInterval() {
        final DateTime userDateTime = new DateTime(2015, 1, 2, 12, 12, 12);
        final Long nextUserAlarmTimestamp = new DateTime(2015, 1, 2, 12, 14, 12).getMillis();
        final UploadSettings uploadSettings = new UploadSettings(DateTimeZone.getDefault(), nextUserAlarmTimestamp);
        final Integer unadjustedUploadInteval = uploadSettings.computeUploadIntervalForTests(userDateTime);
        final Integer adjustedUploadInterval = uploadSettings.adjustUploadIntervalForTests(unadjustedUploadInteval, nextUserAlarmTimestamp);
        final Boolean isUploadIntervalOverwritten = adjustedUploadInterval == uploadSettings.getFastestUploadInterval();
        assertThat(isUploadIntervalOverwritten, is(true));
    }

    @Test
    public void testRestoreStandardUploadInterval() {
        final DateTime userDateTime = new DateTime(2015, 1, 2, 12, 12, 12);
        final Long nextUserAlarmTimestamp = new DateTime(2015, 1, 12, 13, 14, 12).getMillis();
        final UploadSettings uploadSettings = new UploadSettings(DateTimeZone.getDefault(), nextUserAlarmTimestamp);
        final Integer unadjustedUploadInterval = uploadSettings.computeUploadIntervalForTests(userDateTime);
        final Integer adjustedUploadInterval = uploadSettings.adjustUploadIntervalForTests(unadjustedUploadInterval, nextUserAlarmTimestamp);
        final Boolean isUploadIntervalOverwritten = adjustedUploadInterval == uploadSettings.getFastestUploadInterval();
        assertThat(isUploadIntervalOverwritten, is(false));
    }
    */
}
