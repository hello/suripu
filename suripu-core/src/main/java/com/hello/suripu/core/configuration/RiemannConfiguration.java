package com.hello.suripu.core.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;

public class RiemannConfiguration extends Configuration {

    @JsonProperty("host")
    private String host = "riemann.internal.hello.is";

    public String getHost() {
        return host;
    }

    @JsonProperty("port")
    private Integer port = 5555;

    public Integer getPort() {
        return port;
    }
}
