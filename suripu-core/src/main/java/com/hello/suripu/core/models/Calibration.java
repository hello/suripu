package com.hello.suripu.core.models;


import com.fasterxml.jackson.annotation.JsonProperty;

public class Calibration {
    private final static Integer DEFAULT_DUST_OFFSET_VALUE = 0;

    @JsonProperty("dust_offset")
    public final Integer dustOffset;

    public Calibration(final Integer dustOffset) {
        this.dustOffset = dustOffset;
    }

    public static final Calibration createWithDefaultDustOffset() {
        return new Calibration(DEFAULT_DUST_OFFSET_VALUE);
    }
}
