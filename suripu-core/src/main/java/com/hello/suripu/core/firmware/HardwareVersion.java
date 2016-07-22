package com.hello.suripu.core.firmware;


public enum HardwareVersion {
    SENSE_ONE(1),
    SENSE_ONE_FIVE(4);

    public int value;

    HardwareVersion(int value) {
        this.value = value;
    }

    public static HardwareVersion fromInt(int value) {
        for(HardwareVersion version : HardwareVersion.values()) {
            if (version.value == value) {
                return version;
            }
        }

        throw new IllegalArgumentException(String.format("Unknown hardware version: %d", value));
    }
}
