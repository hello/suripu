package com.hello.suripu.core.models;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DeviceInactivePage {
    @JsonProperty("previous")
    public final String previousUrl;
    @JsonProperty("next")
    public final String nextUrl;
    @JsonProperty("limit")
    public final Integer limit;
    @JsonProperty("content")
    public final List<DeviceInactive> inactiveSense;

    public DeviceInactivePage(final String previousUrl, final String nextUrl, final Integer limit, final List<DeviceInactive> inactiveSense) {
        this.previousUrl = previousUrl;
        this.nextUrl = nextUrl;
        this.limit = limit;
        this.inactiveSense = inactiveSense;
    }
}
