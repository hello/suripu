package com.hello.suripu.workers.insights;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.configuration.DynamoDBConfiguration;
import com.hello.suripu.workers.framework.WorkerConfiguration;
import com.yammer.dropwizard.db.DatabaseConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

/**
 * Created by kingshy on 1/6/15.
 */
public class InsightsGeneratorWorkerConfiguration extends WorkerConfiguration {

    @Valid
    @NotNull
    @JsonProperty("common_db")
    private DatabaseConfiguration commonDB = new DatabaseConfiguration();
    public DatabaseConfiguration getCommonDB() { return commonDB; }

    @Valid
    @NotNull
    @JsonProperty("sensors_db")
    private DatabaseConfiguration sensorsDB = new DatabaseConfiguration();
    public DatabaseConfiguration getSensorsDB() {
        return sensorsDB;
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
    @Max(1000)
    @JsonProperty("max_records")
    private Integer maxRecords;
    public Integer getMaxRecords() {
        return maxRecords;
    }

    @Valid
    @NotNull
    @JsonProperty("insights_dynamo_db")
    private DynamoDBConfiguration insightsDynamoDB;
    public DynamoDBConfiguration getInsightsDynamoDB() {
        return insightsDynamoDB;
    }

    @Valid
    @NotNull
    @JsonProperty("sleep_score_db")
    private DynamoDBConfiguration sleepScoreDynamoDB;
    public DynamoDBConfiguration getSleepScoreDynamoDB() { return sleepScoreDynamoDB; }

    @Valid
    @NotNull
    @JsonProperty("sleep_score_version")
    private String sleepScoreVersion;
    public String getSleepScoreVersion() { return sleepScoreVersion; }

    @Valid
    @NotNull
    @JsonProperty("preferences_db")
    private DynamoDBConfiguration preferencesDynamoDB;

    public DynamoDBConfiguration getPreferencesDynamoDB() {
        return preferencesDynamoDB;
    }

    @Valid
    @NotNull
    @JsonProperty("sleep_stats_db")
    private DynamoDBConfiguration sleepStatsDynamoDBConfiguration;
    public DynamoDBConfiguration getSleepStatsDynamoConfiguration(){
        return this.sleepStatsDynamoDBConfiguration;
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
    @JsonProperty("features_db")
    private DynamoDBConfiguration featuresDynamoDBConfiguration;
    public DynamoDBConfiguration getFeaturesDynamoDBConfiguration(){
        return this.featuresDynamoDBConfiguration;
    }

}
