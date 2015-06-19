package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;


/**
 * Created by jnorgan on 5/1/15.
 */
public class UpgradeNodeRequest {

    @NotNull
    public final String groupName;

    @NotNull
    public final Integer fromFWVersion;

    @NotNull
    public final Integer toFWVersion;

    public final Integer rolloutPercent;

    @JsonCreator
    public UpgradeNodeRequest(
            @JsonProperty("group_name") final String groupName,
            @JsonProperty("from_fw_version") final Integer fromFWVersion,
            @JsonProperty("to_fw_version") final Integer toFWVersion,
            @JsonProperty("rollout_percent") final Integer rolloutPercent) {

        this.groupName = groupName;
        this.fromFWVersion = fromFWVersion;
        this.toFWVersion = toFWVersion;
        this.rolloutPercent = rolloutPercent;
    }
}
