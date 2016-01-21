package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by jnorgan on 3/5/15.
 */
public class FirmwareInfo {

    @JsonProperty("middle_version")
    public final String middleVersion;

    @JsonProperty("top_version")
    public final String topVersion;

    @JsonProperty("device_id")
    public final String device_id;

    @JsonProperty("timestamp")
    public final Long timestamp;


    public FirmwareInfo(final String middleVersion, final String topVersion, final String device_id, final Long timestamp) {
        this.middleVersion = middleVersion;
        this.topVersion = topVersion;
        this.device_id = device_id;
        this.timestamp = timestamp;
    }
}