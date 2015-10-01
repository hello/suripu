package com.hello.suripu.workers.framework;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.configuration.KinesisConfiguration;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.coredw.configuration.GraphiteConfiguration;
import com.yammer.dropwizard.config.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class WorkerConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("metrics_enabled")
    private Boolean metricsEnabled;

    public Boolean getMetricsEnabled() {
        return metricsEnabled;
    }

    @Valid
    @NotNull
    @JsonProperty("graphite")
    private GraphiteConfiguration graphite;

    public GraphiteConfiguration getGraphite() {
        return graphite;
    }

    @Valid
    @NotNull
    @JsonProperty("app_name")
    private String appName;

    public String getAppName() {
        return appName;
    }

    @Valid
    @NotNull
    @JsonProperty("kinesis")
    private KinesisConfiguration kinesisConfiguration;

    public String getKinesisEndpoint() {
        return kinesisConfiguration.getEndpoint();
    }

    public ImmutableMap<QueueName,String> getQueues() {
        return ImmutableMap.copyOf(kinesisConfiguration.getStreams());
    }


    @Valid
    @JsonProperty("debug")
    private Boolean debug = Boolean.FALSE;

    public Boolean getDebug() { return debug; }
}
