package com.hello.suripu.core.models.sleep_sounds;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

/**
 * Created by jakepiccolo on 2/18/16.
 */
public class Duration {

    @JsonProperty("id")
    public final Long id;

    @JsonProperty("name")
    public final String name;

    @JsonIgnore
    public final Optional<Long> durationSeconds;

    private Duration(final Long id, final String name, final Optional<Long> durationSeconds) {
        this.id = id;
        this.name = name;
        this.durationSeconds = durationSeconds;
    }

    public static Duration create(final Long id, final String name, final Long durationSeconds) {
        return new Duration(id, name, Optional.fromNullable(durationSeconds));
    }
}
