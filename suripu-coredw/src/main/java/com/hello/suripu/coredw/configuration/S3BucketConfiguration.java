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


}
