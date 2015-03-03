package com.hello.suripu.service.resources;

import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.service.configuration.SenseUploadConfiguration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by pangwu on 1/23/15.
 */
public class ReceiveResourceTest {

    @Test
    public void testComputeNextUploadInterval(){
        final SenseUploadConfiguration senseUploadConfiguration = new SenseUploadConfiguration();
        final long actualRingTime = DateTime.now().plusMinutes(3).withSecondOfMinute(0).withMillisOfSecond(0).getMillis();

        final RingTime nextRingTime = new RingTime(actualRingTime, actualRingTime, new long[0], false);
        final int uploadCycle = ReceiveResource.computeNextUploadInterval(nextRingTime, DateTime.now(), senseUploadConfiguration);
        assertThat(uploadCycle, is(1));

    }

    @Test
    public void testShouldWriteRingHistory(){
        final DateTime now = DateTime.now();
        final DateTime actualRingTime = now.plusMinutes(2);
        final DateTime expectedRingTime = now.plusMinutes(3);
        final RingTime ringTime = new RingTime(actualRingTime.getMillis(), expectedRingTime.getMillis(), new long[0], true);

        assertThat(ReceiveResource.shouldWriteRingTimeHistory(now, ringTime, 3), is(true));
        assertThat(ReceiveResource.shouldWriteRingTimeHistory(now, ringTime, 1), is(false));
        assertThat(ReceiveResource.shouldWriteRingTimeHistory(now.plusMinutes(5), ringTime, 1), is(false));
    }


    @Test
    public void testComputeNextUploadInterval1HourInFuture(){


        for(int i = 1; i < DateTimeConstants.MINUTES_PER_DAY; i++) {
            final SenseUploadConfiguration senseUploadConfiguration = new SenseUploadConfiguration();
            final long actualRingTime = DateTime.now().plusMinutes(i).withSecondOfMinute(0).withMillisOfSecond(0).getMillis();

            final RingTime nextRingTime = new RingTime(actualRingTime, actualRingTime, new long[0], false);
            final int uploadCycle = ReceiveResource.computeNextUploadInterval(nextRingTime, DateTime.now(), senseUploadConfiguration);
            assertThat(uploadCycle <= senseUploadConfiguration.getLongInterval(), is(true));
        }
    }

    @Test
    public void testComputeNextUploadIntervalAlarmInThePast(){


        for(int i = 1; i < DateTimeConstants.MINUTES_PER_DAY; i++) {
            final SenseUploadConfiguration senseUploadConfiguration = new SenseUploadConfiguration();
            final long actualRingTime = DateTime.now().minusMinutes(i).withSecondOfMinute(0).withMillisOfSecond(0).getMillis();

            final RingTime nextRingTime = new RingTime(actualRingTime, actualRingTime, new long[0], false);
            final int uploadCycle = ReceiveResource.computeNextUploadInterval(nextRingTime, DateTime.now(), senseUploadConfiguration);
            assertThat(uploadCycle <= senseUploadConfiguration.getLongInterval(), is(true));
        }
    }

    @Test
    public void testComputeNextUploadIntervalShouldBePositive(){

        for(int i = 1; i < DateTimeConstants.MINUTES_PER_DAY; i++) {
            final SenseUploadConfiguration senseUploadConfiguration = new SenseUploadConfiguration();
            final long actualRingTime = DateTime.now().minusMinutes(i).withSecondOfMinute(0).withMillisOfSecond(0).getMillis();

            final RingTime nextRingTime = new RingTime(actualRingTime, actualRingTime, new long[0], false);
            final int uploadCycle = ReceiveResource.computeNextUploadInterval(nextRingTime, DateTime.now(), senseUploadConfiguration);
            assertThat(uploadCycle > 0, is(true));
        }
    }

    @Test
    public void testComputeNextUploadIntervalRandomNow(){

        final Random random = new Random(DateTime.now().getMillis());

        for(int i = 1; i < DateTimeConstants.MINUTES_PER_DAY; i++) {
            final SenseUploadConfiguration senseUploadConfiguration = new SenseUploadConfiguration();
            final long actualRingTime = DateTime.now().plusMinutes(i).withSecondOfMinute(0).withMillisOfSecond(0).getMillis();

            final DateTime current = new DateTime(random.nextLong());
            final RingTime nextRingTime = new RingTime(actualRingTime, actualRingTime, new long[0], false);
            final int uploadCycle = ReceiveResource.computeNextUploadInterval(nextRingTime, current, senseUploadConfiguration);
            assertThat(uploadCycle <= senseUploadConfiguration.getLongInterval(), is(true));
        }
    }

    @Test
    public void testComputePassRingTimeUploadIntervalRandomNow(){

        final Random random = new Random(DateTime.now().getMillis());

        for(int i = 1; i < DateTimeConstants.MINUTES_PER_DAY; i++) {
            final long actualRingTime = DateTime.now().plusMinutes(i).withSecondOfMinute(0).withMillisOfSecond(0).getMillis();

            final DateTime current = new DateTime(random.nextLong());
            final RingTime nextRingTime = new RingTime(actualRingTime, actualRingTime, new long[0], false);
            final int uploadCycle = ReceiveResource.computePassRingTimeUploadInterval(nextRingTime, current, 10);
            assertThat(uploadCycle <= 10, is(true));
        }
    }

    @Test
    public void testOTAAlwaysOTACheck(){

        String deviceID = "fake-sense";
        final List<String> deviceGroups = Arrays.asList("group1", "group2", "group3");
        final Set<String> overrideOTAGroups = new HashSet<String>();
        overrideOTAGroups.add("override");
        final DateTimeZone userTimeZone = DateTimeZone.UTC;
        final Integer deviceUptimeDelayMinutes = 20;
        final Integer uptimeInSeconds = 10 * DateTimeConstants.SECONDS_PER_MINUTE; //This should invalidate all cases that aren't 'alwaysOTA'
        final DateTime currentDTZ = DateTime.now().withZone(DateTimeZone.forID("UTC"));
        final DateTime startOTAWindow = new DateTime(userTimeZone).withHourOfDay(11).withMinuteOfHour(0);
        final DateTime endOTAWindow = new DateTime(userTimeZone).withHourOfDay(22).withMinuteOfHour(0);

        assertThat(ReceiveResource.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, deviceUptimeDelayMinutes, uptimeInSeconds, currentDTZ, startOTAWindow, endOTAWindow, true), is(true));

        assertThat(ReceiveResource.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, deviceUptimeDelayMinutes, uptimeInSeconds, currentDTZ, startOTAWindow, endOTAWindow, false), is(false));
    }

    @Test
    public void testOTAUpdateTimeWindowCheck(){

        String deviceID = "fake-sense";
        final List<String> deviceGroups = Arrays.asList("group1", "group2", "group3");
        final Set<String> overrideOTAGroups = new HashSet<String>();
        overrideOTAGroups.add("override");
        final DateTimeZone userTimeZone = DateTimeZone.UTC;
        final Integer deviceUptimeDelayMinutes = 20;
        final Integer uptimeInSeconds = 21 * DateTimeConstants.SECONDS_PER_MINUTE;
        final DateTime badDTZ = new DateTime(userTimeZone).withHourOfDay(8).withMinuteOfHour(0);
        final DateTime goodDTZ = new DateTime(userTimeZone).withHourOfDay(14).withMinuteOfHour(0);
        final DateTime startOTAWindow = new DateTime(userTimeZone).withHourOfDay(11).withMinuteOfHour(0);
        final DateTime endOTAWindow = new DateTime(userTimeZone).withHourOfDay(22).withMinuteOfHour(0);
        final Boolean alwaysOTA = false;

        assertThat(ReceiveResource.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, deviceUptimeDelayMinutes, uptimeInSeconds, goodDTZ, startOTAWindow, endOTAWindow, alwaysOTA), is(true));
        
        assertThat(ReceiveResource.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, deviceUptimeDelayMinutes, uptimeInSeconds, badDTZ, startOTAWindow, endOTAWindow, alwaysOTA), is(false));
    }

    @Test
    public void testOTAUptimeCheck(){

        String deviceID = "fake-sense";
        final List<String> deviceGroups = Arrays.asList("group1", "group2", "group3");
        final Set<String> overrideOTAGroups = new HashSet<String>();
        overrideOTAGroups.add("override");
        final DateTimeZone userTimeZone = DateTimeZone.UTC;
        final DateTime goodDTZ = new DateTime(userTimeZone).withHourOfDay(14).withMinuteOfHour(0);
        final DateTime startOTAWindow = new DateTime(userTimeZone).withHourOfDay(11).withMinuteOfHour(0);
        final DateTime endOTAWindow = new DateTime(userTimeZone).withHourOfDay(22).withMinuteOfHour(0);
        final Boolean alwaysOTA = false;

        assertThat(ReceiveResource.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, 20, 1201, goodDTZ, startOTAWindow, endOTAWindow, alwaysOTA), is(true));

        assertThat(ReceiveResource.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, 20, 1200, goodDTZ, startOTAWindow, endOTAWindow, alwaysOTA), is(false));
    }

    @Test
    public void testOTAGroupOverrides(){

        String deviceID = "fake-sense";
        final List<String> deviceGroups = Arrays.asList("group1", "group2", "group3");
        final Set<String> overrideOTAGroups = new HashSet<String>();
        overrideOTAGroups.add("group1");
        final Set<String> nonOverrideOTAGroups = new HashSet<String>();
        overrideOTAGroups.add("group4");
        final DateTimeZone userTimeZone = DateTimeZone.UTC;
        final Integer deviceUptimeDelayMinutes = 20;
        final Integer uptimeInSeconds = 19 * DateTimeConstants.SECONDS_PER_MINUTE; //Should cause canOTA to be false unless overridden by group membership
        final DateTime goodDTZ = new DateTime(userTimeZone).withHourOfDay(14).withMinuteOfHour(0);
        final DateTime startOTAWindow = new DateTime(userTimeZone).withHourOfDay(11).withMinuteOfHour(0);
        final DateTime endOTAWindow = new DateTime(userTimeZone).withHourOfDay(22).withMinuteOfHour(0);
        final Boolean alwaysOTA = false;

        assertThat(ReceiveResource.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, deviceUptimeDelayMinutes, uptimeInSeconds, goodDTZ, startOTAWindow, endOTAWindow, alwaysOTA), is(true));

        assertThat(ReceiveResource.canDeviceOTA(deviceID, deviceGroups, nonOverrideOTAGroups, deviceUptimeDelayMinutes, uptimeInSeconds, goodDTZ, startOTAWindow, endOTAWindow, alwaysOTA), is(false));
    }
}
