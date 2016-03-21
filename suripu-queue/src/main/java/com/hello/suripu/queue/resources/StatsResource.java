package com.hello.suripu.queue.resources;

import com.hello.suripu.queue.timeline.TimelineQueueConsumerManager;
import com.hello.suripu.queue.timeline.TimelineQueueProducerManager;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by ksg on 3/16/16
 */

@Path("/stats")
public class StatsResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatsResource.class);

    private final TimelineQueueProducerManager producerManager;
    private final TimelineQueueConsumerManager consumerManager;

    public StatsResource(final TimelineQueueProducerManager producerManager,
                         final TimelineQueueConsumerManager consumerManager) {
        this.producerManager = producerManager;
        this.consumerManager = consumerManager;
    }

    @GET
    @Timed
    @Path("/consumer/processed")
    @Produces(MediaType.APPLICATION_JSON)
    public long checkProcessedMessages() {
        return this.consumerManager.getProcessed();
    }

    @GET
    @Timed
    @Path("/consumer/running")
    @Produces(MediaType.APPLICATION_JSON)
    public long checkRunningIterations() {
        return this.consumerManager.getRunningIterations();
    }

    @GET
    @Timed
    @Path("/consumer/idle")
    @Produces(MediaType.APPLICATION_JSON)
    public long checkIdleIterations() {
        return this.consumerManager.getIdleIterations();
    }

    @GET
    @Timed
    @Path("/producer/created")
    @Produces(MediaType.APPLICATION_JSON)
    public long checkCreatedMessages() {
        return this.producerManager.getTotalMessagesCreated();
    }

    @GET
    @Timed
    @Path("/producer/sent")
    @Produces(MediaType.APPLICATION_JSON)
    public long checkSentdMessages() {
        return this.producerManager.getTotalMessagesSent();
    }

    @GET
    @Timed
    @Path("/producer/fail")
    @Produces(MediaType.APPLICATION_JSON)
    public long checkFailedMessages() {
        return this.producerManager.getTotalMessagesFailed();
    }

}
