package com.hello.suripu.service.models;

import com.hello.suripu.service.configuration.SenseUploadConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UploadSettingsTest {

    private final static UploadSettings uploadSettings = new UploadSettings(DateTimeZone.forID("America/Los_Angeles"));
    private final static SenseUploadConfiguration senseUploadConfiguration = new SenseUploadConfiguration();

    @Test
    public void testWeekdayNonPeakLowerBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 2, 11, 0, 0);
        final Boolean isUploadIntervalLong = uploadSettings.computeUploadIntervalForTests(userDateTime) == senseUploadConfiguration.longInterval;
        assertThat(isUploadIntervalLong, is(true));
    }

    @Test
    public void testWeekdayNonPeakUpperBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 2, 20, 59, 59);
        final Boolean isUploadIntervalLong = uploadSettings.computeUploadIntervalForTests(userDateTime) == senseUploadConfiguration.longInterval;
        assertThat(isUploadIntervalLong, is(true));
    }

    @Test
    public void testWeekdayPeakLowerBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 1, 10, 0, 0);
        final Boolean isUploadIntervalShort = uploadSettings.computeUploadIntervalForTests(userDateTime) == senseUploadConfiguration.shortInterval;
        assertThat(isUploadIntervalShort, is(true));
    }

    @Test
    public void testWeekdayPeakUpperBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 1, 10, 59, 59);
        final Boolean isUploadIntervalShort = uploadSettings.computeUploadIntervalForTests(userDateTime) == senseUploadConfiguration.shortInterval;
        assertThat(isUploadIntervalShort, is(true));
    }

    @Test
    public void testWeekendLowerBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 3, 0, 0, 0);
        final Boolean isUploadIntervalShort = uploadSettings.computeUploadIntervalForTests(userDateTime) == senseUploadConfiguration.shortInterval;
        assertThat(isUploadIntervalShort, is(true));
    }

    @Test
    public void testWeekendUpperBound() {
        final DateTime userDateTime = new DateTime(2015, 1, 4, 23, 59, 59);
        final Boolean isUploadIntervalShort = uploadSettings.computeUploadIntervalForTests(userDateTime) == senseUploadConfiguration.shortInterval;
        assertThat(isUploadIntervalShort, is(true));
    }
}
