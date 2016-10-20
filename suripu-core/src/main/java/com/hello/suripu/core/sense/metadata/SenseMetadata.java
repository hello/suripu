package com.hello.suripu.core.sense.metadata;

import com.hello.suripu.core.firmware.HardwareVersion;
import com.hello.suripu.core.models.device.v2.Sense;

public class SenseMetadata {

    private final String senseId;
    private final Sense.Color color;
    private final HardwareVersion hardwareVersion;

    private SenseMetadata(String senseId, Sense.Color color, HardwareVersion hardwareVersion) {
        this.senseId = senseId;
        this.color = color;
        this.hardwareVersion = hardwareVersion;
    }

    public String senseId() {
        return senseId;
    }

    public Sense.Color color() {
        return color;
    }

    public HardwareVersion hardwareVersion() {
        return hardwareVersion;
    }

    public static SenseMetadata unknown(final String senseId) {
        return new SenseMetadata(senseId, Sense.Color.UNKNOWN, null);
    }

    public static SenseMetadata create(final String senseId, final Sense.Color color, final HardwareVersion hardwareVersion) {
        return new SenseMetadata(senseId,color,hardwareVersion);
    }
}
