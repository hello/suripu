package com.hello.suripu.core.sense.metadata;

import com.google.common.base.Optional;
import com.hello.suripu.core.firmware.HardwareVersion;
import com.hello.suripu.core.models.device.v2.Sense;

public class SenseMetadata {

    private final String senseId;
    private final Sense.Color color;
    private final HardwareVersion hardwareVersion;
    private final Optional<Long> primaryAccountId;

    private SenseMetadata(String senseId, Sense.Color color, HardwareVersion hardwareVersion, Long primaryAccountId) {
        this.senseId = senseId;
        this.color = color;
        this.hardwareVersion = hardwareVersion;
        this.primaryAccountId = Optional.fromNullable(primaryAccountId);
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

    public boolean hasPrimaryAccountId() {
        return primaryAccountId.isPresent();
    }

    public Long primaryAccountId() {
        return primaryAccountId.orNull();
    }

    public static SenseMetadata unknown(final String senseId) {
        return new SenseMetadata(senseId, Sense.Color.UNKNOWN, null, null);
    }

    public static SenseMetadata create(final String senseId, final Sense.Color color, final HardwareVersion hardwareVersion, final Long primaryAccountId) {
        return new SenseMetadata(senseId,color,hardwareVersion, primaryAccountId);
    }
}
