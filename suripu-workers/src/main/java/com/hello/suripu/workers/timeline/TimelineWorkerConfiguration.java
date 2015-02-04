package com.hello.suripu.workers.timeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.configuration.DynamoDBConfiguration;
import com.hello.suripu.workers.framework.WorkerConfiguration;
import com.yammer.dropwizard.db.DatabaseConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Created by pangwu on 1/26/15.
 */
public class TimelineWorkerConfiguration extends WorkerConfiguration {
    @Valid
    @NotNull
    @Max(20)
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
    @JsonProperty("common_db")
    private DatabaseConfiguration commonDB = new DatabaseConfiguration();

    public DatabaseConfiguration getCommonDB() {
        return commonDB;
    }

    @Valid
    @NotNull
    @JsonProperty("insights_db")
    private DatabaseConfiguration insightsDB = new DatabaseConfiguration();

    public DatabaseConfiguration getInsightsDB() {
        return insightsDB;
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
    @JsonProperty("user_info_db")
    private DynamoDBConfiguration userInfoDynamoDBConfiguration;
    public DynamoDBConfiguration getUserInfoDynamoDBConfiguration(){
        return this.userInfoDynamoDBConfiguration;
    }

    @Valid
    @JsonProperty("score_threshold")
    private int scoreThreshold;

    public int getScoreThreshold() {
        return scoreThreshold;
    }

    @Valid
    @NotNull
    @JsonProperty("sleep_score_db")
    private DynamoDBConfiguration sleepScoreDBConfiguration;
    public DynamoDBConfiguration getSleepScoreDBConfiguration(){
        return this.sleepScoreDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("sleep_score_version")
    private String sleepScoreVersion;
    public String getSleepScoreVersion() {
        return this.sleepScoreVersion;
    }

    @Valid
    @NotNull
    @JsonProperty("timeline_db")
    private DynamoDBConfiguration timelineDBConfiguration;
    public DynamoDBConfiguration getTimelineDBConfiguration(){
        return this.timelineDBConfiguration;
    }

    @Valid
    @NotNull
    @Min(60)
    @JsonProperty("max_no_motion_period_minutes")
    private Integer maxNoMoitonPeriodInMinutes;
    public Integer getMaxNoMoitonPeriodInMinutes(){
        return this.maxNoMoitonPeriodInMinutes;
    }

    @Valid
    @NotNull
    @Min(5)
    @Max(24)
    @JsonProperty("hour_of_day_trigger")
    private Integer earliestProcessTime;
    public Integer getEarliestProcessTime(){
        return this.earliestProcessTime;
    }

    @Valid
    @NotNull
    @Min(10)
    @Max(24)
    @JsonProperty("end_process_time")
    private Integer lastProcessTime;
    public Integer getLastProcessTime(){
        return this.lastProcessTime;
    }
}
