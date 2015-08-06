package com.hello.suripu.core.models;


import com.fasterxml.jackson.annotation.JsonProperty;

public class Calibration {
    private final static Integer DEFAULT_DUST_OFFSET_VALUE = 0;

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
}
