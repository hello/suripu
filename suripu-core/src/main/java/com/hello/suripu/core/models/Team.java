package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Set;

public class Team {

    @JsonProperty("name")
    public final String name;

    @JsonProperty("ids")
    public final Set<String> ids;

    @JsonCreator
    public Team(
            @JsonProperty("name") final String name,
            @JsonProperty("ids") final List<String> ids) {
        this.name = name;
        this.ids = ImmutableSet.copyOf(ids);
    }

    public Team(
            final String name,
            final Set<String> ids) {
        this.name = name;
        this.ids = ImmutableSet.copyOf(ids);
    }
}
