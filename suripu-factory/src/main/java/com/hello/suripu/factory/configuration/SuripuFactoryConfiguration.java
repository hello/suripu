package com.hello.suripu.factory.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.coredw.configuration.DynamoDBConfiguration;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.db.DatabaseConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class SuripuFactoryConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("database")
    private DatabaseConfiguration database = new DatabaseConfiguration();

    public DatabaseConfiguration getDatabaseConfiguration() {
        return database;
    }


    @Valid
    @NotNull
    @JsonProperty("dynamodb")
    private DynamoDBConfiguration dynamoDBConfiguration;

    public DynamoDBConfiguration getDynamoDBConfiguration(){
        return dynamoDBConfiguration;
    }
}
