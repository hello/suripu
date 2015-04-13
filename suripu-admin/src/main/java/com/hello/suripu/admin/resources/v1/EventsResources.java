package com.hello.suripu.admin.resources.v1;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.SenseEventsDAO;
import com.hello.suripu.core.metrics.DeviceEvents;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/v1/events")
public class EventsResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventsResources.class);

    private final SenseEventsDAO senseEventsDAO;

    public EventsResources(final SenseEventsDAO senseEventsDAO) {
        this.senseEventsDAO = senseEventsDAO;
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{device_id}")
    public ImmutableList<DeviceEvents> getDeviceEvents(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                       @PathParam("device_id") final String deviceId,
                                                       @QueryParam("start_ts") final Long startTs,
                                                       @QueryParam("limit") final Integer limit) {
        if (limit == null) {
            return ImmutableList.copyOf(senseEventsDAO.get(deviceId, new DateTime(startTs, DateTimeZone.UTC)));
        }
        return ImmutableList.copyOf(senseEventsDAO.get(deviceId, new DateTime(startTs, DateTimeZone.UTC), limit));
    }
}

