package com.hello.suripu.app.resources;

import com.hello.suripu.core.models.DeviceData;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/recent")
public class RecentResource {

    @GET
    @Timed
    public DeviceData getRecent() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        return new DeviceData(0L, 0L,
                DeviceData.floatToDBInt(12.3f),
                DeviceData.floatToDBInt(22.0f),
                DeviceData.floatToDBInt(55.0f),
                DeviceData.floatToDBInt(90f),
                now,
                now.getZone().getOffset(now));
    }

}
