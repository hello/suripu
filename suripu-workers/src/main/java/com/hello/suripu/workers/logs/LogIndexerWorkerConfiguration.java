package com.hello.suripu.workers.logs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.configuration.DynamoDBConfiguration;
import com.hello.suripu.core.configuration.RedisConfiguration;
import com.hello.suripu.core.configuration.SearchifyConfiguration;
import com.hello.suripu.workers.framework.WorkerConfiguration;
import com.yammer.dropwizard.db.DatabaseConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class LogIndexerWorkerConfiguration extends WorkerConfiguration {

    @JsonProperty("max_records")
    private Integer maxRecords = 50;
    public Integer maxRecords() {
        return maxRecords;
    }

    @JsonProperty("sense_logs")
    private SearchifyConfiguration senseLogs;
    public SearchifyConfiguration senseLogs() {
        return senseLogs;
    }

    @Valid
    @NotNull
    @JsonProperty("sense_events")
    private DynamoDBConfiguration senseEventsDynamoDBConfiguration;
    public DynamoDBConfiguration getSenseEventsDynamoDBConfiguration() {
        return senseEventsDynamoDBConfiguration;
    }


    @Valid
    @NotNull
    @JsonProperty("common_db")
    private DatabaseConfiguration commonDBConfiguration;
    public DatabaseConfiguration getCommonDBConfiguration() {
        return this.commonDBConfiguration;
    }


    @Valid
    @NotNull
    @JsonProperty("features_db")
    private DynamoDBConfiguration featuresDynamoDBConfiguration;
    public DynamoDBConfiguration getFeaturesDynamoDBConfiguration(){
        return this.featuresDynamoDBConfiguration;
    }

    @JsonProperty("redis")
    private RedisConfiguration redisConfiguration;
    public RedisConfiguration redisConfiguration() {
        return redisConfiguration;
    }
}
