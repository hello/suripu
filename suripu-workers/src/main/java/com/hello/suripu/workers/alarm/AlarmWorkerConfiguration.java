package com.hello.suripu.workers.alarm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.configuration.DynamoDBConfiguration;
import com.hello.suripu.workers.framework.WorkerConfiguration;
import com.yammer.dropwizard.db.DatabaseConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

/**
 * Created by pangwu on 9/23/14.
 */
public class AlarmWorkerConfiguration extends WorkerConfiguration {


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
    @JsonProperty("sensors_db")
    private DatabaseConfiguration sensorsDB = new DatabaseConfiguration();

    public DatabaseConfiguration getSensorsDB() {
        return sensorsDB;
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

    @Valid
    @NotNull
    @JsonProperty("smart_alarm_log_db")
    private DynamoDBConfiguration smartAlarmLogDBConfiguration;
    public DynamoDBConfiguration getSmartAlarmLogDBConfiguration(){
        return this.smartAlarmLogDBConfiguration;
    }


    @Valid
    @NotNull
    @JsonProperty("features_db")
    private DynamoDBConfiguration featuresDynamoDBConfiguration;
    public DynamoDBConfiguration getFeaturesDynamoDBConfiguration(){
        return this.featuresDynamoDBConfiguration;
    }
}
