package com.hello.suripu.core.analytics;

public enum TrackingEvent {
    PILL_LOW_BATTERY("pill_low_battery"),
    UNKNOWN("unknown");

    private String value;

    TrackingEvent(String value){
        this.value = value;
    }

    public String value() {
        return value;
    }

}
