package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class DeviceKeyStoreRecord {
    @JsonProperty("device_id")
    public final String deviceId;

    private String key = "";

    @JsonProperty("key")
    public String key() {
        return censorKey(key);
    }

    public String uncensoredKey() {
        return key;
    }

    @JsonProperty("metadata")
    public final String metadata;

    @JsonProperty("created_at")
    public final String createdAt;


    private DeviceKeyStoreRecord(final String deviceId, final String key, final String metadata, final String createdAt) {
        this.deviceId = deviceId;
        this.key = key;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public static DeviceKeyStoreRecord create(final String deviceId, final String key, final String metadata, final String createdAt) {
        return new DeviceKeyStoreRecord( deviceId, key, metadata, createdAt);
    }

    private static String censorKey(final String key) {
        if (key.isEmpty()) {
            return "";
        }
        char[] censoredParts = new char[key.length() - 8];
        Arrays.fill(censoredParts, 'x');
        return new StringBuilder(key).replace(4, key.length() - 4, new String(censoredParts)).toString();
    }
}