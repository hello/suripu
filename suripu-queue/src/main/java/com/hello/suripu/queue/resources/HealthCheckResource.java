package com.hello.suripu.queue.resources;

import com.hello.suripu.queue.models.QueueHealthCheck;
import com.yammer.metrics.annotation.Timed;
import com.yammer.metrics.core.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by ksg on 3/16/16
 */

@Path("/health")
public class HealthCheckResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckResource.class);

    private final QueueHealthCheck queueHealthCheck;

    public HealthCheckResource (final QueueHealthCheck queueHealthCheck) {
        this.queueHealthCheck = queueHealthCheck;
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Boolean checkHealth() {
        LOGGER.debug("key=suripu-queue action=health-check");
        try {
            return (this.queueHealthCheck.check() == HealthCheck.Result.healthy());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
