package com.hello.suripu.app.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.configuration.KinesisConfiguration;
import com.hello.suripu.core.configuration.NewDynamoDBConfiguration;
import com.hello.suripu.core.configuration.PushNotificationsConfiguration;
import com.hello.suripu.coredw.configuration.EmailConfiguration;
import com.hello.suripu.coredw.configuration.GraphiteConfiguration;
import com.hello.suripu.coredw.configuration.QuestionConfiguration;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.db.DatabaseConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class SuripuAppConfiguration extends Configuration {

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
    @JsonProperty("metrics_enabled")
    private Boolean metricsEnabled;

    public Boolean getMetricsEnabled() {
        return metricsEnabled;
    }

    @Valid
    @JsonProperty("debug")
    private Boolean debug = Boolean.FALSE;

    public Boolean getDebug() {
        return debug;
    }

    @Valid
    @NotNull
    @JsonProperty("graphite")
    private GraphiteConfiguration graphite;

    public GraphiteConfiguration getGraphite() {
        return graphite;
    }

    @Valid
    @NotNull
    @JsonProperty("allowed_query_range_seconds")
    private Long allowedQueryRange;

    public Long getAllowedQueryRange() {
        return allowedQueryRange;
    }

    @Valid
    @NotNull
    @JsonProperty("kinesis")
    private KinesisConfiguration kinesisConfiguration;
    public KinesisConfiguration getKinesisConfiguration() {
        return kinesisConfiguration;
    }

    @Valid
    @JsonProperty("score_threshold")
    private int scoreThreshold;

    public int getScoreThreshold() {
        return scoreThreshold;
    }


    @Valid
    @JsonProperty("push_notifications")
    private PushNotificationsConfiguration pushNotificationsConfiguration;

    public PushNotificationsConfiguration getPushNotificationsConfiguration() {
        return pushNotificationsConfiguration;
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
    @JsonProperty("sleep_stats_version")
    private String sleepStatsVersion;
    public String getSleepStatsVersion() {
        return this.sleepStatsVersion;
    }

    @Valid
    @NotNull
    @JsonProperty("question_configs")
    private QuestionConfiguration questionConfigs;
    public QuestionConfiguration getQuestionConfigs() {
        return this.questionConfigs;
    }


    @Valid
    @NotNull
    @JsonProperty("email")
    private EmailConfiguration emailConfiguration;
    public EmailConfiguration emailConfiguration() {
        return emailConfiguration;
    }

    @Valid
    @NotNull
    @Min(1)
    @Max(100)
    @JsonProperty("max_cache_refresh_days")
    private int maxCacheRefreshDay;
    public Integer getMaxCacheRefreshDay() {
        return this.maxCacheRefreshDay;
    }

    @Valid
    @NotNull
    @JsonProperty("dynamodb")
    private NewDynamoDBConfiguration dynamoDBConfiguration;

    public NewDynamoDBConfiguration dynamoDBConfiguration(){
        return dynamoDBConfiguration;
    }
}
