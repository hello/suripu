package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.models.CurrentRoomState;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
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

@Path("/v1/room")
public class RoomConditionsResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomConditionsResource.class);
    private final DeviceDataDAO deviceDataDAO;
    private final DeviceDAO deviceDAO;
    private final long allowedRangeInSeconds;

    public RoomConditionsResource(final DeviceDataDAO deviceDataDAO, final DeviceDAO deviceDAO, final long allowedRangeInSeconds) {
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;
        this.allowedRangeInSeconds = allowedRangeInSeconds;
    }


    @Timed
    @GET
    @Path("/current")
    @Produces(MediaType.APPLICATION_JSON)
    public CurrentRoomState current(@Scope({OAuthScope.SENSORS_BASIC}) final AccessToken token) {

        final Optional<DeviceData> data = deviceDataDAO.getMostRecent(token.accountId);
        if(!data.isPresent()) {
            return CurrentRoomState.empty();
        }

        final DeviceData deviceData = data.get();
        LOGGER.debug("Last device data in db = {}", deviceData);
        final CurrentRoomState roomState = CurrentRoomState.fromDeviceData(deviceData, DateTime.now(), 15);
        return roomState;
    }


    @Timed
    @GET
    @Path("/{sensor}/day")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getLast24hours(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @PathParam("sensor") String sensor,
            @QueryParam("from") Long clientUtcTimestamp) {

        validateQueryRange(clientUtcTimestamp, DateTime.now(), accessToken.accountId, allowedRangeInSeconds);

        final int slotDurationInMinutes = 5;
        final int queryDurationInHours = 24;

        // get latest device_id connected to this account
        final Optional<Long> deviceId = deviceDAO.getByAccountId(accessToken.accountId);
        if(!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return deviceDataDAO.generateTimeSerie(clientUtcTimestamp, accessToken.accountId, deviceId.get(), slotDurationInMinutes, queryDurationInHours, sensor);
    }

    @Timed
    @GET
    @Path("/{sensor}/{device_name}/day")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getLast24hoursDeviceName(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @PathParam("sensor") String sensor,
            @PathParam("device_name") String deviceName,
            @QueryParam("from") Long clientUtcTimestamp) {

        validateQueryRange(clientUtcTimestamp, DateTime.now(), accessToken.accountId, allowedRangeInSeconds);

        final int slotDurationInMinutes = 5;
        final int  queryDurationInHours = 24;

        // check that accountId, deviceName pair exists
        final Optional<Long> deviceId = deviceDAO.getIdForAccountIdDeviceId(accessToken.accountId, deviceName);
        if (!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return deviceDataDAO.generateTimeSerie(clientUtcTimestamp, accessToken.accountId, deviceId.get(), slotDurationInMinutes, queryDurationInHours, sensor);
    }

    @Timed
    @GET
    @Path("/admin/{sensor}/day")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getLast24hoursAdmin(
            @Scope({OAuthScope.ADMINISTRATION_READ}) final AccessToken accessToken,
            @PathParam("sensor") String sensor,
            @QueryParam("account") Long accountId,
            @QueryParam("from") Long clientUtcTimestamp) {

        final int slotDurationInMinutes = 1;
        final int  queryDurationInHours = 24;

        // get latest device_id connected to this account
        final Optional<Long> deviceId = deviceDAO.getByAccountId(accountId);
        if(!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return deviceDataDAO.generateTimeSerie(clientUtcTimestamp, accountId, deviceId.get(), slotDurationInMinutes, queryDurationInHours, sensor);
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


    @Timed
    @GET
    @Path("/{sensor}/week")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getLastWeek(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @PathParam("sensor") final String sensor,
            @QueryParam("from") Long clientUtcTimestamp) {

        validateQueryRange(clientUtcTimestamp, DateTime.now(), accessToken.accountId, allowedRangeInSeconds);

        final int slotDurationInMinutes = 60;
        final int  queryDurationInHours = 24 * 7; // 7 days

        // get latest device_id connected to this account
        final Optional<Long> deviceId = deviceDAO.getByAccountId(accessToken.accountId);
        if(!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return deviceDataDAO.generateTimeSerie(clientUtcTimestamp, accessToken.accountId, deviceId.get(), slotDurationInMinutes, queryDurationInHours, sensor);
    }

}
