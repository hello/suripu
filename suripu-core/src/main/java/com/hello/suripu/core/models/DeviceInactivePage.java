package com.hello.suripu.core.models;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DeviceInactivePage {
    @JsonProperty("previous")
    public final Long previousCursorTimestamp;
    @JsonProperty("next")
    public final Long nextCursorTimestamp;
    @JsonProperty("limit")
    public final Integer limit;
    @JsonProperty("content")
    public final List<DeviceInactive> inactiveDevices;

    public DeviceInactivePage(final Long previousCursorTimestamp, final Long nextCursorTimestamp, final Integer limit, final List<DeviceInactive> inactiveDevices) {
        this.previousCursorTimestamp = previousCursorTimestamp;
        this.nextCursorTimestamp = nextCursorTimestamp;
        this.limit = limit;
        this.inactiveDevices = inactiveDevices;
    }
}
