package com.hello.suripu.coredw8.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.dropwizard.Configuration;

public class S3BucketConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("bucket")
    private String bucket;
    public String getBucket(){
        return bucket;
    }


    @Valid
    @NotNull
    @JsonProperty("key")
    private String key;

    public String getKey(){ return key; }

    public static S3BucketConfiguration create(final String bucket, final String key) {
        final S3BucketConfiguration s3BucketConfiguration = new S3BucketConfiguration();
        s3BucketConfiguration.bucket = bucket;
        s3BucketConfiguration.key = key;
        return s3BucketConfiguration;
    }
}
