package com.hello.suripu.workers;


import com.google.common.collect.Maps;
import com.hello.suripu.core.models.WifiInfo;
import com.hello.suripu.workers.sense.lastSeen.SenseLastSeenProcessor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SenseLastSeenProcessorTest {
    @Test
    public void testHasSignificantRssiChange() {
        final Map<String, WifiInfo> wifiInfoHistory = Maps.newHashMap();

        wifiInfoHistory.put("sense123", WifiInfo.create("sense123", "ssid123", -75, DateTime.now(DateTimeZone.UTC)));

        assertThat(SenseLastSeenProcessor.hasSignificantRssiChange(wifiInfoHistory, "sense234", -75), is(true));
        assertThat(SenseLastSeenProcessor.hasSignificantRssiChange(wifiInfoHistory, "sense123", -75), is(false));
        assertThat(SenseLastSeenProcessor.hasSignificantRssiChange(wifiInfoHistory, "sense123", -80), is(true));
        assertThat(SenseLastSeenProcessor.hasSignificantRssiChange(wifiInfoHistory, "sense123", -66), is(true));
    }
}
