package com.hello.suripu.core.models;


import com.fasterxml.jackson.annotation.JsonProperty;

public class Calibration {
    private final static Integer DEFAULT_DUST_OFFSET_VALUE = -1;
    private final static Float DUST_CALIBRATION_BASE = 300f;
    private final static Float DUST_CALIBRATION_K_FACTOR = 1.5f;



    @JsonProperty("sense_id")
    public final String senseId;

    @JsonProperty("dust_offset")
    public final Integer dustOffset;

    public Calibration(final String senseId, final Integer dustOffset) {
        this.senseId = senseId;
        this.dustOffset = dustOffset;
    }

    public static final Calibration createDefault(final String senseId) {
        return new Calibration(senseId, DEFAULT_DUST_OFFSET_VALUE);
    }

    public final int getCalibratedDustCount(final Integer rawDust) {
        if (this.dustOffset == DEFAULT_DUST_OFFSET_VALUE) {
            return 0;
        }
        return Math.round(rawDust + (DUST_CALIBRATION_BASE - this.dustOffset * DUST_CALIBRATION_K_FACTOR));
    }
}
