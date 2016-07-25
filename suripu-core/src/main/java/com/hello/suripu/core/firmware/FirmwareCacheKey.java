package com.hello.suripu.core.firmware;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class FirmwareCacheKey {

    public final String humanReadableGroupName;
    public final HardwareVersion hardwareVersion;

    private FirmwareCacheKey(final String humanReadableGroupName, final HardwareVersion hardwareVersion) {
        this.humanReadableGroupName = humanReadableGroupName;
        this.hardwareVersion = hardwareVersion;
    }

    public static FirmwareCacheKey create(final String humanReadableGroupName, final HardwareVersion hardwareVersion) {
        return new FirmwareCacheKey(humanReadableGroupName, hardwareVersion);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof FirmwareCacheKey)) {
            return false;
        }

        final FirmwareCacheKey other = (FirmwareCacheKey) obj;
        return Objects.equal(humanReadableGroupName, other.humanReadableGroupName) &&
                Objects.equal(hardwareVersion, other.hardwareVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(humanReadableGroupName, hardwareVersion);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(GenericFirmwareUpdateQuery.class)
                .add("humanReadableGroupName", humanReadableGroupName)
                .add("hardwareVersion", hardwareVersion)
                .toString();
    }
}
