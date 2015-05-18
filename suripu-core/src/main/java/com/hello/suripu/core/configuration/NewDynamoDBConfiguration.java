package com.hello.suripu.core.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class NewDynamoDBConfiguration {
    @Valid
    @NotNull
    @JsonProperty
    private String region = "us-east-1";

    public String getRegion() {
        return region;
    }


    @Valid
    @NotNull
    @JsonProperty("tables")
    private Map<DynamoDBTableName, String> tables = new HashMap<>();

    public ImmutableMap<DynamoDBTableName, String> tables() {
        return ImmutableMap.copyOf(tables);
    }

    @Valid
    @NotNull
    @JsonProperty("endpoints")
    private Map<DynamoDBTableName, String> endpoints = new HashMap<>();

    public ImmutableMap<DynamoDBTableName, String> endpoints() {
        return ImmutableMap.copyOf(endpoints);
    }
}
