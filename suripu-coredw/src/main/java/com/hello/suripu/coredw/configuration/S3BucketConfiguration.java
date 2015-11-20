package com.hello.suripu.coredw.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by benjo on 10/20/15.
 */
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
