package com.hello.suripu.core.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import org.joda.time.DateTime;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class DeviceEvents {

    @JsonProperty("device_id")
    public final String deviceId;

    @JsonProperty("created_at")
    public final DateTime createdAt;

    @JsonProperty("events")
    public final Set<String> events;

    public DeviceEvents(final String deviceId, final DateTime createdAt, final Set<String> events) {
        checkNotNull(deviceId, "device_id can not be null");
        checkNotNull(events, "events can not be null");
        this.deviceId = deviceId;
        this.createdAt = createdAt;
        this.events = ImmutableSet.copyOf(events);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(DeviceEvents.class)
                .add("device_id", deviceId)
                .add("created_at", createdAt)
                .add("events", events)
                .toString();
    }
}
