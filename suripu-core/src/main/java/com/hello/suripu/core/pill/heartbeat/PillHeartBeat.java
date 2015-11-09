package com.hello.suripu.core.pill.heartbeat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.hello.suripu.core.models.DeviceStatus;
import org.joda.time.DateTime;

public class PillHeartBeat {

    @JsonProperty("pill_id")
    public final String pillId;

    @JsonProperty("battery_level")
    public final int batteryLevel;

    @JsonProperty("firmware_version")
    public final int firmwareVersion;

    @JsonProperty("uptime")
    public final int uptimeInSeconds;

    @JsonProperty("created_at")
    public final DateTime createdAtUTC;

    private PillHeartBeat(final String pillId, final int batteryLevel, final int firmwareVersion, final int uptimeInSeconds, final DateTime createdAtUTC) {
        this.pillId = pillId;
        this.batteryLevel = batteryLevel;
        this.firmwareVersion = firmwareVersion;
        this.uptimeInSeconds = uptimeInSeconds;
        this.createdAtUTC = createdAtUTC;
    }

    /**
     * Creates a PillHeartbeat given the data contained in a pill heartbeat packet
     * @param pillId
     * @param batteryLevel
     * @param firmwareVersion
     * @param uptimeInSeconds
     * @param createdAtUTC
     * @return
     */
    public static PillHeartBeat create(final String pillId, final int batteryLevel, final int firmwareVersion, final int uptimeInSeconds, final DateTime createdAtUTC) {
        return new PillHeartBeat(pillId, batteryLevel, firmwareVersion, uptimeInSeconds, createdAtUTC);
    }


    public static PillHeartBeat fromDeviceStatus(final String pillId, final DeviceStatus deviceStatus) {
        return new PillHeartBeat(pillId, deviceStatus.batteryLevel, Integer.parseInt(deviceStatus.firmwareVersion), deviceStatus.uptime, deviceStatus.lastSeen);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final PillHeartBeat other = (PillHeartBeat) obj;
        return Objects.equal(pillId, other.pillId)
                && Objects.equal(batteryLevel, other.batteryLevel)
                && Objects.equal(firmwareVersion, other.firmwareVersion)
                && Objects.equal(uptimeInSeconds, other.uptimeInSeconds)
                && Objects.equal(createdAtUTC, other.createdAtUTC);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("pill_id", pillId)
                .add("battery_level", batteryLevel)
                .add("firmware_version", firmwareVersion)
                .add("uptime_in_seconds", uptimeInSeconds)
                .add("created_at_utc", createdAtUTC)
                .toString();
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(
                this.pillId, this.batteryLevel, this.firmwareVersion, this.uptimeInSeconds, this.createdAtUTC);

    }
}
