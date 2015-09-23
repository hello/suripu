package com.hello.suripu.core.models.device.v2;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Devices {

    @JsonProperty("senses")
    public final List<Sense> senses;

    @JsonProperty("pills")
    public final List<Pill> pills;

    public Devices(final List<Sense> senses, final List<Pill> pills) {
        this.senses = senses;
        this.pills = pills;
    }
}
