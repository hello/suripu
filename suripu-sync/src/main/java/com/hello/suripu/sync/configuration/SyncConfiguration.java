package com.hello.suripu.sync.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.configuration.DynamoDBConfiguration;
import com.yammer.dropwizard.config.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class SyncConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("dynamodb")
    private DynamoDBConfiguration dynamoDBConfiguration = new DynamoDBConfiguration();

    public DynamoDBConfiguration getDynamoDBConfiguration() {
        return dynamoDBConfiguration;
    }
}
