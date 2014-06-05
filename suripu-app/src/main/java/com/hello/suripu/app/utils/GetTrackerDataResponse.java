package com.hello.suripu.app.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.TrackerMotion;

import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 6/3/14.
 */
public class GetTrackerDataResponse {

    @JsonProperty("data")
    public final Map<String, List<TrackerMotion>> data;

    @JsonCreator
    public GetTrackerDataResponse(@JsonProperty("data") final Map<String, List<TrackerMotion>> data){
        this.data = data;
    }

}
