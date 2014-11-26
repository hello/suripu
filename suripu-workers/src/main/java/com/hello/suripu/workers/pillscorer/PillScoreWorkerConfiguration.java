package com.hello.suripu.workers.pillscorer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.configuration.DynamoDBConfiguration;
import com.hello.suripu.workers.framework.WorkerConfiguration;
import com.yammer.dropwizard.db.DatabaseConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

public class PillScoreWorkerConfiguration extends WorkerConfiguration {

    @Valid
    @NotNull
    @JsonProperty("common_db")
    private DatabaseConfiguration commonDB = new DatabaseConfiguration();

    public DatabaseConfiguration getCommonDB() {
        return commonDB;
    }

    @Valid
    @NotNull
    @JsonProperty("pill_key_store")
    private DynamoDBConfiguration dynamoDBKeyStoreConfiguration;
    public DynamoDBConfiguration getDynamoDBKeyStoreConfiguration() {
        return dynamoDBKeyStoreConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("timezone_history_db")
    private DynamoDBConfiguration timezoneHistoryConfiguration;
    public DynamoDBConfiguration getTimezoneHistoryConfiguration() {
        return timezoneHistoryConfiguration;
    }

    @Valid
    @NotNull
    @Max(1000)
    @JsonProperty("max_records")
    private Integer maxRecords;

    public Integer getMaxRecords() {
        return maxRecords;
    }

    @Valid
    @NotNull
    @Max(60)
    @JsonProperty("date_minutes_bucket")
    private Integer dateMinuteBucket;

    public Integer getDateMinuteBucket() {
        return dateMinuteBucket;
    }

    @Valid
    @NotNull
    @Max(10000)
    @JsonProperty("checkpoint_threshold")
    private Integer checkpointThreshold;

    public Integer getCheckpointThreshold() {
        return checkpointThreshold;
    }

}
