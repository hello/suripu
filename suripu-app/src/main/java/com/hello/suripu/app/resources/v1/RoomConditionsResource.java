package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.DeviceDataDAODynamoDB;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.CurrentRoomState;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.SmoothSample;
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
import java.util.List;
import java.util.Map;

@Path("/v1/room")
public class RoomConditionsResource extends BaseResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomConditionsResource.class);
    private final static ImmutableSet<String> hiddenSensors = ImmutableSet.copyOf(Sets.newHashSet("light_variance", "light_peakiness", "dust_min", "dust_max", "dust_variance"));

    private final AccountDAO accountDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final DeviceDataDAODynamoDB deviceDataDAODynamoDB;
    private final DeviceDAO deviceDAO;
    private final long allowedRangeInSeconds;
    private final SenseColorDAO senseColorDAO;
    private final CalibrationDAO calibrationDAO;


    public RoomConditionsResource(
            final AccountDAO accountDAO, final DeviceDataDAO deviceDataDAO, final DeviceDataDAODynamoDB deviceDataDAODynamoDB,
            final DeviceDAO deviceDAO, final long allowedRangeInSeconds,final SenseColorDAO senseColorDAO, final CalibrationDAO calibrationDAO) {
        this.accountDAO = accountDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDataDAODynamoDB = deviceDataDAODynamoDB;
        this.deviceDAO = deviceDAO;
        this.allowedRangeInSeconds = allowedRangeInSeconds;
        this.senseColorDAO = senseColorDAO;
        this.calibrationDAO = calibrationDAO;
    }


    @Timed
    @GET
    @Path("/current")
    @Produces(MediaType.APPLICATION_JSON)
    public CurrentRoomState current(@Scope({OAuthScope.SENSORS_BASIC}) final AccessToken token,
                                    @DefaultValue("c") @QueryParam("temp_unit") final String unit) {


        final Optional<DeviceAccountPair> deviceIdPair = deviceDAO.getMostRecentSensePairByAccountId(token.accountId);

        if(!deviceIdPair.isPresent()) {
            LOGGER.warn("Did not find any device_id for account_id = {}", token.accountId);
            return CurrentRoomState.empty(false); // at this stage we don't have a Sense id, so we can't use FF.
        }

        final Optional<Calibration> calibrationOptional = getCalibrationStrict(deviceIdPair.get().externalDeviceId);
        final Boolean hasDust = calibrationOptional.isPresent();

        if(isSensorsViewUnavailable(token.accountId)) {
            LOGGER.warn("SENSORS VIEW UNAVAILABLE FOR USER {}", token.accountId);
            return CurrentRoomState.empty(hasDust);
        }

        Integer thresholdInMinutes = 15;
        Integer mostRecentLookBackMinutes = 30;
        if (this.hasDelayCurrentRoomStateThreshold(token.accountId)) {
            thresholdInMinutes = 120;
            mostRecentLookBackMinutes = 120;
        }

        final Optional<DeviceData> data = deviceDataDAO.getMostRecent(token.accountId, deviceIdPair.get().internalDeviceId,
                DateTime.now(DateTimeZone.UTC).plusMinutes(2),
                DateTime.now(DateTimeZone.UTC).minusMinutes(mostRecentLookBackMinutes));


        if(!data.isPresent()) {
            return CurrentRoomState.empty(hasDust);
        }

        //default -- return the usual
        DeviceData deviceData = data.get();

        if (this.hasColorCompensationEnabled(token.accountId)) {
            //color compensation?  get the color
            final Optional<Device.Color> color = senseColorDAO.getColorForSense(deviceIdPair.get().externalDeviceId);
            deviceData = data.get().withCalibratedLight(color); //and compensate 
        }

        LOGGER.debug("Last device data in db = {}", deviceData);

        final CurrentRoomState roomState = CurrentRoomState.fromDeviceData(deviceData, DateTime.now(), thresholdInMinutes, unit, calibrationOptional);

        return roomState.withDust(hasDust);
    }


    @Timed
    @GET
    @Path("/{sensor}/week")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getLastWeek(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @PathParam("sensor") final String sensor,
            @QueryParam("from") Long queryEndTimestampUTC) { // utc or local???

        if (hiddenSensors.contains(sensor)) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

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

        if (hiddenSensors.contains(sensor)) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }
        validateQueryRange(queryEndTimestampUTC, DateTime.now(), accessToken.accountId, allowedRangeInSeconds);

        final int slotDurationInMinutes = 5;
        final long queryStartTimeUTC = new DateTime(queryEndTimestampUTC, DateTimeZone.UTC).minusHours(24).getMillis();


        // get latest device_id connected to this account
        final Optional<DeviceAccountPair> deviceIdPair = deviceDAO.getMostRecentSensePairByAccountId(accessToken.accountId);
        if(!deviceIdPair.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        Optional<Device.Color> color = Optional.absent();
        if (this.hasColorCompensationEnabled(accessToken.accountId)) {
            color = senseColorDAO.getColorForSense(deviceIdPair.get().externalDeviceId);
        }

        final Optional<Calibration> calibrationOptional = getCalibrationStrict(deviceIdPair.get().externalDeviceId);
        final List<Sample> timeSeries = deviceDataDAO.generateTimeSeriesByUTCTime(queryStartTimeUTC, queryEndTimestampUTC,
                accessToken.accountId, deviceIdPair.get().internalDeviceId, slotDurationInMinutes, sensor, missingDataDefaultValue(accessToken.accountId), color, calibrationOptional);

        return adjustTimeSeries(timeSeries, sensor, deviceIdPair.get().externalDeviceId);
    }

    @Timed
    @GET
    @Path("/all_sensors/24hours")
    @Produces(MediaType.APPLICATION_JSON)
    public  Map<Sensor, List<Sample>> getAllSensorsLast24hours(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @PathParam("sensor") String sensor,
            @QueryParam("from_utc") Long queryEndTimestampUTC) {

        if (hiddenSensors.contains(sensor)) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        if (isSensorsViewUnavailable(accessToken.accountId)) {
            LOGGER.warn("SENSORS VIEW UNAVAILABLE FOR USER {}", accessToken.accountId);
            return AllSensorSampleList.getEmptyData();
        }
        validateQueryRange(queryEndTimestampUTC, DateTime.now(), accessToken.accountId, allowedRangeInSeconds);

        final int slotDurationInMinutes = 5;
        final long queryStartTimeUTC = new DateTime(queryEndTimestampUTC, DateTimeZone.UTC).minusHours(24).getMillis();


        // get latest device_id connected to this account
        final Optional<DeviceAccountPair> deviceIdPair = deviceDAO.getMostRecentSensePairByAccountId(accessToken.accountId);
        if(!deviceIdPair.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        Optional<Device.Color> color = Optional.absent();
        if (this.hasColorCompensationEnabled(accessToken.accountId)) {
            color = senseColorDAO.getColorForSense(deviceIdPair.get().externalDeviceId);
        }

        final Optional<Calibration> calibrationOptional = getCalibrationStrict(deviceIdPair.get().externalDeviceId);

        final AllSensorSampleList sensorData = deviceDataDAO.generateTimeSeriesByUTCTimeAllSensors(queryStartTimeUTC, queryEndTimestampUTC,
                accessToken.accountId, deviceIdPair.get().internalDeviceId, slotDurationInMinutes,
                missingDataDefaultValue(accessToken.accountId), color, calibrationOptional);

        if (sensorData.isEmpty()) {
            return AllSensorSampleList.getEmptyData();
        }

        final AllSensorSampleList adjustedSensorData = adjustTimeSeriesAllSensors(sensorData, deviceIdPair.get().externalDeviceId);

        return getDisplayData(adjustedSensorData.getAllData(), hasCalibrationEnabled(deviceIdPair.get().externalDeviceId));
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
        final Optional<DeviceAccountPair> deviceIdPair = deviceDAO.getMostRecentSensePairByAccountId(accessToken.accountId);
        if(!deviceIdPair.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        Optional<Device.Color> color = Optional.absent();
        if (this.hasColorCompensationEnabled(accessToken.accountId)) {
            color = senseColorDAO.getColorForSense(deviceIdPair.get().externalDeviceId);
        }

        final Optional<Calibration> calibrationOptional = getCalibrationStrict(deviceIdPair.get().externalDeviceId);

        final AllSensorSampleList sensorData = deviceDataDAO.generateTimeSeriesByUTCTimeAllSensors(queryStartTimeUTC, queryEndTimestampUTC,
                accessToken.accountId, deviceIdPair.get().internalDeviceId, slotDurationInMinutes,
                missingDataDefaultValue(accessToken.accountId), color, calibrationOptional);

        if (sensorData.isEmpty()) {
            return AllSensorSampleList.getEmptyData();
        }

        final AllSensorSampleList adjustedSensorData = adjustTimeSeriesAllSensors(sensorData, deviceIdPair.get().externalDeviceId);

        return getDisplayData(adjustedSensorData.getAllData(), hasCalibrationEnabled(deviceIdPair.get().externalDeviceId));
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
        if (hiddenSensors.contains(sensor)) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }
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

        if (hiddenSensors.contains(sensor)) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

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

        Optional<Device.Color> color = Optional.absent();

        if (this.hasColorCompensationEnabled(accessToken.accountId)) {
            color = senseColorDAO.getColorForSense(deviceName);
        }

        final Optional<Calibration> calibrationOptional = getCalibrationStrict(deviceName);

        final List<Sample> timeSeries = deviceDataDAO.generateTimeSeriesByUTCTime(queryStartTimeInUTC, queryEndTimestampInUTC,
                accessToken.accountId, deviceId.get(), slotDurationInMinutes,
                sensor, missingDataDefaultValue(accessToken.accountId), color, calibrationOptional);
        return adjustTimeSeries(timeSeries, sensor, deviceName);
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

        if (hiddenSensors.contains(sensor)) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        validateQueryRange(queryEndTimestampUTC, DateTime.now(), accessToken.accountId, allowedRangeInSeconds);

        final int slotDurationInMinutes = 5;
        final long queryStartTimeUTC = new DateTime(queryEndTimestampUTC, DateTimeZone.UTC).minusHours(24).getMillis();

        // check that accountId, deviceName pair exists
        final Optional<Long> deviceId = deviceDAO.getIdForAccountIdDeviceId(accessToken.accountId, deviceName);

        if (!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        Optional<Device.Color> color = Optional.absent();

        if (this.hasColorCompensationEnabled(accessToken.accountId)) {
            color = senseColorDAO.getColorForSense(deviceName);
        }

        final Optional<Calibration> calibrationOptional = getCalibrationStrict(deviceName);

        final List<Sample> timeSeries = deviceDataDAO.generateTimeSeriesByUTCTime(queryStartTimeUTC, queryEndTimestampUTC,
                accessToken.accountId, deviceId.get(), slotDurationInMinutes,
                sensor, missingDataDefaultValue(accessToken.accountId), color, calibrationOptional);

        return adjustTimeSeries(timeSeries, sensor, deviceName);
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
        final Optional<DeviceAccountPair> deviceIdPair = deviceDAO.getMostRecentSensePairByAccountId(accountId);

        if(!deviceIdPair.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        Optional<Device.Color> color = Optional.absent();

        if (this.hasColorCompensationEnabled(accountId)) {
            color = senseColorDAO.getColorForSense(deviceIdPair.get().externalDeviceId);
        }

        final Optional<Calibration> calibrationOptional = getCalibrationStrict(deviceIdPair.get().externalDeviceId);

        final List<Sample> timeSeries = deviceDataDAO.generateTimeSeriesByUTCTime(queryStartTimeInUTC, queryEndTimestampInUTC,
                accountId, deviceIdPair.get().internalDeviceId, slotDurationInMinutes, sensor,
                missingDataDefaultValue(accountId), color, calibrationOptional);

        return adjustTimeSeries(timeSeries, sensor, deviceIdPair.get().externalDeviceId);

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
        final Optional<DeviceAccountPair> deviceIdPair = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if(!deviceIdPair.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        Optional<Device.Color> color = Optional.absent();

        if (this.hasColorCompensationEnabled(accountId)) {
            color = senseColorDAO.getColorForSense(deviceIdPair.get().externalDeviceId);
        }

        final Optional<Calibration> calibrationOptional = getCalibrationStrict(deviceIdPair.get().externalDeviceId);

        List<Sample> timeSeries;
        if (hasDeviceDataDynamoDBEnabled(accountId)) {
            try {
                timeSeries = deviceDataDAODynamoDB.generateTimeSeriesByUTCTime(queryStartTimeInUTC, queryEndTimestampInUTC,
                        accountId, deviceIdPair.get().internalDeviceId, slotDurationInMinutes,
                        sensor, missingDataDefaultValue(accountId), color, calibrationOptional);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Caught exception while attempting to get time series from DynamoDB: {}.\n");
                timeSeries = deviceDataDAO.generateTimeSeriesByUTCTime(queryStartTimeInUTC, queryEndTimestampInUTC,
                        accountId, deviceIdPair.get().internalDeviceId, slotDurationInMinutes,
                        sensor, missingDataDefaultValue(accountId), color, calibrationOptional);
            }
        } else {
             timeSeries = deviceDataDAO.generateTimeSeriesByUTCTime(queryStartTimeInUTC, queryEndTimestampInUTC,
                    accountId, deviceIdPair.get().internalDeviceId, slotDurationInMinutes,
                    sensor, missingDataDefaultValue(accountId), color, calibrationOptional);
        }

        return adjustTimeSeries(timeSeries, sensor, deviceIdPair.get().externalDeviceId);
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
        final Optional<DeviceAccountPair> deviceIdPair = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if(!deviceIdPair.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        Optional<Device.Color> color = Optional.absent();

        if (this.hasColorCompensationEnabled(accountId)) {
            color = senseColorDAO.getColorForSense(deviceIdPair.get().externalDeviceId);
        }

        final Optional<Calibration> calibrationOptional = getCalibrationStrict(deviceIdPair.get().externalDeviceId);

        final AllSensorSampleList sensorData = deviceDataDAO.generateTimeSeriesByUTCTimeAllSensors(queryStartTimeInUTC, queryEndTimestampInUTC,
                accountId, deviceIdPair.get().internalDeviceId, slotDurationInMinutes, missingDataDefaultValue(accountId), color, calibrationOptional);

        if (sensorData.isEmpty()) {
            return AllSensorSampleList.getEmptyData();
        }

        final AllSensorSampleList adjustedSensorData = adjustTimeSeriesAllSensors(sensorData, deviceIdPair.get().externalDeviceId);

        return getDisplayData(adjustedSensorData.getAllData(), hasCalibrationEnabled(deviceIdPair.get().externalDeviceId));
    }

    private static Map<Sensor, List<Sample>> getDisplayData(final Map<Sensor, List<Sample>> allSensorData, Boolean hasDust){
        final Map<Sensor, List<Sample>> displayData = Maps.newHashMap();
        displayData.put(Sensor.LIGHT, allSensorData.get(Sensor.LIGHT));
        displayData.put(Sensor.HUMIDITY, allSensorData.get(Sensor.HUMIDITY));
        displayData.put(Sensor.SOUND, allSensorData.get(Sensor.SOUND));
        displayData.put(Sensor.TEMPERATURE, allSensorData.get(Sensor.TEMPERATURE));
        if(hasDust) {
            displayData.put(Sensor.PARTICULATES, allSensorData.get(Sensor.PARTICULATES));
        }
        return displayData;
    }

    private Calibration getCalibration(final String senseId) {
        final Optional<Calibration> optionalCalibration = this.hasCalibrationEnabled(senseId) ? calibrationDAO.getStrict(senseId) : Optional.<Calibration>absent();
        return optionalCalibration.isPresent() ? optionalCalibration.get() : Calibration.createDefault(senseId);
    }

    private Optional<Calibration> getCalibrationStrict(final String senseId) {
        return calibrationDAO.getStrict(senseId);
    }

    private List<Sample> adjustTimeSeries (final List<Sample> samples, final String sensor, final String senseId) {
        if (Sensor.PARTICULATES.name().equalsIgnoreCase(sensor) && this.hasDustSmoothEnabled(senseId)) {
            return SmoothSample.convert(samples);
        }
        return samples;
    }

    private AllSensorSampleList adjustTimeSeriesAllSensors (final AllSensorSampleList allSensorSampleList, final String senseId) {
        if (!this.hasDustSmoothEnabled(senseId)) {
            return allSensorSampleList;
        }

        allSensorSampleList.update(Sensor.PARTICULATES, SmoothSample.convert(allSensorSampleList.get(Sensor.PARTICULATES)));
        return allSensorSampleList;
    }
}
