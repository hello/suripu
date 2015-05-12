package com.hello.suripu.research.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by benjo on 3/20/15.
 */
public class BinnedSensorData {


    @JsonProperty("account_id")
    public final Long accountId;

    @JsonProperty("data")
    public final List<List<Double>> binnedSensorData;

    @JsonProperty("ts_utc")
    public final List<Long> timesUTC;

    @JsonProperty("timezone_offset_millis")
    public final Integer timezoneOffsetMillis;

    public BinnedSensorData(Long accountId, List<List<Double>> binnedSensorData, List<Long> timesUTC, Integer timezoneOffsetMillis) {
        this.accountId = accountId;
        this.binnedSensorData = binnedSensorData;
        this.timesUTC = timesUTC;
        this.timezoneOffsetMillis = timezoneOffsetMillis;
    }
}
