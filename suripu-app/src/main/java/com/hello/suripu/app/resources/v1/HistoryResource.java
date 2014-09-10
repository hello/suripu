package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.SoundDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.SoundRecord;
import com.hello.suripu.core.models.TrackerMotion;
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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/v1/history")
public class HistoryResource {


    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryResource.class);
    private final SoundDAO soundDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final DeviceDAO deviceDAO;
    private final DeviceDataDAO deviceDataDAO;

    public HistoryResource(final SoundDAO soundDAO,
                           final TrackerMotionDAO trackerMotionDAO,
                           final DeviceDAO deviceDAO,
                           final DeviceDataDAO deviceDataDAO) {
        this.soundDAO = soundDAO;
        this.deviceDAO = deviceDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.deviceDataDAO = deviceDataDAO;

    }

    @Deprecated
    @GET
    @Path("/motion")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public List<TrackerMotion> getMotionBetween(
            @Scope({OAuthScope.API_INTERNAL_DATA_READ}) final AccessToken accessToken,
            @QueryParam("from") final Long from,
            @QueryParam("to") final Long to) {

        if(from == null || to == null){
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }

        if(Math.abs(to - from) > 3 * 24 * 60 * 60 * 1000){
            // Just don't allow a big query
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }

        final ImmutableList<TrackerMotion> trackerMotions = this.trackerMotionDAO.getBetween(
                accessToken.accountId,
                new DateTime(from, DateTimeZone.UTC),
                new DateTime(to, DateTimeZone.UTC)
        );

        return trackerMotions;
    }

    @Deprecated
    @GET
    @Path("/sound")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public List<SoundRecord> getSoundBetween(
            @Scope({OAuthScope.API_INTERNAL_DATA_READ}) final AccessToken accessToken,
            @QueryParam("from") final Long from,
            @QueryParam("to") final Long to) {

        if(from == null || to == null){
            LOGGER.warn("Either from or to params were null");
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }

        if(Math.abs(to - from) > 3 * 24 * 60 * 60 * 1000){
            // Just don't allow a big query
            LOGGER.warn("Query is too big. Range = {}", Math.abs(to - from));
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }

        // FIXME: The device Id is no longer needed, should query everything based on account id.
        final Optional<Long> deviceId = deviceDAO.getByAccountId(accessToken.accountId);
        if(!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        final ImmutableList<SoundRecord> soundRecords = this.soundDAO.getSoundDataBetween(deviceId.get(),
                new DateTime(from, DateTimeZone.UTC),
                new DateTime(to, DateTimeZone.UTC));

        return soundRecords;
    }
}
