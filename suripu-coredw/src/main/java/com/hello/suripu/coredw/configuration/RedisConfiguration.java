package com.hello.suripu.coredw.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;

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
}
