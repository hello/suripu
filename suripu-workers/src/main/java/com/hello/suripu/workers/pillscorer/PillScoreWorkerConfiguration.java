package com.hello.suripu.workers.pillscorer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.configuration.QueueName;
import com.yammer.dropwizard.db.DatabaseConfiguration;
import com.yammer.dropwizard.config.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class PillScoreWorkerConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("common_db")
    private DatabaseConfiguration commonDB = new DatabaseConfiguration();

    public DatabaseConfiguration getCommonDB() {
        return commonDB;
    }

    @Valid
    @NotNull
    @JsonProperty("app_name")
    private String appName;

    public String getAppName() {
        return appName;
    }

    @Valid
    @NotEmpty
    @JsonProperty("queues")
    private Map<QueueName,String> queues = new HashMap<QueueName, String>();

    public ImmutableMap<QueueName,String> getQueues() {
        return ImmutableMap.copyOf(queues);
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
    @JsonProperty("process_threshold")
    private Integer processThreshold;

    public Integer getProcessThreshold() {
        return processThreshold;
    }

    @Valid
    @NotNull
    @Max(100)
    @JsonProperty("checkpoint_threshold")
    private Integer checkpointThreshold;

    public Integer getCheckpointThreshold() {
        return checkpointThreshold;
    }

    @Valid
    @JsonProperty("debug")
    private Boolean debug = Boolean.FALSE;

}
