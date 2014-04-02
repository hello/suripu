package com.hello.suripu.app.resources;

import com.hello.suripu.core.Record;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/recent")
public class RecentResource {

    @GET
    @Timed
    public Record getRecent() {
        return new Record(12.3f, 22.0f, 55.0f, DateTime.now(), 0);
    }

}
