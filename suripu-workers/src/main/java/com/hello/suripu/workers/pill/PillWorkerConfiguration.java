package com.hello.suripu.workers.pill;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.configuration.QueueNames;
import com.yammer.dropwizard.config.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class PillWorkerConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("app_name")
    private String appName;

    public String getAppName() {
        return appName;
    }

    @Valid
    @NotNull
    @JsonProperty("queues")
    private Map<QueueNames,String> queues = new HashMap<QueueNames, String>();

    public ImmutableMap<QueueNames,String> getQueues() {
        return ImmutableMap.copyOf(queues);
    }
}
