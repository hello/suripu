package com.hello.suripu.core.logging;

public enum SenseLogTag {

    ALARM_RINGING("alarm_ringing"),
    FIRMWARE_CRASH("firmware_crash"),
    WIFI_INFO("wifi_info"),
    DUST_STATS("dust_stats");

    public final String value;

    SenseLogTag(String value){
        this.value = value;
    }
}

