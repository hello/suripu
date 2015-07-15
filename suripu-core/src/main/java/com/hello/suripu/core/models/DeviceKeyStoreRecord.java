package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceKeyStoreRecord {

    @JsonProperty("key")
    public final String key;

    @JsonProperty("metadata")
    public final String metadata;

    @JsonProperty("created_at")
    public final String createdAt;

    public DeviceKeyStoreRecord(final String key, final String metadata, final String createdAt) {
        this.key = key;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }
}