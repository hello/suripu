package com.hello.suripu.queue.resources;

import com.yammer.metrics.annotation.Timed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by ksg on 3/15/16
 */
@Path("/queue")
public class QueueInsertResources {
    @GET
    @Timed
    @Path("/check")
    @Produces(MediaType.APPLICATION_JSON)
    public Boolean getTrends() {
        return true;
    }
}
