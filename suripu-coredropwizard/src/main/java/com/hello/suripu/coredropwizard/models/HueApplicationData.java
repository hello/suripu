package com.hello.suripu.coredropwizard.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HueApplicationData {

    @JsonProperty("bridge_id")
    public final String bridgeId;

    @JsonProperty("whitelist_id")
    public final String whitelistId;

    @JsonProperty("group_id")
    public final Integer groupId;

    public HueApplicationData(
        @JsonProperty("bridge_id") final String bridgeId,
        @JsonProperty("whitelist_id") final String whitelistId,
        @JsonProperty("group_id") final Integer groupId
    ) {
        this.bridgeId = bridgeId;
        this.whitelistId = whitelistId;
        this.groupId = groupId;
    }
}
