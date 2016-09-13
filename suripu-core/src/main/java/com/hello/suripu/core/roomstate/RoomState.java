package com.hello.suripu.core.roomstate;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface RoomState {
    @JsonProperty("temperature")
    State temperature();

    @JsonProperty("humidity")
    State humidity();

    @JsonProperty("light")
    State light();

    @JsonProperty("sound")
    State sound();

    @JsonProperty("particulates")
    State particulates();
}
