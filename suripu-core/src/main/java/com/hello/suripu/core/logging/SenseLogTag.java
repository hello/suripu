package com.hello.suripu.core.logging;

import java.util.ArrayList;
import java.util.List;

public enum SenseLogTag {

    ALARM_RINGING("alarm_ringing"),
    FIRMWARE_CRASH("firmware_crash"),
    WIFI_INFO("wifi_info"),
    DUST_STATS("dust_stats");

    public final String value;

    SenseLogTag(String value){
        this.value = value;
    }

    public static List<String> rawValues() {
        final List<String> rawValues = new ArrayList<>();
        for (final SenseLogTag senseLogTag : SenseLogTag.values()) {
            rawValues.add(senseLogTag.value);
        }
        return rawValues;
    }
}

