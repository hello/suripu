package com.hello.suripu.core.models;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WifiInfo {

    public final static String DEFAULT_SSID = "";
    public final static Integer DEFAULT_RSSI = 0;

    public final static Integer RSSI_LOW_CEILING = -90;
    public final static Integer RSSI_MEDIUM_CEILING = -60;
    public final static Integer RSSI_NONE = 0;

    public enum Condition {
        NONE("NONE"),
        BAD("BAD"),
        FAIR("FAIR"),
        GOOD("GOOD");

        private final String value;
        Condition(final String value) {
            this.value = value;
        }
    }

    @JsonProperty("sense_id")
    public final String senseId;

    @JsonProperty("ssid")
    public final String ssid;

    @JsonProperty("rssi")
    public final Integer rssi;

    @JsonProperty("condition")
    public final Condition condition;

    @JsonProperty("last_updated")
    public final Long lastUpdated;

    @JsonCreator
    private WifiInfo(final String senseId, final String ssid, final Integer rssi, final Condition condition, final Long lastUpdated) {
        this.senseId = senseId;
        this.ssid = ssid;
        this.rssi = rssi;
        this.condition = condition;
        this.lastUpdated = lastUpdated;
    }

    public static WifiInfo create(final String senseId, final String ssid, final Integer rssi, final Long lastUpdated) {
        return new WifiInfo(senseId, ssid, rssi, getCondition(rssi), lastUpdated);
    }

    public static WifiInfo createEmpty(final String senseId) {
        return WifiInfo.create(senseId, DEFAULT_SSID, DEFAULT_RSSI, 0L);
    }

    private static Condition getCondition(final Integer rssi) {
        if (rssi == RSSI_NONE) {
            return Condition.NONE;
        }
        else if (rssi <= RSSI_LOW_CEILING) {
            return Condition.BAD;
        }
        else if (rssi <= RSSI_MEDIUM_CEILING) {
            return Condition.FAIR;
        }
        else {
            return Condition.GOOD;
        }
    }
}
