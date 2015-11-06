package com.hello.suripu.service.models;

import com.hello.suripu.service.configuration.SenseUploadConfiguration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class UploadSettingsTest {

    private SenseUploadConfiguration senseUploadConfiguration = new SenseUploadConfiguration();

    @Before
    public void setUp() {
        senseUploadConfiguration = new SenseUploadConfiguration();

    }

    @Test
    public void testNonPeakWeekdays() {
        final Integer defaultInterval = senseUploadConfiguration.getDefaultUploadInterval();
        final Integer increasedNonPeakInterval = senseUploadConfiguration.getIncreasedNonPeakUploadInterval();
        Integer interval;


        final DateTime nonPeakDTStart = new DateTime(2015, 1, 2, 11, 0, 0); // 10am, thursday
        final DateTime nonPeakDTEnd = new DateTime(2015, 1, 2, 22, 59, 59); //10:59:50pm friday

        // default interval
        interval = UploadSettings.computeUploadIntervalPerUserPerSetting(nonPeakDTStart, senseUploadConfiguration, false);
        assertThat(interval, is(defaultInterval));

        interval = UploadSettings.computeUploadIntervalPerUserPerSetting(nonPeakDTEnd, senseUploadConfiguration, false);
        assertThat(interval, is(defaultInterval));

        // increased upload interval
        interval = UploadSettings.computeUploadIntervalPerUserPerSetting(nonPeakDTStart, senseUploadConfiguration, true);
        assertThat(interval, is(increasedNonPeakInterval));

        interval = UploadSettings.computeUploadIntervalPerUserPerSetting(nonPeakDTEnd, senseUploadConfiguration, true);
        assertThat(interval, is(increasedNonPeakInterval));
    }

    @Test
    public void testPeakWeekdays() {
        final Integer defaultInterval = senseUploadConfiguration.getDefaultUploadInterval();
        final Integer increasedPeakInterval = senseUploadConfiguration.getIncreasedPeakUploadInterval();

        Integer interval;

        final DateTime peakDTStart = new DateTime(2015, 1, 1, 23, 0, 0); // 11pm thursday
        final DateTime peakDTEnd = new DateTime(2015, 1, 2, 10, 59, 59); // 10:59:59am friday

        // default interval
        interval = UploadSettings.computeUploadIntervalPerUserPerSetting(peakDTStart, senseUploadConfiguration, false);
        assertThat(interval, is(defaultInterval));

        interval = UploadSettings.computeUploadIntervalPerUserPerSetting(peakDTEnd, senseUploadConfiguration, false);
        assertThat(interval, is(defaultInterval));

        // increased interval
        interval = UploadSettings.computeUploadIntervalPerUserPerSetting(peakDTStart, senseUploadConfiguration, true);
        assertThat(interval, is(increasedPeakInterval));

        interval = UploadSettings.computeUploadIntervalPerUserPerSetting(peakDTEnd, senseUploadConfiguration, true);
        assertThat(interval, is(increasedPeakInterval));

    }

    public void testWeekend() {

        final Integer defaultInterval = senseUploadConfiguration.getDefaultUploadInterval();
        final Integer increasedPeakInterval = senseUploadConfiguration.getIncreasedPeakUploadInterval();

        Integer interval;

        // weekend, default
        final DateTime weekendDTStart = new DateTime(2015, 1, 3, 0, 0, 0); // Saturday midnight
        interval = UploadSettings.computeUploadIntervalPerUserPerSetting(weekendDTStart, senseUploadConfiguration, false);
        assertThat(interval, is(defaultInterval));

        final DateTime weekendDTEnds = new DateTime(2015, 1, 4, 23, 59, 59); // Sunday 11:59:59pm
        interval = UploadSettings.computeUploadIntervalPerUserPerSetting(weekendDTStart, senseUploadConfiguration, false);
        assertThat(interval, is(defaultInterval));

        // increased interval -- note, weekends are treated as peak
        interval = UploadSettings.computeUploadIntervalPerUserPerSetting(weekendDTStart, senseUploadConfiguration, true);
        assertThat(interval, is(increasedPeakInterval));

        interval = UploadSettings.computeUploadIntervalPerUserPerSetting(weekendDTStart, senseUploadConfiguration, true);
        assertThat(interval, is(increasedPeakInterval));

    }

    @Test
    public void testExpediteUploadInterval() {
        final DateTime userDateTime = new DateTime(2015, 1, 2, 12, 12, 12); // Friday, 12:12:12pm (non-peak)
        final Long simulateCurrentTimestamp = new DateTime(2015, 1, 2, 12, 11, 12).getMillis();
        final Long nextUserAlarmTimestamp = new DateTime(2015, 1, 2, 12, 14, 12).getMillis();

        // test with increased interval set to true
        final Integer unadjustedUploadInterval = UploadSettings.computeUploadIntervalPerUserPerSetting(userDateTime, senseUploadConfiguration, true);
        final Integer adjustedUploadInterval = UploadSettings.adjustUploadIntervalInMinutes(simulateCurrentTimestamp, unadjustedUploadInterval, nextUserAlarmTimestamp);
        assertThat(unadjustedUploadInterval > adjustedUploadInterval, is(true));
        assertThat(adjustedUploadInterval, is(UploadSettings.getFastestUploadInterval()));
    }

    @Test
    public void testRestoreStandardUploadInterval() {
        final DateTime userDateTime = new DateTime(2015, 1, 2, 12, 12, 12);
        final Long simulateCurrentTimestamp = new DateTime(2015, 1, 12, 12, 15, 12).getMillis();
        final Long nextUserAlarmTimestamp = new DateTime(2015, 1, 12, 14, 14, 12).getMillis();

        final Integer unadjustedUploadInterval = UploadSettings.computeUploadIntervalPerUserPerSetting(userDateTime, senseUploadConfiguration, false);
        final Integer adjustedUploadInterval = UploadSettings.adjustUploadIntervalInMinutes(simulateCurrentTimestamp, unadjustedUploadInterval, nextUserAlarmTimestamp);
        assertThat(adjustedUploadInterval.equals(unadjustedUploadInterval), is(true));
    }
}
