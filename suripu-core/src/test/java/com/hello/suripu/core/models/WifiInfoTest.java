package com.hello.suripu.core.models;


import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class WifiInfoTest {
    @Test
    public void testBadCondition(){
        final WifiInfo wifiInfo = WifiInfo.create(
                "test-sense",
                "test-network",
                -90,
                DateTime.now(DateTimeZone.UTC)
        );
        assertThat(wifiInfo.condition, is(WifiInfo.Condition.BAD));
    }

    @Test
    public void testFairCondition(){
        final WifiInfo wifiInfo = WifiInfo.create(
                "test-sense",
                "test-network",
                -89,
                DateTime.now(DateTimeZone.UTC)
        );
        assertThat(wifiInfo.condition, is(WifiInfo.Condition.FAIR));
    }

    @Test
    public void testGoodCondition(){
        final WifiInfo wifiInfo = WifiInfo.create(
                "test-sense",
                "test-network",
                -59,
                DateTime.now(DateTimeZone.UTC)
        );
        assertThat(wifiInfo.condition, is(WifiInfo.Condition.GOOD));
    }

    @Test
    public void testNoneCondition(){
        final WifiInfo wifiInfo = WifiInfo.create(
                "test-sense",
                "test-network",
                WifiInfo.RSSI_NONE,
                DateTime.now(DateTimeZone.UTC)
        );
        // fake GOOD to deal with Texas Instruments bug
        assertThat(wifiInfo.condition, is(WifiInfo.Condition.GOOD));
    }
}
