package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class SoundRecord {

    @JsonProperty("device_id")
    public final Long deviceId;

    @JsonProperty("value")
    public final Integer averageMaxAmplitude;

    @JsonProperty("timestamp")
    public final DateTime dateTime;

    public SoundRecord(
            Long deviceId,
            Integer averageMaxAmplitude,
            DateTime dateTime
    ) {
        this.deviceId = deviceId;
        this.averageMaxAmplitude = averageMaxAmplitude;
        this.dateTime = dateTime;
    }
}
