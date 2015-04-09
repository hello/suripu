package com.hello.suripu.core.metrics;

import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class DeviceEvents {
    public final String deviceId;
    public final DateTime createdAt;
    public final Map<String, String> events;

    public DeviceEvents(final String deviceId, final DateTime createdAt, final Map<String, String> events) {
        checkNotNull(deviceId, "device_id can not be null");
        checkNotNull(events, "events can not be null");
        this.deviceId = deviceId;
        this.createdAt = createdAt;
        this.events = ImmutableMap.copyOf(events);
    }
}
