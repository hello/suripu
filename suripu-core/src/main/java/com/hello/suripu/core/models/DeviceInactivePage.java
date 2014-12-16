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
    public final List<DeviceInactive> inactiveDevices;

    public DeviceInactivePage(final String previousUrl, final String nextUrl, final Integer limit, final List<DeviceInactive> inactiveDevices) {
        this.previousUrl = previousUrl;
        this.nextUrl = nextUrl;
        this.limit = limit;
        this.inactiveDevices = inactiveDevices;
    }
    public static DeviceInactivePage getInactivePageByRawInput(List<DeviceInactive> inactiveDevices, Long afterTimestamp, Long beforeTimestamp, Integer maxItemsPerPage){
        String previousUrl = String.format("?before=%d", afterTimestamp);
        String nextUrl = String.format("?after=%d", beforeTimestamp);
        if(!inactiveDevices.isEmpty())  {
            final Long minTimestamp = inactiveDevices.get(0).lastSeenTimestamp - 1;
            final Long maxTimestamp = inactiveDevices.get(inactiveDevices.size() - 1).lastSeenTimestamp + 1;
            nextUrl = String.format("?after=%d", maxTimestamp);
            previousUrl = String.format("?before=%d", minTimestamp);
        }
        return new DeviceInactivePage(previousUrl, nextUrl, maxItemsPerPage, inactiveDevices);
    }
}
