package com.hello.suripu.app.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.configuration.KinesisConfiguration;
import com.hello.suripu.core.configuration.PushNotificationsConfiguration;
import com.hello.suripu.coredw.configuration.DynamoDBConfiguration;
import com.hello.suripu.coredw.configuration.EmailConfiguration;
import com.hello.suripu.coredw.configuration.GraphiteConfiguration;
import com.hello.suripu.coredw.configuration.QuestionConfiguration;
import com.hello.suripu.coredw.configuration.S3BucketConfiguration;
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
    public DynamoDBConfiguration getScheduledRingTimeHistoryDBConfiguration(){
        return this.ringTimeDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("ring_history_db")
    private DynamoDBConfiguration ringTimeHistoryDBConfiguration;
    public DynamoDBConfiguration getRingTimeHistoryDBConfiguration(){
        return this.ringTimeHistoryDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("alarm_info_db")
    private DynamoDBConfiguration userInfoDynamoDBConfiguration;
    public DynamoDBConfiguration getUserInfoDynamoDBConfiguration(){
        return this.userInfoDynamoDBConfiguration;
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
    @JsonProperty("algorithm_test_db")
    private DynamoDBConfiguration algorithmTestDBConfiguration;
    public DynamoDBConfiguration getAlgorithmTestDBConfiguration(){
        return this.algorithmTestDBConfiguration;
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
    @JsonProperty("sense_state_db")
    private DynamoDBConfiguration senseStateDBConfiguration;
    public DynamoDBConfiguration getSenseStateDBConfiguration() { return this.senseStateDBConfiguration; }

    @Valid
    @NotNull
    @JsonProperty("file_manifest")
    private DynamoDBConfiguration fileManifestDBConfiguration;
    public DynamoDBConfiguration getFileManifestDBConfiguration() { return this.fileManifestDBConfiguration; }

    @Valid
    @NotNull
    @JsonProperty("sleep_score_version")
    private String sleepScoreVersion;
    public String getSleepScoreVersion() {
        return this.sleepScoreVersion;
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
    @JsonProperty("sense_key_store_dynamo_db")
    private DynamoDBConfiguration senseKeyStoreDynamoConfiguration;
    public DynamoDBConfiguration getSenseKeyStoreDynamoDBConfiguration() { return senseKeyStoreDynamoConfiguration; }

    @Valid
    @NotNull
    @JsonProperty("pill_key_store_dynamo_db")
    private DynamoDBConfiguration pillKeyStoreDynamoDBConfiguration;
    public DynamoDBConfiguration getPillKeyStoreDynamoDBConfiguration() { return pillKeyStoreDynamoDBConfiguration; }

    @Valid
    @NotNull
    @JsonProperty("timeline_db")
    private DynamoDBConfiguration timelineDBConfiguration;
    public DynamoDBConfiguration getTimelineDBConfiguration(){
        return this.timelineDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("password_reset_db")
    private DynamoDBConfiguration passwordResetDBConfiguration;
    public DynamoDBConfiguration getPasswordResetDBConfiguration(){
        return this.passwordResetDBConfiguration;
   
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
    @JsonProperty("sleephmm_db")
    private DynamoDBConfiguration sleepHmmDBConfiguration;
    public DynamoDBConfiguration getSleepHmmDBConfiguration(){
        return this.sleepHmmDBConfiguration;
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
    @JsonProperty("timeline_log_db")
    private DynamoDBConfiguration timelineLogDBConfiguration;
    public DynamoDBConfiguration getTimelineLogDBConfiguration(){
        return this.timelineLogDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("ota_history_db")
    private DynamoDBConfiguration otaHistoryDBConfiguration;
    public DynamoDBConfiguration getOTAHistoryDBConfiguration(){
        return this.otaHistoryDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("resp_commands_db")
    private DynamoDBConfiguration responseCommandsDBConfiguration;
    public DynamoDBConfiguration getResponseCommandsDBConfiguration(){
        return this.responseCommandsDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("fw_path_db")
    private DynamoDBConfiguration fwUpgradePathDBConfiguration;
    public DynamoDBConfiguration getFWUpgradePathDBConfiguration(){
        return this.fwUpgradePathDBConfiguration;
    }


    @Valid
    @NotNull
    @JsonProperty("sense_last_seen")
    private DynamoDBConfiguration senseLastSeenConfiguration;
    public DynamoDBConfiguration getSenseLastSeenConfiguration(){
        return this.senseLastSeenConfiguration;
    }


    @Valid
    @NotNull
    @JsonProperty("feature_extraction_models")
    private DynamoDBConfiguration hmmFeatureExtractionModelsConfiguration;
    public DynamoDBConfiguration getFeatureExtractionModelsConfiguration() {
        return this.hmmFeatureExtractionModelsConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("online_hmm_models")
    private DynamoDBConfiguration onlineHmmModelsConfiguration;
    public DynamoDBConfiguration getOnlineHmmModelsConfiguration() {
        return this.onlineHmmModelsConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("calibration")
    private DynamoDBConfiguration calibrationConfiguration;
    public DynamoDBConfiguration getCalibrationConfiguration() {return this.calibrationConfiguration;}

    @Valid
    @NotNull
    @JsonProperty("wifi_info")
    private DynamoDBConfiguration wifiInfoConfiguration;
    public DynamoDBConfiguration getWifiInfoConfiguration() {return this.wifiInfoConfiguration;}

    @Valid
    @NotNull
    @JsonProperty("app_stats")
    private DynamoDBConfiguration appStatsConfiguration;
    public DynamoDBConfiguration getAppStatsConfiguration() {return this.appStatsConfiguration;}

    @Valid
    @NotNull
    @JsonProperty("pill_heartbeat")
    private DynamoDBConfiguration pillHeartBeatConfiguration;
    public DynamoDBConfiguration getPillHeartBeatConfiguration() {return this.pillHeartBeatConfiguration;}

    @Valid
    @NotNull
    @JsonProperty("device_data")
    private DynamoDBConfiguration deviceDataConfiguration;
    public DynamoDBConfiguration getDeviceDataConfiguration() { return this.deviceDataConfiguration; }

    @Valid
    @JsonProperty("next_flush_sleep")
    private Long nextFlushSleepMillis = 50L;
    public Long getNextFlushSleepMillis() { return nextFlushSleepMillis; }

    @Valid
    @JsonProperty("stop_month")
    private int stopMonth = 2; // set default to feb
    public int getStopMonth() { return this.stopMonth; }

    @Valid
    @NotNull
    @JsonProperty("timeline_model_ensembles")
    private S3BucketConfiguration timelineModelEnsemblesConfiguration;
    public S3BucketConfiguration getTimelineModelEnsemblesConfiguration() { return timelineModelEnsemblesConfiguration; }

    @Valid
    @NotNull
    @JsonProperty("timeline_seed_model")
    private S3BucketConfiguration timelineSeedModelConfiguration;
    public S3BucketConfiguration getTimelineSeedModelConfiguration() { return timelineSeedModelConfiguration; }

    @Valid
    @JsonProperty("pill_data")
    private DynamoDBConfiguration pillDataConfiguration;
    public DynamoDBConfiguration getPillDataConfiguration() { return this.pillDataConfiguration; }

    @JsonProperty("provision_key")
    private S3BucketConfiguration provisionKeyConfiguration = S3BucketConfiguration.create("hello-secure", "hello-pvt.pem");
    public S3BucketConfiguration getProvisionKeyConfiguration() { return provisionKeyConfiguration; }

    @Valid
    @NotNull
    @JsonProperty("messeji_http_client")
    private MessejiHttpClientConfiguration messejiHttpClientConfiguration;
    public MessejiHttpClientConfiguration getMessejiHttpClientConfiguration() { return messejiHttpClientConfiguration; }
}
