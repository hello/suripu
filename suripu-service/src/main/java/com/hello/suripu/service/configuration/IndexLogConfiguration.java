package com.hello.suripu.service.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IndexLogConfiguration {
    @JsonProperty("private_url")
    private String privateUrl;

    public String getPrivateUrl() {
        return privateUrl;
    }

    @JsonProperty("index_name")
    private String indexName;

    public String getIndexName() {
        return indexName;
    }
}
