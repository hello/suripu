package com.hello.suripu.core.models;


import com.fasterxml.jackson.annotation.JsonProperty;

public class Calibration {
    private final static Integer DEFAULT_DUST_CALIBRATION_DELTA = 0;
    private final static Float DUST_CALIBRATION_BASE = 300f;
    private final static Float DUST_CALIBRATION_K_FACTOR = 1.5f;

    @JsonProperty("sense_id")
    public final String senseId;

    @JsonProperty("dust_calibration_delta")
    public final Integer dustCalibrationDelta;

    public Calibration(final String senseId, final Integer dustOffset) {
        this.senseId = senseId;
        this.dustCalibrationDelta = Math.round(DUST_CALIBRATION_BASE - dustOffset * DUST_CALIBRATION_K_FACTOR);
    }

    public static final Calibration createDefault(final String senseId) {
        return new Calibration(senseId, DEFAULT_DUST_CALIBRATION_DELTA);
    }
}
