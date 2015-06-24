package com.hello.suripu.core.models;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import java.util.Map;

public class SenseLogDocument {
    private static final String ALARM_RINGING_REGEX = "(?s)^.*?(ALARM RINGING).*$";
    private static final String FIRMWARE_CRASH_REGEX = "(?s)^.*?(xkd|travis|fault).*$";
    private static final String WIFI_INFO_REGEX = "(?s)^.*?(SSID RSSI UNIQUE).*$";
    private static final String DUST_REGEX = "(?s)^.*?(dust).*$";

    public final String senseId;
    public final Long timestamp;
    public final String content;
    public final String origin;
    public final Boolean hasAlarm;
    public final Boolean hasFirmwareCrash;
    public final Boolean hasWifiInfo;
    public final Boolean hasDust;

    public SenseLogDocument(final String senseId, final Long timestamp, final String content, final String origin) {
        this.senseId = senseId;
        this.timestamp = timestamp;
        this.content = content;
        this.origin =  origin;
        this.hasAlarm = content.matches(ALARM_RINGING_REGEX);
        this.hasFirmwareCrash = content.matches(FIRMWARE_CRASH_REGEX);
        this.hasWifiInfo = content.matches(WIFI_INFO_REGEX);
        this.hasDust = content.matches(DUST_REGEX);
    }
    public Map<String, Object> toMap() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        return objectMapper.convertValue(this, Map.class);
    }
}
