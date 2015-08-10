package com.hello.suripu.core.models;


import com.fasterxml.jackson.annotation.JsonProperty;

public class Calibration {
    private final static Integer DEFAULT_DUST_OFFSET = 0;
    private final static Integer DEFAULT_DUST_CALIBRATION_DELTA = 0;
    private final static Float DUST_CALIBRATION_BASE = 300f;
    private final static Float DUST_CALIBRATION_K_FACTOR = 1.5f;
    private final static String DEFAULT_METADATA = "";

    @JsonProperty("sense_id")
    public final String senseId;

    @JsonProperty("dust_offset")
    public final Integer dustOffset;

    @JsonProperty("dust_calibration_delta")
    public final Integer dustCalibrationDelta;

    @JsonProperty("metadata")
    public final String metadata;

    private Calibration(final String senseId, final Integer dustOffset, final Integer dustCalibrationDelta, final String metadata) {
        this.senseId = senseId;
        this.dustOffset = dustOffset;
        this.dustCalibrationDelta = dustCalibrationDelta;
        this.metadata = metadata;
    }

    public static Calibration create(final String senseId, final Integer dustOffset, final String metadata) {
        return new Calibration(senseId, dustOffset, Math.round(DUST_CALIBRATION_BASE - dustOffset * DUST_CALIBRATION_K_FACTOR), metadata);
    }

    public static final Calibration createDefault(final String senseId) {
        return new Calibration(senseId, DEFAULT_DUST_OFFSET, DEFAULT_DUST_CALIBRATION_DELTA, DEFAULT_METADATA);
    }

    public static final Calibration createDefaultWithArbitrarySenseId() {
        return new Calibration("dummy-sense", DEFAULT_DUST_OFFSET, DEFAULT_DUST_CALIBRATION_DELTA, DEFAULT_METADATA);
    }
}
