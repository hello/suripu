package com.hello.suripu.core.models;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class Calibration {
    private final static Integer DEFAULT_DUST_OFFSET = 0;
    private final static Integer DEFAULT_DUST_CALIBRATION_DELTA = 0;
    private final static Float DUST_CALIBRATION_BASE = 300f;
    private final static Float DUST_CALIBRATION_K_FACTOR = 1.3f;

    private final static Integer DEFAULT_LIGHTS_OUT_DELTA = 100;

    @JsonProperty("sense_id")
    public final String senseId;

    @JsonProperty("dust_offset")
    public final Integer dustOffset;

    @JsonProperty("dust_calibration_delta")
    public final Integer dustCalibrationDelta;

    private final Integer lightsOutDelta;

    @JsonIgnore
    public final Integer lightsOutDelta() {
        return lightsOutDelta;
    }

    @JsonProperty("tested_at")
    public final Long testedAt;

    private Calibration(final String senseId, final Integer dustOffset, final Integer dustCalibrationDelta, final Optional<Integer> lightsOutDelta, final Long testedAt) {
        this.senseId = senseId;
        this.dustOffset = dustOffset;
        this.dustCalibrationDelta = dustCalibrationDelta;
        this.lightsOutDelta = lightsOutDelta.or(DEFAULT_LIGHTS_OUT_DELTA);
        this.testedAt = testedAt;
    }

    @JsonCreator
    public static Calibration create(@JsonProperty("sense_id") final String senseId,
                                     @JsonProperty("dust_offset") Integer dustOffset,
                                     @JsonProperty("test_at") Long testedAt) {
        return new Calibration(senseId, dustOffset, Math.round(DUST_CALIBRATION_BASE - (float)dustOffset * DUST_CALIBRATION_K_FACTOR), Optional.absent(), testedAt);
    }

    public static Calibration createWithLightsOutDelta(final String senseId,
                                                       final Integer dustOffset,
                                                       final Integer lightsOutDelta,
                                                       final Long testedAt) {
        return new Calibration(senseId, dustOffset, Math.round(DUST_CALIBRATION_BASE - (float)dustOffset * DUST_CALIBRATION_K_FACTOR), Optional.fromNullable(lightsOutDelta), testedAt);
    }

    public static final Calibration createDefault(final String senseId) {
        return new Calibration(senseId, DEFAULT_DUST_OFFSET, DEFAULT_DUST_CALIBRATION_DELTA, Optional.absent(), DateTime.now(DateTimeZone.UTC).getMillis());
    }
}
