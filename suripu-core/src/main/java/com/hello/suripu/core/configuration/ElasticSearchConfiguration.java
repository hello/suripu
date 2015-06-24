package com.hello.suripu.core.configuration;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class ElasticSearchConfiguration extends Configuration{
    @JsonProperty("host")
    private String host;

    public String getHost() {return host;}

    @JsonProperty("http_port")
    private Integer httpPort;

    public Integer getHttpPort() {return httpPort;}

    @JsonProperty("transport_tcp_port")
    private Integer transportTCPPort;

    public Integer getTransportTCPPort() { return transportTCPPort; }

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

}

