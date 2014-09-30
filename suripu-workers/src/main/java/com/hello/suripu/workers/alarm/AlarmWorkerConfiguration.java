package com.hello.suripu.workers.alarm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.configuration.DynamoDBConfiguration;
import com.hello.suripu.core.configuration.KinesisConfiguration;
import com.hello.suripu.core.configuration.QueueName;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.db.DatabaseConfiguration;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pangwu on 9/23/14.
 */
public class AlarmWorkerConfiguration extends Configuration {
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
    @JsonProperty("kinesis")
    private KinesisConfiguration kinesisConfiguration;

    public String getKinesisEndpoint() {
        return kinesisConfiguration.getEndpoint();
    }

    @Valid
    @NotNull
    @JsonProperty("smart_alarm_process_ahead_in_minutes")
    private Integer processAheadTimeInMinutes;

    public Integer getProcessAheadTimeInMinutes() {
        return this.processAheadTimeInMinutes;
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
    @JsonProperty("common_db")
    private DatabaseConfiguration commonDB = new DatabaseConfiguration();

    public DatabaseConfiguration getCommonDB() {
        return commonDB;
    }

    @Valid
    @NotNull
    @JsonProperty("light_sleep_init_threshold")
    private Float lightSleepThreshold;

    public Float getLightSleepThreshold(){
        return this.lightSleepThreshold;
    }


    @Valid
    @NotNull
    @JsonProperty("aggregate_window_size_min")
    private Integer aggregateWindowSizeInMinute;

    public Integer getAggregateWindowSizeInMinute(){
        return this.aggregateWindowSizeInMinute;
    }

    @Valid
    @NotNull
    @JsonProperty("ring_time_db")
    private DynamoDBConfiguration ringTimeDBConfiguration;
    public DynamoDBConfiguration getRingTimeDBConfiguration(){
        return this.ringTimeDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("timezone_history_db")
    private DynamoDBConfiguration timeZoneHistoryDBConfiguration;
    public DynamoDBConfiguration getTimeZoneHistoryDBConfiguration(){
        return this.timeZoneHistoryDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("alarm_db")
    private DynamoDBConfiguration alarmDBConfiguration;
    public DynamoDBConfiguration getAlarmDBConfiguration(){
        return this.alarmDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("alarm_info_db")
    private DynamoDBConfiguration alarmInfoDynamoDBConfiguration;
    public DynamoDBConfiguration getAlarmInfoDynamoDBConfiguration(){
        return this.alarmInfoDynamoDBConfiguration;
    }
}
