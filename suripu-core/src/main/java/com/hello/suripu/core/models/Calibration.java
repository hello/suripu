package com.hello.suripu.core.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

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

    @JsonProperty("tested_at")
    public final Long testedAt;

    private Calibration(final String senseId, final Integer dustOffset, final Integer dustCalibrationDelta, final String metadata, final Long testedAt) {
        this.senseId = senseId;
        this.dustOffset = dustOffset;
        this.dustCalibrationDelta = dustCalibrationDelta;
        this.metadata = metadata;
        this.testedAt = testedAt;
    }

    public static Calibration create(final String senseId, final Integer dustOffset, final String metadata, final Long testedAt) {
        return new Calibration(senseId, dustOffset, Math.round(DUST_CALIBRATION_BASE - (float)dustOffset * DUST_CALIBRATION_K_FACTOR), metadata, testedAt);
    }

    public static final Calibration createDefault(final String senseId) {
        return new Calibration(senseId, DEFAULT_DUST_OFFSET, DEFAULT_DUST_CALIBRATION_DELTA, DEFAULT_METADATA, DateTime.now(DateTimeZone.UTC).getMillis());
    }
}
