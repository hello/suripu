package com.hello.suripu.core.pill.heartbeat;

import com.google.common.base.Objects;
import org.joda.time.DateTime;

public class PillHeartBeat {

    public final String pillId;
    public final int batteryLevel;
    public final int firmwareVersion;
    public final int uptimeInSeconds;
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
