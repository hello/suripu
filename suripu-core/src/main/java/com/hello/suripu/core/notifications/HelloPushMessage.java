package com.hello.suripu.core.notifications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.hello.suripu.core.models.Sensor;

public class HelloPushMessage {
    public final String body;
    public final String target;
    public final String details;

    @JsonCreator
    public HelloPushMessage(
            @JsonProperty("body") final String body,
            @JsonProperty("target") final String target,
            @JsonProperty("details") final String details) {
        this.body = body;
        this.target = target;
        this.details = details;
    }

    public static HelloPushMessage fromSensors(final String body, final Sensor sensor) {
        return new HelloPushMessage(body, "sensors", sensor.name());
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(HelloPushMessage.class)
                .add("body", body)
                .add("target", target)
                .add("details", details)
                .toString();
    }
}
