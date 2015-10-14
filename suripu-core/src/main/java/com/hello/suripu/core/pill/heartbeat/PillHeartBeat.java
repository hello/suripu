package com.hello.suripu.core.pill.heartbeat;

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
}
