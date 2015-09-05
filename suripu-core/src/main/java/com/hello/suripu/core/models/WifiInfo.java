package com.hello.suripu.core.models;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

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

        public static Condition fromRssi(final Integer rssi) {
            if (rssi == RSSI_NONE) {
                return Condition.NONE;
            }
            else if (rssi <= RSSI_LOW_CEILING) {
                return Condition.BAD;
            }
            else if (rssi <= RSSI_MEDIUM_CEILING) {
                return Condition.FAIR;
            }
            return Condition.GOOD;
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
    public final DateTime lastUpdated;

    private WifiInfo(final String senseId, final String ssid, final Integer rssi, final Condition condition, final DateTime lastUpdated) {
        this.senseId = senseId;
        this.ssid = ssid;
        this.rssi = rssi;
        this.condition = condition;
        this.lastUpdated = lastUpdated;
    }

    @JsonCreator
    public static WifiInfo create(@JsonProperty("sense_id") final String senseId,
                                  @JsonProperty("ssid") final String ssid,
                                  @JsonProperty("rssi") final Integer rssi,
                                  @JsonProperty("last_updated") final DateTime lastUpdated) {
        return new WifiInfo(senseId, ssid, rssi, Condition.fromRssi(rssi), lastUpdated);
    }

    public static WifiInfo createEmpty(final String senseId) {
        return WifiInfo.create(senseId, DEFAULT_SSID, DEFAULT_RSSI, DateTime.now(DateTimeZone.UTC));
    }
}
