package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class DeviceKeyStoreRecord {

    private String key = "";

    @JsonProperty("key")
    public String key() {
        return censorKey(key);
    }

    @JsonProperty("metadata")
    public final String metadata;

    @JsonProperty("created_at")
    public final String createdAt;

    private DeviceKeyStoreRecord(final String key, final String metadata, final String createdAt) {
        this.key = key;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public static DeviceKeyStoreRecord create(final String key, final String metadata, final String createdAt) {
        return new DeviceKeyStoreRecord(key, metadata, createdAt);
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