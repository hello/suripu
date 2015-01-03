package com.hello.suripu.core.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;

import javax.validation.constraints.NotNull;

public class SearchifyConfiguration extends Configuration {

    @NotNull
    @JsonProperty("private_url")
    private String privateUrl;

    public String privateUrl() {
        return privateUrl;
    }

    @NotNull
    @JsonProperty("index_name")
    private String indexName;

    public String indexName() {
        return indexName;
    }
}
