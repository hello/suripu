package com.hello.suripu.core.firmware;

import com.google.common.base.Objects;

public class SenseFirmwareUpdateQuery {

    public final String senseId;
    public final String group;
    public final String currentFirmwareVersion;
    public final HardwareVersion hwVersion;


    private SenseFirmwareUpdateQuery(final String senseId, final String group, final String currentFirmwareVersion, final HardwareVersion hardwareVersion) {
        this.senseId = senseId;
        this.group = group;
        this.currentFirmwareVersion = currentFirmwareVersion;
        this.hwVersion = hardwareVersion;
    }

    public static SenseFirmwareUpdateQuery forSenseOne(final String senseId, final String group, final String currentFirmwareVersion) {
        return new SenseFirmwareUpdateQuery(senseId, group, currentFirmwareVersion, HardwareVersion.SENSE_ONE);
    }

    public static SenseFirmwareUpdateQuery forSenseOneFive(final String senseId, final String group, final String currentFirmwareVersion) {
        return new SenseFirmwareUpdateQuery(senseId, group, currentFirmwareVersion, HardwareVersion.SENSE_ONE_FIVE);
    }

    public static SenseFirmwareUpdateQuery forSense(final String senseId, final String group, final String currentFirmwareVersion, final Integer hwVersionFromHeader) {
        return new SenseFirmwareUpdateQuery(senseId, group, currentFirmwareVersion, HardwareVersion.fromInt(hwVersionFromHeader));
    }

    public static SenseFirmwareUpdateQuery forSense(final String senseId, final String group, final String currentFirmwareVersion, final HardwareVersion hardwareVersion) {
        return new SenseFirmwareUpdateQuery(senseId, group, currentFirmwareVersion, hardwareVersion);
    }

    @Override
    public boolean equals(final Object obj) {

        if (obj == null || !(obj instanceof SenseFirmwareUpdateQuery)) {
            return false;
        }

        final SenseFirmwareUpdateQuery other = (SenseFirmwareUpdateQuery) obj;
        return Objects.equal(senseId, other.senseId) &&
                Objects.equal(group, other.group) &&
                Objects.equal(currentFirmwareVersion, other.currentFirmwareVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(senseId, group, currentFirmwareVersion, hwVersion);
    }
}
