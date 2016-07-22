package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.firmware.HardwareVersion;
import com.hello.suripu.core.firmware.Product;

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

    @JsonProperty("product")
    public final Product product;

    @JsonProperty("hw_version")
    public final HardwareVersion hardwareVersion;

    private DeviceKeyStoreRecord(final String deviceId, final String key, final String metadata, final String createdAt, final HardwareVersion version, final Product product) {
        this.deviceId = deviceId;
        this.key = key;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.hardwareVersion = version;
        this.product = product;
    }

    @Deprecated
    public static DeviceKeyStoreRecord create(final String deviceId, final String key, final String metadata, final String createdAt) {
        return new DeviceKeyStoreRecord(deviceId, key, metadata, createdAt, HardwareVersion.SENSE_ONE, Product.SENSE);
    }

    public static DeviceKeyStoreRecord forSense(final String deviceId, final String key, final String metadata, final String createdAt, final HardwareVersion version) {
        return new DeviceKeyStoreRecord( deviceId, key, metadata, createdAt, version, Product.SENSE);
    }

    public static DeviceKeyStoreRecord forPill(final String deviceId, final String key, final String metadata, final String createdAt, final HardwareVersion version) {
        return new DeviceKeyStoreRecord( deviceId, key, metadata, createdAt, version, Product.PILL);
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