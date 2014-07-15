package com.hello.suripu.app.resources;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.models.CurrentRoomState;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/room")
public class RoomConditionsResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomConditionsResource.class);
    private final DeviceDataDAO deviceDataDAO;
    private final long allowedRangeInSeconds;

    public RoomConditionsResource(final DeviceDataDAO deviceDataDAO, final long allowedRangeInSeconds) {
        this.deviceDataDAO = deviceDataDAO;
        this.allowedRangeInSeconds = allowedRangeInSeconds;
    }


    @GET
    @Path("/current")
    @Produces(MediaType.APPLICATION_JSON)
    public CurrentRoomState current(@Scope({OAuthScope.SENSORS_BASIC}) final AccessToken token) {
        final Optional<DeviceData> data = deviceDataDAO.getMostRecent(token.accountId);
        if(!data.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        final CurrentRoomState roomState = CurrentRoomState.fromDeviceData(data.get());
        return roomState;
    }


    @GET
    @Path("/{sensor}/day")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getLast24hours(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @PathParam("sensor") String sensor,
            @QueryParam("from") Long clientUtcTimestamp) {

        validateQueryRange(clientUtcTimestamp, DateTime.now(), accessToken.accountId, allowedRangeInSeconds);

        final int slotDurationInMinutes = 5;
        final int  queryDurationInHours = 24;

        return deviceDataDAO.generateTimeSerie(clientUtcTimestamp, accessToken.accountId, slotDurationInMinutes, queryDurationInHours, sensor);
    }


    /**
     * Validates that the current request start range is within reasonable bounds
     * @param clientUtcTimestamp
     * @param nowForServer
     * @param accountId
     */
    private void validateQueryRange(final Long clientUtcTimestamp, final DateTime nowForServer, final Long accountId, final long allowedRangeInSeconds) {
        if (clientUtcTimestamp == null) {
            throw new WebApplicationException(400);
        }

        if(Math.abs(clientUtcTimestamp - nowForServer.getMillis()) > allowedRangeInSeconds * 1000) {
            LOGGER.warn("Invalid request, {} is too far off for account_id = {}", clientUtcTimestamp, accountId);
            throw new WebApplicationException(400);
        }
    }


    @GET
    @Path("/{sensor}/week")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getLastWeek(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @PathParam("sensor") final String sensor,
            @QueryParam("from") Long clientUtcTimestamp) {

        final int slotDurationInMinutes = 60;
        final int  queryDurationInHours = 24 * 7; // 7 days

        validateQueryRange(clientUtcTimestamp, DateTime.now(), accessToken.accountId, allowedRangeInSeconds);
        return deviceDataDAO.generateTimeSerie(clientUtcTimestamp, accessToken.accountId, slotDurationInMinutes, queryDurationInHours, sensor);
    }

}
