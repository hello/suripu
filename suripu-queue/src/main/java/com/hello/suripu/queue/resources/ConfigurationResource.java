package com.hello.suripu.queue.resources;

import com.hello.suripu.queue.configuration.SQSConfiguration;
import com.hello.suripu.queue.configuration.SuripuQueueConfiguration;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by ksg on 3/22/16
 */

@Path("/config")
public class ConfigurationResource {
    private final SuripuQueueConfiguration configuration;

    public ConfigurationResource(final SuripuQueueConfiguration configuration) {
        this.configuration = configuration;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SuripuQueueConfiguration getConfiguration() {
        return this.configuration;
    }

    @GET
    @Path("/sqs")
    @Produces(MediaType.APPLICATION_JSON)
    public SQSConfiguration getSQSConfiguration() {
        return this.configuration.getSqsConfiguration();
    }
}
