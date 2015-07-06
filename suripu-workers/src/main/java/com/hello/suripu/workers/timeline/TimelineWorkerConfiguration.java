package com.hello.suripu.workers.timeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.configuration.DynamoDBConfiguration;
import com.hello.suripu.core.configuration.NewDynamoDBConfiguration;
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
    private static int DEFAULT_MAX_READ_CAPACITY = 200;

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
    @JsonProperty("score_threshold")
    private int scoreThreshold;

    public int getScoreThreshold() {
        return scoreThreshold;
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
    @JsonProperty("start_process_time")
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


    @Valid
    @NotNull
    @Min(5)
    @Max(24)
    @JsonProperty("start_expire_time")
    private Integer earliestExpireTime;
    public Integer getEarliestExpireTime(){
        return this.earliestExpireTime;
    }

    @Valid
    @NotNull
    @Min(10)
    @Max(24)
    @JsonProperty("end_expire_time")
    private Integer lastExpireTime;
    public Integer getLastExpireTime(){
        return this.lastExpireTime;
    }

    @Valid
    @NotNull
    @Min(1)
    @Max(100)
    @JsonProperty("max_cache_refresh_days")
    private int maxCacheRefreshDay;
    public Integer getMaxCacheRefreshDay(){
        return this.maxCacheRefreshDay;
    }

    @Valid
    @NotNull
    @JsonProperty("sleep_stats_version")
    private String sleepStatsVersion;
    public String getSleepStatsVersion() {
        return this.sleepStatsVersion;
    }

    @Valid
    @NotNull
    @JsonProperty("dynamodb")
    private NewDynamoDBConfiguration dynamoDBConfiguration;

    public NewDynamoDBConfiguration getDynamoDBConfiguration(){
        return dynamoDBConfiguration;
    }

    @Min(50)
    @Max(200)
    @JsonProperty("user_info_max_read_capacity_sec")
    private Integer mergeUserInfoDynamoDBReadCapacityPerSecondUpperBound = DEFAULT_MAX_READ_CAPACITY;
    public Integer getMergeUserInfoDynamoDBReadCapacityPerSecondUpperBound(){
        return this.mergeUserInfoDynamoDBReadCapacityPerSecondUpperBound;
    }

    @Valid
    @NotNull
    @JsonProperty("hmm_bayesnet_priors")
    private DynamoDBConfiguration hmmBayesnetPriorsConfiguration;
    public DynamoDBConfiguration getHmmBayesnetPriorsConfiguration() {
        return this.hmmBayesnetPriorsConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("hmm_bayesnet_paths")
    private DynamoDBConfiguration hmmBayesnetPathsConfiguration;
    public DynamoDBConfiguration getHmmBayesnetPathsConfiguration() {
        return this.hmmBayesnetPathsConfiguration;
    }
}
