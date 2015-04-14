package com.hello.suripu.admin.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.configuration.DynamoDBConfiguration;
import com.hello.suripu.core.configuration.KinesisConfiguration;
import com.hello.suripu.core.configuration.KinesisLoggerConfiguration;
import com.hello.suripu.core.configuration.RedisConfiguration;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.db.DatabaseConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class SuripuAdminConfiguration extends Configuration {

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
    @JsonProperty("debug")
    private Boolean debug = Boolean.FALSE;

    public Boolean getDebug() {
        return debug;
    }

    @Valid
    @NotNull
    @JsonProperty("kinesis")
    private KinesisConfiguration kinesisConfiguration;

    public KinesisConfiguration getKinesisConfiguration() {
        return kinesisConfiguration;
    }


    @Valid
    @NotNull
    @JsonProperty("features_db")
    private DynamoDBConfiguration featuresDynamoDBConfiguration;

    public DynamoDBConfiguration getFeaturesDynamoDBConfiguration() {
        return this.featuresDynamoDBConfiguration;
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
    @JsonProperty("alarm_info_db")
    private DynamoDBConfiguration userInfoDynamoDBConfiguration;

    public DynamoDBConfiguration getUserInfoDynamoDBConfiguration() {
        return this.userInfoDynamoDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("redis")
    private RedisConfiguration redisConfiguration;

    public RedisConfiguration getRedisConfiguration() {
        return redisConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("sense_key_store_dynamo_db")
    private DynamoDBConfiguration senseKeyStoreDynamoConfiguration;

    public DynamoDBConfiguration getSenseKeyStoreDynamoDBConfiguration() {
        return senseKeyStoreDynamoConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("pill_key_store_dynamo_db")
    private DynamoDBConfiguration pillKeyStoreDynamoDBConfiguration;

    public DynamoDBConfiguration getPillKeyStoreDynamoDBConfiguration() {
        return pillKeyStoreDynamoDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("password_reset_db")
    private DynamoDBConfiguration passwordResetDBConfiguration;

    public DynamoDBConfiguration getPasswordResetDBConfiguration() { return passwordResetDBConfiguration; }

    @Valid
    @NotNull
    @JsonProperty("sense_events")
    private DynamoDBConfiguration senseEventsDBConfiguration;

    public DynamoDBConfiguration getSenseEventsDBConfiguration() { return senseEventsDBConfiguration; }

    @Valid
    @NotNull
    @JsonProperty("teams_db")
    private DynamoDBConfiguration teamsDynamoDBConfiguration;

    public DynamoDBConfiguration getTeamsDynamoDBConfiguration(){
        return teamsDynamoDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("firmware_versions")
    private DynamoDBConfiguration firmwareVersionsDynamoDBConfiguration;

    public DynamoDBConfiguration getFirmwareVersionsDynamoDBConfiguration(){
        return firmwareVersionsDynamoDBConfiguration;
    }
}