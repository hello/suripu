package com.hello.suripu.core.models;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.validation.constraints.NotNull;

public class Calibration {
    private final static Integer DEFAULT_DUST_OFFSET = 0;
    private final static Integer DEFAULT_DUST_CALIBRATION_DELTA = 0;
    private final static Float DUST_CALIBRATION_BASE = 300f;
    private final static Float DUST_CALIBRATION_K_FACTOR = 1.3f;

    @NotNull
    @JsonProperty("sense_id")
    public final String senseId;

    @NotNull
    @JsonProperty("dust_offset")
    public final Integer dustOffset;

    @NotNull
    @JsonProperty("dust_calibration_delta")
    public final Integer dustCalibrationDelta;

    @NotNull
    @JsonProperty("tested_at")
    public final Long testedAt;

    private Calibration(final String senseId, final Integer dustOffset, final Integer dustCalibrationDelta, final Long testedAt) {
        this.senseId = senseId;
        this.dustOffset = dustOffset;
        this.dustCalibrationDelta = dustCalibrationDelta;
        this.testedAt = testedAt;
    }

    @JsonCreator
    public static Calibration create(@JsonProperty("sense_id") final String senseId,
                                     @JsonProperty("dust_offset") Integer dustOffset,
                                     @JsonProperty("test_at") Long testedAt) {
        return new Calibration(senseId, dustOffset, Math.round(DUST_CALIBRATION_BASE - (float)dustOffset * DUST_CALIBRATION_K_FACTOR), testedAt);
    }

    public static final Calibration createDefault(final String senseId) {
        return new Calibration(senseId, DEFAULT_DUST_OFFSET, DEFAULT_DUST_CALIBRATION_DELTA, DateTime.now(DateTimeZone.UTC).getMillis());
    }
}
