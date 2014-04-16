package com.hello.suripu.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class SoundRecord {

    @JsonProperty("device_id")
    public final Long deviceId;

    @JsonProperty("avg_max_amplitude")
    public final float averageMaxAmplitude;

    @JsonProperty("ts")
    public final DateTime dateTime;

    public SoundRecord(
            Long deviceId,
            float averageMaxAmplitude,
            DateTime dateTime
    ) {
        this.deviceId = deviceId;
        this.averageMaxAmplitude = averageMaxAmplitude;
        this.dateTime = dateTime;
    }
}
