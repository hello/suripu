package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Created by jnorgan on 5/1/15.
 */
public class UpgradeNodeRequest {

    public final String groupName;
    public final Integer fromFWVersion;
    public final Integer toFWVersion;
    public final Float startRolloutPercent;
    public final Float endRolloutPercent;

    @JsonCreator
    public UpgradeNodeRequest(
            @JsonProperty("group_name") final String groupName,
            @JsonProperty("from_fw_version") final Integer fromFWVersion,
            @JsonProperty("to_fw_version") final Integer toFWVersion,
            @JsonProperty("start_rollout_percent") final Float startRolloutPercent,
            @JsonProperty("end_rollout_percent") final Float endRolloutPercent) {

        this.groupName = groupName;
        this.fromFWVersion = fromFWVersion;
        this.toFWVersion = toFWVersion;
        this.startRolloutPercent = startRolloutPercent;
        this.endRolloutPercent = endRolloutPercent;
    }
}
