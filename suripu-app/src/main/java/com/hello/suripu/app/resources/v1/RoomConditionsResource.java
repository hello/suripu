package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.CurrentRoomState;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.resources.BaseResource;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/v1/room")
public class RoomConditionsResource extends BaseResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomConditionsResource.class);
    private final AccountDAO accountDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final DeviceDAO deviceDAO;
    private final long allowedRangeInSeconds;

    public RoomConditionsResource(final AccountDAO accountDAO, final DeviceDataDAO deviceDataDAO, final DeviceDAO deviceDAO, final long allowedRangeInSeconds) {
        this.accountDAO = accountDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;
        this.allowedRangeInSeconds = allowedRangeInSeconds;
    }


    @Timed
    @GET
    @Path("/current")
    @Produces(MediaType.APPLICATION_JSON)
    public CurrentRoomState current(@Scope({OAuthScope.SENSORS_BASIC}) final AccessToken token,
                                    @DefaultValue("c") @QueryParam("temp_unit") final String unit) {

        if(isSensorsViewUnavailable(token.accountId)) {
            LOGGER.warn("SENSORS VIEW UNAVAILABLE FOR USER {}", token.accountId);
            return CurrentRoomState.empty();
        }

        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(token.accountId);
        if(!deviceId.isPresent()) {
            LOGGER.warn("Did not find any device_id for account_id = {}", token.accountId);
            return CurrentRoomState.empty();
        }

        final Optional<DeviceData> data = deviceDataDAO.getMostRecent(token.accountId, deviceId.get(), DateTime.now(DateTimeZone.UTC).plusMinutes(2), DateTime.now(DateTimeZone.UTC).minusMinutes(30));
        if(!data.isPresent()) {
            return CurrentRoomState.empty();
        }

        final DeviceData deviceData = data.get();
        LOGGER.debug("Last device data in db = {}", deviceData);
        final CurrentRoomState roomState = CurrentRoomState.fromDeviceData(deviceData, DateTime.now(), 15, unit);
        return roomState;
    }


    @Timed
    @GET
    @Path("/{sensor}/week")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getLastWeek(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @PathParam("sensor") final String sensor,
            @QueryParam("from") Long queryEndTimestampUTC) { // utc or local???
        return retrieveWeekData(accessToken.accountId, sensor, queryEndTimestampUTC);
    }


    @Timed
    @GET
    @Path("/all_sensors/week")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Sensor, List<Sample>> getAllSensorsLastWeek(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @QueryParam("from_utc") Long queryEndTimestampUTC) {
        return retrieveAllSensorsWeekData(accessToken.accountId, queryEndTimestampUTC);
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
                accessToken.accountId, deviceId.get(), slotDurationInMinutes, sensor, missingDataDefaultValue(accessToken.accountId));
    }

    @Timed
    @GET
    @Path("/all_sensors/24hours")
    @Produces(MediaType.APPLICATION_JSON)
    public  Map<Sensor, List<Sample>> getAllSensorsLast24hours(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @PathParam("sensor") String sensor,
            @QueryParam("from_utc") Long queryEndTimestampUTC) {


        if(isSensorsViewUnavailable(accessToken.accountId)) {
            LOGGER.warn("SENSORS VIEW UNAVAILABLE FOR USER {}", accessToken.accountId);
            return AllSensorSampleList.getEmptyData();
        }
        validateQueryRange(queryEndTimestampUTC, DateTime.now(), accessToken.accountId, allowedRangeInSeconds);

        final int slotDurationInMinutes = 5;
        final long queryStartTimeUTC = new DateTime(queryEndTimestampUTC, DateTimeZone.UTC).minusHours(24).getMillis();


        // get latest device_id connected to this account
        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accessToken.accountId);
        if(!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        final AllSensorSampleList sensorData = deviceDataDAO.generateTimeSeriesByUTCTimeAllSensors(queryStartTimeUTC, queryEndTimestampUTC,
                accessToken.accountId, deviceId.get(), slotDurationInMinutes, missingDataDefaultValue(accessToken.accountId));

        if (sensorData.isEmpty()) {
            return AllSensorSampleList.getEmptyData();
        }

        return getDisplayData(sensorData.getAllData());
    }

    @Timed
    @GET
    @Path("/all_sensors/hours")
    @Produces(MediaType.APPLICATION_JSON)
    public  Map<Sensor, List<Sample>> getAllSensorsLastHours(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @QueryParam("quantity") Integer quantity,
            @QueryParam("from_utc") Long queryEndTimestampUTC) {

        validateQueryRange(queryEndTimestampUTC, DateTime.now(), accessToken.accountId, allowedRangeInSeconds);


        if(isSensorsViewUnavailable(accessToken.accountId)) {
            LOGGER.warn("SENSORS VIEW UNAVAILABLE FOR USER {}", accessToken.accountId);
            return AllSensorSampleList.getEmptyData();
        }


        final int slotDurationInMinutes = 5;
        final long queryStartTimeUTC = new DateTime(queryEndTimestampUTC, DateTimeZone.UTC).minusHours(quantity).getMillis();


        // get latest device_id connected to this account
        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accessToken.accountId);
        if(!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        final AllSensorSampleList sensorData = deviceDataDAO.generateTimeSeriesByUTCTimeAllSensors(queryStartTimeUTC, queryEndTimestampUTC,
                accessToken.accountId, deviceId.get(), slotDurationInMinutes, missingDataDefaultValue(accessToken.accountId));
        if (sensorData.isEmpty()) {
            return AllSensorSampleList.getEmptyData();
        }

        return getDisplayData(sensorData.getAllData());
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
            @QueryParam("from") Long queryEndTimestampInUTC) {
        return retrieveDayData(accessToken.accountId, sensor, queryEndTimestampInUTC);
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
            @QueryParam("from") Long queryEndTimestampInUTC) {

        final int slotDurationInMinutes = 5;

        /*
        * We have to minutes one day instead of 24 hours, for the same reason that we want one DAY's
        * data, instead of 24 hours.
         */
        final long queryStartTimeInUTC = new DateTime(queryEndTimestampInUTC, DateTimeZone.UTC).minusDays(1).getMillis();

        validateQueryRange(queryEndTimestampInUTC, DateTime.now(), accessToken.accountId, allowedRangeInSeconds);

        // check that accountId, deviceName pair exists
        final Optional<Long> deviceId = deviceDAO.getIdForAccountIdDeviceId(accessToken.accountId, deviceName);
        if (!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return deviceDataDAO.generateTimeSeriesByUTCTime(queryStartTimeInUTC, queryEndTimestampInUTC,
                accessToken.accountId, deviceId.get(), slotDurationInMinutes,
                sensor, missingDataDefaultValue(accessToken.accountId));
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
                sensor, missingDataDefaultValue(accessToken.accountId));
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

    private List<Sample> retrieveDayData(final Long accountId, final String sensor, final Long queryEndTimestampInUTC) {

        if(isSensorsViewUnavailable(accountId)) {
            LOGGER.warn("SENSORS VIEW UNAVAILABLE FOR USER {}", accountId);
            return Collections.EMPTY_LIST;
        }

        final int slotDurationInMinutes = 5;
        /*
        * We have to minutes one day instead of 24 hours, for the same reason that we want one DAY's
        * data, instead of 24 hours.
         */
        final long queryStartTimeInUTC = new DateTime(queryEndTimestampInUTC, DateTimeZone.UTC).minusDays(1).getMillis();

        validateQueryRange(queryEndTimestampInUTC,
                DateTime.now(),
                accountId,
                allowedRangeInSeconds
        );

        // get latest device_id connected to this account
        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accountId);
        if(!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return deviceDataDAO.generateTimeSeriesByUTCTime(queryStartTimeInUTC, queryEndTimestampInUTC,
                accountId, deviceId.get(), slotDurationInMinutes, sensor, missingDataDefaultValue(accountId));

    }

    private List<Sample> retrieveWeekData(final Long accountId, final String sensor, final Long queryEndTimestampInUTC) {

        if(isSensorsViewUnavailable(accountId)) {
            LOGGER.warn("SENSORS VIEW UNAVAILABLE FOR USER {}", accountId);
            return Collections.EMPTY_LIST;
        }

        final int slotDurationInMinutes = 60;
        //final int  queryDurationInHours = 24 * 7; // 7 days

        /*
        * Again, the same problem:
        * We have to minutes one week instead of 7*24 hours, for the same reason that one week can be more/less than 7 * 24 hours
         */
        final long queryStartTimeInUTC = new DateTime(queryEndTimestampInUTC, DateTimeZone.UTC).minusWeeks(1).getMillis();
        validateQueryRange(queryEndTimestampInUTC,
                DateTime.now(),
                accountId,
                allowedRangeInSeconds);

        // get latest device_id connected to this account
        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accountId);
        if(!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return deviceDataDAO.generateTimeSeriesByUTCTime(queryStartTimeInUTC, queryEndTimestampInUTC,
                accountId, deviceId.get(), slotDurationInMinutes,
                sensor, missingDataDefaultValue(accountId));
    }

    private Map<Sensor, List<Sample>> retrieveAllSensorsWeekData(final Long accountId, final Long queryEndTimestampInUTC) {

        if(isSensorsViewUnavailable(accountId)) {
            LOGGER.warn("SENSORS VIEW UNAVAILABLE FOR USER {}", accountId);
            return AllSensorSampleList.getEmptyData();
        }

        final int slotDurationInMinutes = 60;
        //final int  queryDurationInHours = 24 * 7; // 7 days

        /*
        * Again, the same problem:
        * We have to minutes one week instead of 7*24 hours, for the same reason that one week can be more/less than 7 * 24 hours
         */
        final long queryStartTimeInUTC = new DateTime(queryEndTimestampInUTC, DateTimeZone.UTC).minusWeeks(1).getMillis();

        validateQueryRange(queryEndTimestampInUTC, DateTime.now(), accountId, allowedRangeInSeconds);

        // get latest device_id connected to this account
        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accountId);
        if(!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        final AllSensorSampleList sensorData = deviceDataDAO.generateTimeSeriesByUTCTimeAllSensors(queryStartTimeInUTC, queryEndTimestampInUTC,
                accountId, deviceId.get(), slotDurationInMinutes, missingDataDefaultValue(accountId));

        if (sensorData.isEmpty()) {
            return AllSensorSampleList.getEmptyData();
        }

        return getDisplayData(sensorData.getAllData());
    }

    private static Map<Sensor, List<Sample>> getDisplayData(final Map<Sensor, List<Sample>> allSensorData){
        final Map<Sensor, List<Sample>> displayData = new HashMap<>();
        displayData.put(Sensor.LIGHT, allSensorData.get(Sensor.LIGHT));
        displayData.put(Sensor.HUMIDITY, allSensorData.get(Sensor.HUMIDITY));
        displayData.put(Sensor.SOUND, allSensorData.get(Sensor.SOUND));
        displayData.put(Sensor.TEMPERATURE, allSensorData.get(Sensor.TEMPERATURE));
//        displayData.put(Sensor.PARTICULATES, allSensorData.get(Sensor.PARTICULATES));
        return displayData;
    }
}
