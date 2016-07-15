package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class DeviceKeyStoreRecord {

    public enum Product {
        SENSE(1),
        PILL(2);

        private int value;

        Product(int value) {
            this.value = value;
        }

        public static Product fromInt(int value) {
            for(Product version : Product.values()) {
                if (version.equals(value)) {
                    return version;
                }
            }

            throw new IllegalArgumentException(String.format("Unknown product: %d", value));
        }
    }

    public enum HardwareVersion {
        SENSE_ONE(1),
        SENSE_ONE_FIVE(4);

        private int value;

        HardwareVersion(int value) {
            this.value = value;
        }

        public static HardwareVersion fromInt(int value) {
            for(HardwareVersion version : HardwareVersion.values()) {
                if (version.equals(value)) {
                    return version;
                }
            }

            throw new IllegalArgumentException(String.format("Unknown hardware version: %d", value));
        }
    }


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