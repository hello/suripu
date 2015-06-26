package com.hello.suripu.core.configuration;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class ElasticSearchConfiguration extends Configuration{
    private static final Integer DEFAULT_HTTP_PORT = 9200;
    private static final Integer DEFAULT_TRANSPORT_TCP_PORT = 9300;

    @JsonProperty("host")
    private String host;

    public String getHost() {return host;}

    // according to http: https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-http.html
    @Valid
    @Max(9300)
    @Min(9200)
    @JsonProperty("http_port")
    private Integer httpPort;

    public Integer getHttpPort() {
        if (httpPort == null) {
            return DEFAULT_HTTP_PORT;
        }
        return httpPort;
    }

    // according to https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-transport.html
    @Valid
    @Max(9400)
    @Min(9300)
    @JsonProperty("transport_tcp_port")
    private Integer transportTCPPort;

    public Integer getTransportTCPPort() {
        if (transportTCPPort == null) {
            return DEFAULT_TRANSPORT_TCP_PORT;
        }
        return transportTCPPort;
    }

    @Valid
    @NotNull
    @Max(1000)
    @Min(200)
    @JsonProperty("bulk_actions")
    private Integer bulkActions;

    public Integer getBulkActions() { return bulkActions; }

    @Valid
    @NotNull
    @Max(50)
    @Min(5)
    @JsonProperty("bulk_size_in_megabytes")
    private Integer bulkSizeInMegabyes;

    public Integer getBulkSizeInMegabyes() {return bulkSizeInMegabyes; }

    @Valid
    @NotNull
    @Max(60)
    @Min(5)
    @JsonProperty("flush_interval_in_seconds")
    private Integer flushIntervalInSeconds;

    public Integer getFlushIntervalInSeconds() {return flushIntervalInSeconds; }

    @Valid
    @NotNull
    @Max(10)
    @Min(1)
    @JsonProperty("concurrent_requests")
    private Integer concurrentRequests;

    public Integer getConcurrentRequests() {return concurrentRequests; }


    @Valid
    @NotNull
    @JsonProperty("index_prefix")
    private String indexPrefix;

    public String getIndexPrefix() {return indexPrefix; }
}

