package com.hello.suripu.core.firmware;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class GenericFirmwareUpdateQuery {

    public final String groupName;
    public final String firmwareVersion;
    public final HardwareVersion hardwareVersion;

    public GenericFirmwareUpdateQuery(final String groupName, final String firmwareVersion, final HardwareVersion hardwareVersion) {
        this.groupName = groupName;
        this.firmwareVersion = firmwareVersion;
        this.hardwareVersion = hardwareVersion;
    }

    /**
     * Converts a Sense specific query to a generic FirmwareUpdateQuery
     * @return
     */
    public static GenericFirmwareUpdateQuery from(final SenseFirmwareUpdateQuery senseFirmwareUpdateQuery) {
        return new GenericFirmwareUpdateQuery(
                senseFirmwareUpdateQuery.group,
                senseFirmwareUpdateQuery.currentFirmwareVersion,
                senseFirmwareUpdateQuery.hwVersion
        );
    }



    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof GenericFirmwareUpdateQuery)) {
            return false;
        }

        final GenericFirmwareUpdateQuery other = (GenericFirmwareUpdateQuery) obj;
        return Objects.equal(groupName, other.groupName) &&
                Objects.equal(firmwareVersion, other.firmwareVersion) &&
                Objects.equal(hardwareVersion, other.hardwareVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(groupName, firmwareVersion, hardwareVersion);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(GenericFirmwareUpdateQuery.class)
                .add("groupname", groupName)
                .add("firmwareVersion", firmwareVersion)
                .add("hardwareVersion", hardwareVersion)
                .toString();
    }
}
