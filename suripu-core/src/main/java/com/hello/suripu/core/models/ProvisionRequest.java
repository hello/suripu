package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProvisionRequest {

    public final String deviceId;
    public final String publicKey;
    public final String metadata;

    @JsonCreator
    public ProvisionRequest(
            @JsonProperty("device_id") final String deviceId,
            @JsonProperty("public_key") final String publicKey,
            @JsonProperty("metadata") final String metadata) {

        this.deviceId = deviceId;

        this.publicKey = publicKey;

        this.metadata = metadata;

    }
}
