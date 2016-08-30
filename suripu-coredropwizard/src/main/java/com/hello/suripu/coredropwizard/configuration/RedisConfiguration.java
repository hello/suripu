package com.hello.suripu.coredropwizard.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import jersey.repackaged.com.google.common.base.MoreObjects;


public class RedisConfiguration extends Configuration {

    @JsonProperty("host")
    private String host;

    public String getHost() {
        return host;
    }

    @JsonProperty("port")
    private Integer port;

    public Integer getPort() {
        return port;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("host", host)
                .add("port", port)
                .toString();
    }
}
