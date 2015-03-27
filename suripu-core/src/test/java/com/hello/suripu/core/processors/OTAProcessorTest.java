package com.hello.suripu.core.processors;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jnorgan on 3/3/15.
 */
public class OTAProcessorTest {

    @Test
    public void testOTAAlwaysOTACheck(){

        String deviceID = "fake-sense";
        final List<String> deviceGroups = Arrays.asList("group1", "group2", "group3");
        final Set<String> overrideOTAGroups =  Sets.newHashSet("override");
        final DateTimeZone userTimeZone = DateTimeZone.UTC;
        final Integer deviceUptimeDelayMinutes = 20;
        final Integer uptimeInSeconds = 10 * DateTimeConstants.SECONDS_PER_MINUTE; //This should invalidate all cases that aren't 'alwaysOTA'
        final DateTime currentDTZ = DateTime.now().withZone(DateTimeZone.forID("UTC"));
        final DateTime startOTAWindow = new DateTime(userTimeZone).withHourOfDay(11).withMinuteOfHour(0);
        final DateTime endOTAWindow = new DateTime(userTimeZone).withHourOfDay(22).withMinuteOfHour(0);
        final String ipAddress = "127.0.0.1";

        assertThat(OTAProcessor.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, deviceUptimeDelayMinutes, uptimeInSeconds, currentDTZ, startOTAWindow, endOTAWindow, true, ipAddress), is(true));

        assertThat(OTAProcessor.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, deviceUptimeDelayMinutes, uptimeInSeconds, currentDTZ, startOTAWindow, endOTAWindow, false, ipAddress), is(false));
    }

    @Test
    public void testOTAUpdateTimeWindowCheck(){

        String deviceID = "fake-sense";
        final List<String> deviceGroups = Arrays.asList("group1", "group2", "group3");
        final Set<String> overrideOTAGroups =  Sets.newHashSet("override");
        final DateTimeZone userTimeZone = DateTimeZone.UTC;
        final Integer deviceUptimeDelayMinutes = 20;
        final Integer uptimeInSeconds = 21 * DateTimeConstants.SECONDS_PER_MINUTE;
        final DateTime badDTZ = new DateTime(userTimeZone).withHourOfDay(8).withMinuteOfHour(0);
        final DateTime goodDTZ = new DateTime(userTimeZone).withHourOfDay(14).withMinuteOfHour(0);
        final DateTime startOTAWindow = new DateTime(userTimeZone).withHourOfDay(11).withMinuteOfHour(0);
        final DateTime endOTAWindow = new DateTime(userTimeZone).withHourOfDay(22).withMinuteOfHour(0);
        final Boolean alwaysOTA = false;
        final String ipAddress = "127.0.0.1";

        assertThat(OTAProcessor.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, deviceUptimeDelayMinutes, uptimeInSeconds, goodDTZ, startOTAWindow, endOTAWindow, alwaysOTA, ipAddress), is(true));
        assertThat(OTAProcessor.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, deviceUptimeDelayMinutes, uptimeInSeconds, badDTZ, startOTAWindow, endOTAWindow, alwaysOTA, ipAddress), is(false));
    }

    @Test
    public void testOTAUptimeCheck(){

        String deviceID = "fake-sense";
        final List<String> deviceGroups = Arrays.asList("group1", "group2", "group3");
        final Set<String> overrideOTAGroups =  Sets.newHashSet("override");
        final DateTimeZone userTimeZone = DateTimeZone.UTC;
        final DateTime goodDTZ = new DateTime(userTimeZone).withHourOfDay(14).withMinuteOfHour(0);
        final DateTime startOTAWindow = new DateTime(userTimeZone).withHourOfDay(11).withMinuteOfHour(0);
        final DateTime endOTAWindow = new DateTime(userTimeZone).withHourOfDay(22).withMinuteOfHour(0);
        final Boolean alwaysOTA = false;
        final String ipAddress = "127.0.0.1";

        assertThat(OTAProcessor.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, 20, 1201, goodDTZ, startOTAWindow, endOTAWindow, alwaysOTA, ipAddress), is(true));

        assertThat(OTAProcessor.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, 20, 1200, goodDTZ, startOTAWindow, endOTAWindow, alwaysOTA, ipAddress), is(false));
    }

    @Test
    public void testOTAGroupOverrides(){

        String deviceID = "fake-sense";
        final List<String> deviceGroups = Arrays.asList("group1", "group2", "group3");
        final Set<String> overrideOTAGroups =  Sets.newHashSet("group1");
        final Set<String> nonOverrideOTAGroups =  Sets.newHashSet("group4");
        final DateTimeZone userTimeZone = DateTimeZone.UTC;
        final Integer deviceUptimeDelayMinutes = 20;
        final Integer uptimeInSeconds = 19 * DateTimeConstants.SECONDS_PER_MINUTE; //Should cause canOTA to be false unless overridden by group membership
        final DateTime goodDTZ = new DateTime(userTimeZone).withHourOfDay(14).withMinuteOfHour(0);
        final DateTime startOTAWindow = new DateTime(userTimeZone).withHourOfDay(11).withMinuteOfHour(0);
        final DateTime endOTAWindow = new DateTime(userTimeZone).withHourOfDay(22).withMinuteOfHour(0);
        final Boolean alwaysOTA = false;
        final String ipAddress = "127.0.0.1";

        assertThat(OTAProcessor.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, deviceUptimeDelayMinutes, uptimeInSeconds, goodDTZ, startOTAWindow, endOTAWindow, alwaysOTA, ipAddress), is(true));

        assertThat(OTAProcessor.canDeviceOTA(deviceID, deviceGroups, nonOverrideOTAGroups, deviceUptimeDelayMinutes, uptimeInSeconds, goodDTZ, startOTAWindow, endOTAWindow, alwaysOTA, ipAddress), is(false));
    }

    @Test
    public void testOTAPCHExclusionCheck(){

        String deviceID = "fake-sense";
        final List<String> deviceGroups = Arrays.asList("group1", "group2", "group3");
        final Set<String> overrideOTAGroups =  Sets.newHashSet("override");
        final DateTimeZone userTimeZone = DateTimeZone.UTC;
        final Integer deviceUptimeDelayMinutes = 20;
        final Integer uptimeInSeconds = 10 * DateTimeConstants.SECONDS_PER_MINUTE; //This should invalidate all cases that aren't 'alwaysOTA'
        final DateTime currentDTZ = DateTime.now().withZone(DateTimeZone.forID("UTC"));
        final DateTime startOTAWindow = new DateTime(userTimeZone).withHourOfDay(11).withMinuteOfHour(0);
        final DateTime endOTAWindow = new DateTime(userTimeZone).withHourOfDay(22).withMinuteOfHour(0);
        final String pchAddress1 = "203.166.220.233";
        final String pchAddress2 = "116.204.105.38";
        final String nonPchAddress = "127.0.0.1";
        final String badAddress = "bad Address";

        assertThat(OTAProcessor.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, deviceUptimeDelayMinutes, uptimeInSeconds, currentDTZ, startOTAWindow, endOTAWindow, true, pchAddress1), is(false));
        assertThat(OTAProcessor.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, deviceUptimeDelayMinutes, uptimeInSeconds, currentDTZ, startOTAWindow, endOTAWindow, true, pchAddress2), is(false));
        assertThat(OTAProcessor.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, deviceUptimeDelayMinutes, uptimeInSeconds, currentDTZ, startOTAWindow, endOTAWindow, true, nonPchAddress), is(true));
        assertThat(OTAProcessor.canDeviceOTA(deviceID, deviceGroups, overrideOTAGroups, deviceUptimeDelayMinutes, uptimeInSeconds, currentDTZ, startOTAWindow, endOTAWindow, true, badAddress), is(true));
    }
}
