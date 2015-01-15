package com.hello.suripu.app.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.configuration.DynamoDBConfiguration;
import com.hello.suripu.core.configuration.GraphiteConfiguration;
import com.hello.suripu.core.configuration.KinesisConfiguration;
import com.hello.suripu.core.configuration.KinesisLoggerConfiguration;
import com.hello.suripu.core.configuration.PushNotificationsConfiguration;
import com.hello.suripu.core.configuration.QuestionConfiguration;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.db.DatabaseConfiguration;

import javax.validation.Valid;
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

    /*
    @Valid
    @NotNull
    @JsonProperty("motion_db")
    private DynamoDBConfiguration motionDBConfiguration;

    public DynamoDBConfiguration getMotionDBConfiguration() {
        return this.motionDBConfiguration;
    }
    */

    @Valid
    @NotNull
    @JsonProperty("event_db")
    private DynamoDBConfiguration eventDBConfiguration;
    public DynamoDBConfiguration getEventDBConfiguration(){
        return this.eventDBConfiguration;
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
    @JsonProperty("allowed_query_range_seconds")
    private Long allowedQueryRange;

    public Long getAllowedQueryRange() {
        return allowedQueryRange;
    }

    @Valid
    @NotNull
    @JsonProperty("kinesis_logger")
    private KinesisLoggerConfiguration kinesisLoggerConfiguration;

    public KinesisLoggerConfiguration getKinesisLoggerConfiguration() {
        return kinesisLoggerConfiguration;
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
    @JsonProperty("ring_time_db")
    private DynamoDBConfiguration ringTimeDBConfiguration;
    public DynamoDBConfiguration getRingTimeDBConfiguration(){
        return this.ringTimeDBConfiguration;
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
    @JsonProperty("question_configs")
    private QuestionConfiguration questionConfigs;
    public QuestionConfiguration getQuestionConfigs() {
        return this.questionConfigs;
    }

    @Valid
    @NotNull
    @JsonProperty("features_db")
    private DynamoDBConfiguration featuresDynamoDBConfiguration;
    public DynamoDBConfiguration getFeaturesDynamoDBConfiguration(){
        return this.featuresDynamoDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("teams_db")
    private DynamoDBConfiguration teamsDynamoDBConfiguration;
    public DynamoDBConfiguration getTeamsDynamoDBConfiguration(){
        return this.teamsDynamoDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("insights_dynamo_db")
    private DynamoDBConfiguration insightsDynamoDBConfiguration;
    public DynamoDBConfiguration getInsightsDynamoDBConfiguration(){
        return this.insightsDynamoDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("notifications_db")
    private DynamoDBConfiguration notificationsDBConfiguration;

    public DynamoDBConfiguration getNotificationsDBConfiguration() {
        return notificationsDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("preferences_db")
    private DynamoDBConfiguration preferencesDBConfiguration;
    public DynamoDBConfiguration getPreferencesDBConfiguration() {
        return preferencesDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("key_store_dynamo_db")
    private DynamoDBConfiguration dynamoDBConfiguration;
    public DynamoDBConfiguration getKeyStoreDynamoDBConfiguration() { return dynamoDBConfiguration; }

}
