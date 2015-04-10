package com.hello.suripu.workers.logs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.configuration.DynamoDBConfiguration;
import com.hello.suripu.core.configuration.SearchifyConfiguration;
import com.hello.suripu.workers.framework.WorkerConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class LogIndexerWorkerConfiguration extends WorkerConfiguration {

    @JsonProperty("max_records")
    private Integer maxRecords = 50;
    public Integer maxRecords() {
        return maxRecords;
    }

    @JsonProperty("application_logs")
    private SearchifyConfiguration applicationLogs;
    public SearchifyConfiguration applicationLogs() {
        return applicationLogs;
    }

    @JsonProperty("sense_logs")
    private SearchifyConfiguration senseLogs;
    public SearchifyConfiguration senseLogs() {
        return senseLogs;
    }

    @JsonProperty("workers_logs")
    private SearchifyConfiguration workersLogs;
    public SearchifyConfiguration workersLogs() {
        return workersLogs;
    }

    @Valid
    @NotNull
    @JsonProperty("sense_events")
    private DynamoDBConfiguration senseEventsDynamoDBConfiguration;
    public DynamoDBConfiguration getSenseEventsDynamoDBConfiguration() {
        return senseEventsDynamoDBConfiguration;
    }
}
