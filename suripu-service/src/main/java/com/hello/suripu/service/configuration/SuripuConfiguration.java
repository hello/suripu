package com.hello.suripu.service.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.coredw.configuration.GraphiteConfiguration;
import com.hello.suripu.core.configuration.KinesisConfiguration;
import com.hello.suripu.core.configuration.KinesisLoggerConfiguration;
import com.hello.suripu.core.configuration.NewDynamoDBConfiguration;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.db.DatabaseConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class SuripuConfiguration extends Configuration {

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
    @JsonProperty("audio_bucket_name")
    private String audioBucketName;

    public String getAudioBucketName() {
        return audioBucketName;
    }

    @Valid
    @NotNull
    @JsonProperty("kinesis")
    private KinesisConfiguration kinesisConfiguration;

    public KinesisConfiguration getKinesisConfiguration() {
        return kinesisConfiguration;
    }

    @JsonProperty("room_conditions")
    private Integer roomConditions;
    public Integer getRoomConditions() {
        return roomConditions;
    }

    @JsonProperty("sense_upload_configuration")
    private SenseUploadConfiguration senseUploadConfiguration;
    public SenseUploadConfiguration getSenseUploadConfiguration() { return this.senseUploadConfiguration; }

    @JsonProperty("kinesis_logger")
    private KinesisLoggerConfiguration kinesisLoggerConfiguration;
    public KinesisLoggerConfiguration kinesisLoggerConfiguration() {
        return kinesisLoggerConfiguration;
    }

    @JsonProperty("ota_configuration")
    private OTAConfiguration otaConfiguration;
    public OTAConfiguration getOTAConfiguration() { return this.otaConfiguration; }

    @JsonProperty("aws_access_key_s3")
    private String awsAccessKeyS3;
    public String getAwsAccessKeyS3() {
        return awsAccessKeyS3;
    }

    @JsonProperty("aws_access_secret_s3")
    private String awsAccessSecretS3;
    public String getAwsAccessSecretS3() {
        return awsAccessSecretS3;
    }

    @Valid
    @NotNull
    @Min(60)
    @JsonProperty("ring_duration_sec")
    private Integer ringDuration;
    public Integer getRingDuration(){
        return this.ringDuration;
    }

    @Valid
    @NotNull
    @JsonProperty("dynamodb")
    private NewDynamoDBConfiguration dynamoDBConfiguration;

    public NewDynamoDBConfiguration dynamoDBConfiguration(){
        return dynamoDBConfiguration;
    }
}
