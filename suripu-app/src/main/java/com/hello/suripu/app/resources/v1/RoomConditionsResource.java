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
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
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

        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(token.accountId);
        if(!deviceId.isPresent()) {
            LOGGER.warn("Did not find any device_id for account_id = {}", token.accountId);
            return CurrentRoomState.empty();
        }

        final Optional<DeviceData> data = deviceDataDAO.getMostRecent(token.accountId, deviceId.get());
        if(!data.isPresent()) {
            return CurrentRoomState.empty();
        }

        final DeviceData deviceData = data.get();
        LOGGER.debug("Last device data in db = {}", deviceData);
        final CurrentRoomState roomState = CurrentRoomState.fromDeviceData(deviceData, DateTime.now(), 15);
        return roomState;
    }


    private void validateLocalUTCQueryRange(final DateTime startQueryTimestampLocalUTC, final DateTime endQueryTimestampLocalUTC,
                                            final Long accountId, final long allowedRangeInSeconds) {
        if (startQueryTimestampLocalUTC == null || endQueryTimestampLocalUTC == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if(startQueryTimestampLocalUTC.getZone().equals(DateTimeZone.UTC) == false ||
                endQueryTimestampLocalUTC.getZone().equals(DateTimeZone.UTC) == false) {
            LOGGER.error("validateLocalUTCQueryRange: Query start/end timestamp is not set to local UTC. start: {}, end: {}",
                    startQueryTimestampLocalUTC,
                    endQueryTimestampLocalUTC);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        /*
        * A DAY can be 48 hours. A week can be 2 * 7 * 24 hours at most.
         */
        if(Math.abs(endQueryTimestampLocalUTC.getMillis() - DateTime.now().getMillis()) >
                48 * 60 * DateTimeConstants.MILLIS_PER_MINUTE + allowedRangeInSeconds * 1000) {
            LOGGER.warn("Invalid request, query clock offset {} to {} range is too big for account_id = {}",
                    startQueryTimestampLocalUTC, endQueryTimestampLocalUTC,
                    accountId);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);   // This should be FORBIDDEN
        }
    }

    /*
    * WARNING: This implementation will not giving out the data of last 24 hours.
    * It gives the data of last DAY, which is from a certain local timestamp
    * to that timestamp plus one DAY, keep in mind that one day can be more/less than 24 hours
     */
    @Timed
    @GET
    @Path("/{sensor}/day")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getLastDay(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @PathParam("sensor") String sensor,

            // The @QueryParam("from") should be named as @QueryParam("from_local_utc")
            // to make it explicit that the API is expecting a local time and not confuse
            // the user.
            @QueryParam("from") Long queryEndTimestampInLocalUTC) {

        // From this line I guess this is a bug in the backend instead of the client provide a wrong timestamp..

        // We should expect user provide a local UTC time instead of UTC time, thus
        // the check implementation here is not valid.
        // to fix this, we should expect the client provide its UTC time as well.
        // To provide backward compatibility, I just comment it out for now.
        //validateQueryRange(queryEndTimestampInLocalUTC, DateTime.now(), accessToken.accountId, allowedRangeInSeconds);

        final int slotDurationInMinutes = 5;
        /*
        * We have to minutes one day instead of 24 hours, for the same reason that we want one DAY's
        * data, instead of 24 hours.
         */
        final long queryStartTimeInLocalUTC = new DateTime(queryEndTimestampInLocalUTC, DateTimeZone.UTC).minusDays(1).getMillis();

        validateLocalUTCQueryRange(new DateTime(queryStartTimeInLocalUTC, DateTimeZone.UTC),
                new DateTime(queryEndTimestampInLocalUTC, DateTimeZone.UTC),
                accessToken.accountId,
                allowedRangeInSeconds);

        // get latest device_id connected to this account
        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accessToken.accountId);
        if(!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return deviceDataDAO.generateTimeSeriesByLocalTime(queryStartTimeInLocalUTC, queryEndTimestampInLocalUTC,
                accessToken.accountId, deviceId.get(), slotDurationInMinutes,
                sensor);
    }

    /*
    * WARNING: This implementation will not giving out the data of last 24 hours.
    * It gives the data of last DAY, which is from a certain local timestamp
    * to that timestamp plus one DAY, keep in mind that one day can be more/less than 24 hours
     */
    @Timed
    @GET
    @Path("/{sensor}/{device_name}/day")   // One DAY is not 24 hours, be careful on the naming.
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getLastDayDeviceName(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @PathParam("sensor") String sensor,
            @PathParam("device_name") String deviceName,

            // The @QueryParam("from") should be named as @QueryParam("from_local_utc")
            // to make it explicit that the API is expecting a local time and not confuse
            // the user.
            @QueryParam("from") Long queryEndTimestampInLocalUTC) {

        // From this line I guess this is a bug in the backend instead of the client provide a wrong timestamp..

        // We should expect user provide a local UTC time instead of UTC time, thus
        // the check implementation here is not valid.
        // to fix this, we should expect the client provide its UTC time as well.
        // To provide backward compatibility, I just comment it out for now.
        //validateQueryRange(queryEndTimestampInLocalUTC, DateTime.now(), accessToken.accountId, allowedRangeInSeconds);

        final int slotDurationInMinutes = 5;

        /*
        * We have to minutes one day instead of 24 hours, for the same reason that we want one DAY's
        * data, instead of 24 hours.
         */
        final long queryStartTimeInLocalUTC = new DateTime(queryEndTimestampInLocalUTC, DateTimeZone.UTC).minusDays(1).getMillis();

        validateLocalUTCQueryRange(new DateTime(queryStartTimeInLocalUTC, DateTimeZone.UTC),
                new DateTime(queryEndTimestampInLocalUTC, DateTimeZone.UTC),
                accessToken.accountId,
                allowedRangeInSeconds);

        // check that accountId, deviceName pair exists
        final Optional<Long> deviceId = deviceDAO.getIdForAccountIdDeviceId(accessToken.accountId, deviceName);
        if (!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return deviceDataDAO.generateTimeSeriesByLocalTime(queryStartTimeInLocalUTC, queryEndTimestampInLocalUTC,
                accessToken.accountId, deviceId.get(), slotDurationInMinutes,
                sensor);
    }

    /*
    * WARNING: This implementation will not giving out the data of last 24 hours.
    * It gives the data of last DAY, which is from a certain local timestamp
    * to that timestamp plus one DAY, keep in mind that one day can be more than 24 hours
     */
    @Timed
    @GET
    @Path("/admin/{sensor}/day")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getLastDayAdmin(
            @Scope({OAuthScope.ADMINISTRATION_READ}) final AccessToken accessToken,
            @PathParam("sensor") String sensor,
            @QueryParam("account") Long accountId,

            // The @QueryParam("from") should be named as @QueryParam("from_local_utc")
            // to make it explicit that the API is expecting a local time and not confuse
            // the user.
            @QueryParam("from") Long queryEndTimestampInLocalUTC) {

        final int slotDurationInMinutes = 1;
         /*
        * We have to minutes one day instead of 24 hours, for the same reason that we want one DAY's
        * data, instead of 24 hours.
         */
        final long queryStartTimeInLocalUTC = new DateTime(queryEndTimestampInLocalUTC, DateTimeZone.UTC).minusDays(1).getMillis();

        validateLocalUTCQueryRange(new DateTime(queryStartTimeInLocalUTC, DateTimeZone.UTC),
                new DateTime(queryEndTimestampInLocalUTC, DateTimeZone.UTC),
                accessToken.accountId,
                allowedRangeInSeconds);

        // get latest device_id connected to this account
        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accountId);
        if(!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return deviceDataDAO.generateTimeSeriesByLocalTime(queryStartTimeInLocalUTC, queryEndTimestampInLocalUTC,
                accountId, deviceId.get(), slotDurationInMinutes,
                sensor);
    }

    /**
     * Validates that the current request start range is within reasonable bounds
     * @param clientUtcTimestamp
     * @param nowForServer
     * @param accountId
     */
    private void validateQueryRange(final Long clientUtcTimestamp, final DateTime nowForServer, final Long accountId, final long allowedRangeInSeconds) {
        if (clientUtcTimestamp == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if(Math.abs(clientUtcTimestamp - nowForServer.getMillis()) > allowedRangeInSeconds * 1000) {
            LOGGER.warn("Invalid request, {} is too far off for account_id = {}", clientUtcTimestamp, accountId);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);  // This should be FORBIDDEN
        }
    }


    @Timed
    @GET
    @Path("/{sensor}/week")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getLastWeek(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @PathParam("sensor") final String sensor,
            @QueryParam("from") Long queryEndTimestampInLocalUTC) {

        // We should expect user provide a local UTC time instead of UTC time, thus
        // the check implementation here is not valid.
        // to fix this, we should expect the client provide its UTC time as well.
        // To provide backward compatibility, I just comment it out for now.
        //validateQueryRange(queryEndTimestampInLocalUTC, DateTime.now(), accessToken.accountId, allowedRangeInSeconds);

        final int slotDurationInMinutes = 60;
        //final int  queryDurationInHours = 24 * 7; // 7 days

        /*
        * Again, the same problem:
        * We have to minutes one week instead of 7*24 hours, for the same reason that one week can be more/less than 7 * 24 hours
         */
        final long queryStartTimeInLocalUTC = new DateTime(queryEndTimestampInLocalUTC, DateTimeZone.UTC).minusWeeks(1).getMillis();
        validateLocalUTCQueryRange(new DateTime(queryStartTimeInLocalUTC, DateTimeZone.UTC),
                new DateTime(queryEndTimestampInLocalUTC, DateTimeZone.UTC),
                accessToken.accountId,
                allowedRangeInSeconds);

        // get latest device_id connected to this account
        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accessToken.accountId);
        if(!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return deviceDataDAO.generateTimeSeriesByLocalTime(queryStartTimeInLocalUTC, queryEndTimestampInLocalUTC,
                accessToken.accountId, deviceId.get(), slotDurationInMinutes,
                sensor);
    }



    /*
    * This is the correct implementation of get the last 24 hours' data
    * from the timestamp provided by the client.
     */
    @Timed
    @GET
    @Path("/{sensor}/24hours")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getLast24hours(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @PathParam("sensor") String sensor,
            @QueryParam("from_utc") Long queryEndTimestampUTC) {

        validateQueryRange(queryEndTimestampUTC, DateTime.now(), accessToken.accountId, allowedRangeInSeconds);

        final int slotDurationInMinutes = 5;
        final long queryStartTimeUTC = new DateTime(queryEndTimestampUTC, DateTimeZone.UTC).minusHours(24).getMillis();


        // get latest device_id connected to this account
        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accessToken.accountId);
        if(!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return deviceDataDAO.generateTimeSeriesByUTCTime(queryStartTimeUTC, queryEndTimestampUTC,
                accessToken.accountId, deviceId.get(), slotDurationInMinutes,
                sensor);
    }


    /*
    * This is the correct implementation of get the last 24 hours' data
    * from the timestamp provided by the client.
     */
    @Timed
    @GET
    @Path("/{sensor}/{device_name}/24hours")   // One DAY is not 24 hours, be careful on the naming.
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getLast24hoursDeviceName(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @PathParam("sensor") String sensor,
            @PathParam("device_name") String deviceName,
            @QueryParam("from_utc") Long queryEndTimestampUTC) {

        validateQueryRange(queryEndTimestampUTC, DateTime.now(), accessToken.accountId, allowedRangeInSeconds);

        final int slotDurationInMinutes = 5;
        final long queryStartTimeUTC = new DateTime(queryEndTimestampUTC, DateTimeZone.UTC).minusHours(24).getMillis();

        // check that accountId, deviceName pair exists
        final Optional<Long> deviceId = deviceDAO.getIdForAccountIdDeviceId(accessToken.accountId, deviceName);
        if (!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return deviceDataDAO.generateTimeSeriesByUTCTime(queryStartTimeUTC, queryEndTimestampUTC,
                accessToken.accountId, deviceId.get(), slotDurationInMinutes,
                sensor);
    }

    /*
    * This is the correct implementation of get the last 24 hours' data
    * from the timestamp provided by the client.
     */
    @Timed
    @GET
    @Path("/admin/{sensor}/24hours")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getLast24hoursAdmin(
            @Scope({OAuthScope.ADMINISTRATION_READ}) final AccessToken accessToken,
            @PathParam("sensor") String sensor,
            @QueryParam("account") Long accountId,
            @QueryParam("from_utc") Long queryEndTimestampUTC) {

        final int slotDurationInMinutes = 1;
         /*
        * We have to minutes one day instead of 24 hours, for the same reason that we want one DAY's
        * data, instead of 24 hours.
         */
        final long queryStartTimeUTC = new DateTime(queryEndTimestampUTC, DateTimeZone.UTC).minusHours(24).getMillis();

        // get latest device_id connected to this account
        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accountId);
        if(!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return deviceDataDAO.generateTimeSeriesByUTCTime(queryStartTimeUTC, queryEndTimestampUTC,
                accountId, deviceId.get(), slotDurationInMinutes,
                sensor);
    }

}
