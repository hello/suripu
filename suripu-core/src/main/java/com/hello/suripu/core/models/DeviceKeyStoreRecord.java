package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceKeyStoreRecord {

    @JsonProperty("key")
    public final String key;

    @JsonProperty("metadata")
    public final String metadata;

    public DeviceKeyStoreRecord(final String key, final String metadata) {
        this.key = key;
        this.metadata = metadata;
    }
}