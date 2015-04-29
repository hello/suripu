package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.UserLabelDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.DataScience.JoinedSensorsMinuteData;
import com.hello.suripu.core.models.DataScience.UserLabel;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.JsonError;
import com.hello.suripu.core.util.TimelineUtils;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 12/1/14.
 */
@Path("/v1/datascience")
public class DataScienceResource extends BaseResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataScienceResource.class);
    private final AccountDAO accountDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final DeviceDAO deviceDAO;
    private final InsightProcessor insightProcessor;
    private final UserLabelDAO userLabelDAO;

    public DataScienceResource(final AccountDAO accountDAO,
                               final TrackerMotionDAO trackerMotionDAO,
                               final DeviceDataDAO deviceDataDAO,
                               final DeviceDAO deviceDAO,
                               final InsightProcessor insightProcessor,
                               final UserLabelDAO userLabelDAO) {
        this.accountDAO = accountDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;
        this.insightProcessor = insightProcessor;
        this.userLabelDAO = userLabelDAO;
    }

    @GET
    @Path("/pill/{email}/{query_date_local_utc}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TrackerMotion> getMotion(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                             @PathParam("query_date_local_utc") final String date,
                             @PathParam("email") final String email) {
        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withHourOfDay(20);
        final DateTime endDate = targetDate.plusHours(16);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        final Optional<Long> accountId = getAccountIdByEmail(email);
        if (!accountId.isPresent()) {
            LOGGER.debug("ID not found for account {}", email);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final List<TrackerMotion> trackerMotions = trackerMotionDAO.getBetweenLocalUTC(accountId.get(), targetDate, endDate);
        LOGGER.debug("Length of trackerMotion: {}", trackerMotions.size());

        return trackerMotions;
    }

    @GET
    @Path("/light/{email}/{query_date_local_utc}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Event> getLightOut(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                            @PathParam("query_date_local_utc") final String date,
                                            @PathParam("email") final String email) {
        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withHourOfDay(20);
        final DateTime endDate = targetDate.plusHours(16);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        final Optional<Long> accountId = getAccountIdByEmail(email);
        if (!accountId.isPresent()) {
            LOGGER.debug("ID not found for account {}", email);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final Optional<Long> internalSenseIdOptional = this.deviceDAO.getMostRecentSenseByAccountId(accountId.get());

        if(!internalSenseIdOptional.isPresent()){
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if (internalSenseIdOptional.isPresent()) {
            final int slotDurationMins = 1;

            final AllSensorSampleList sensorData = deviceDataDAO.generateTimeSeriesByLocalTimeAllSensors(
                    targetDate.getMillis(), endDate.getMillis(),
                    accountId.get(), internalSenseIdOptional.get(),
                    slotDurationMins,
                    missingDataDefaultValue(accountId.get()));
            final List<Sample> lightData = sensorData.get(Sensor.LIGHT);
            final TimelineUtils timelineUtils = new TimelineUtils();
            final List<Event> lightEvents = timelineUtils.getLightEvents(lightData);
            return lightEvents;
        }

        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }


    @GET
    @Path("/sensors/{email}/{query_date_local_utc}/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getSensors(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                   @PathParam("query_date_local_utc") final String date,
                                   @PathParam("type") final String dataType,
                                   @PathParam("email") final String email) {
        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withHourOfDay(20);
        final DateTime endDate = targetDate.plusHours(16);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        final Optional<Long> accountId = getAccountIdByEmail(email);
        if (!accountId.isPresent()) {
            LOGGER.debug("ID not found for account {}", email);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        final Optional<Long> deviceId = this.deviceDAO.getMostRecentSenseByAccountId(accountId.get());
        if (deviceId.isPresent()) {
            final int slotDurationMins = 1;

            AllSensorSampleList sensorData = this.deviceDataDAO.generateTimeSeriesByLocalTimeAllSensors(
                    targetDate.getMillis(), endDate.getMillis(),
                    accountId.get(), deviceId.get(),
                    slotDurationMins,
                    missingDataDefaultValue(accountId.get()));
            final List<Sample> data = sensorData.get(Sensor.valueOf(dataType));
            return data;
        }
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    // TODO: rm later. temporary endpoint to create insights
    @PUT
    @Path("insights/{category}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createLightInsight(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                   @PathParam("category") int value) {

        final InsightCard.Category category = InsightCard.Category.fromInteger(value);

        final Optional<Account> accountOptional = accountDAO.getById(accessToken.accountId);
        if (accountOptional.isPresent()) {
            final Long accountId = accountOptional.get().id.get();

            final Optional<Long> deviceIdOptional = deviceDAO.getMostRecentSenseByAccountId(accountId);
            if (!deviceIdOptional.isPresent()) {
                return;
            }

//            insightProcessor.generateInsights(accountId, accountOptional.get().created);
            insightProcessor.generateInsightsByCategory(accountId, deviceIdOptional.get(), category);
        }
    }


    // TODO: temporary located here, need to move this to suripu-admin
    @GET
    @Path("/admin/pill/{email}/{query_date_local_utc}/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TrackerMotion> getMotionAdmin(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                              @PathParam("query_date_local_utc") String date,
                                              @PathParam("email") String email) {
        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withHourOfDay(20);
        final DateTime endDate = targetDate.plusHours(16);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        final Optional<Long> accountId = getAccountIdByEmail(email);
        if (!accountId.isPresent()) {
            LOGGER.debug("ID not found for account {}", email);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final List<TrackerMotion> trackerMotions = trackerMotionDAO.getBetweenLocalUTC(accountId.get(), targetDate, endDate);
        LOGGER.debug("Length of trackerMotion: {}", trackerMotions.size());

        return trackerMotions;
    }

    private Optional<Long> getAccountIdByEmail(final String email) {
        final Optional<Account> accountOptional = accountDAO.getByEmail(email);

        if (!accountOptional.isPresent()) {
            LOGGER.debug("Account {} not found", email);
            return Optional.absent();
        }

        final Account account = accountOptional.get();
        if (!account.id.isPresent()) {
            LOGGER.debug("ID not found for account {}", email);
            return Optional.absent();
        }
        return account.id;
    }

    // labeling
    @Timed
    @GET
    @Path("/admin/{email}/{sensor}/day")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getAdminLastDay(
            @Scope({OAuthScope.ADMINISTRATION_READ}) AccessToken accessToken,
            @PathParam("email") final String email,
            @PathParam("sensor") final String sensor,
            @QueryParam("from") Long queryEndTimestampInUTC) {

        final Optional<Long> optionalAccountId = getAccountIdByEmail(email);
        if (!optionalAccountId.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final int slotDurationInMinutes = 5;
        /*
        * We have to minutes one day instead of 24 hours, for the same reason that we want one DAY's
        * data, instead of 24 hours.
         */
        final long queryStartTimeInUTC = new DateTime(queryEndTimestampInUTC, DateTimeZone.UTC).minusDays(1).getMillis();

        // get latest device_id connected to this account
        final Long accountId = optionalAccountId.get();
        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accountId);
        if(!deviceId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return deviceDataDAO.generateTimeSeriesByUTCTime(queryStartTimeInUTC, queryEndTimestampInUTC,
                accountId, deviceId.get(), slotDurationInMinutes, sensor, missingDataDefaultValue(accessToken.accountId));
    }


    @POST
    @Path("/label")
    @Consumes(MediaType.APPLICATION_JSON)
    public void label(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
                      @Valid final UserLabel label) {

        final Optional<Long> optionalAccountId = getAccountIdByEmail(label.email);
        if (!optionalAccountId.isPresent()) {
            LOGGER.debug("Account {} not found", label.email);
            return;
        }

        UserLabel.UserLabelType userLabel = UserLabel.UserLabelType.fromString(label.labelString);

        final DateTime nightDate = DateTime.parse(label.night, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withTimeAtStartOfDay();

        final DateTime labelTimestampUTC = new DateTime(label.ts, DateTimeZone.UTC);

        userLabelDAO.insertUserLabel(optionalAccountId.get(),
                label.email, userLabel.toString().toLowerCase(),
                nightDate, labelTimestampUTC, label.durationMillis, labelTimestampUTC.plusMillis(label.tzOffsetMillis),
                label.tzOffsetMillis, label.note);

    }

    @POST
    @Path("/batch_label")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public int label(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
                      @Valid final List<UserLabel> labels) {

        final List<Long> accountIds = new ArrayList<>();
        final List<String> emails = new ArrayList<>();
        final List<String> userLabels = new ArrayList<>();
        final List<DateTime> nightDates = new ArrayList<>();
        final List<DateTime> UTCTimestamps = new ArrayList<>();
        final List<Integer> durations = new ArrayList<>();
        final List<DateTime> localUTCTimestamps = new ArrayList<>();
        final List<Integer> tzOffsets = new ArrayList<>();
        final List<String> notes = new ArrayList<>();

        for (UserLabel label : labels) {

            final Optional<Long> optionalAccountId = getAccountIdByEmail(label.email);
            if (!optionalAccountId.isPresent()) {
                LOGGER.debug("Account {} not found", label.email);
                continue;
            }

            final Long accountId = optionalAccountId.get();
            accountIds.add(accountId);
            emails.add(label.email);

            UserLabel.UserLabelType userLabel = UserLabel.UserLabelType.fromString(label.labelString);
            userLabels.add(userLabel.toString().toLowerCase());

            final DateTime nightDate = DateTime.parse(label.night, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                    .withZone(DateTimeZone.UTC).withTimeAtStartOfDay();
            nightDates.add(nightDate);

            final DateTime labelTimestampUTC = new DateTime(label.ts, DateTimeZone.UTC);
            UTCTimestamps.add(labelTimestampUTC);
            durations.add(label.durationMillis);
            localUTCTimestamps.add(labelTimestampUTC.plusMillis(label.tzOffsetMillis));

            tzOffsets.add(label.tzOffsetMillis);

            notes.add(label.note);
        }

        int inserted = 0;
        try {
            userLabelDAO.batchInsertUserLabels(accountIds, emails, userLabels, nightDates,
                    UTCTimestamps, durations, localUTCTimestamps, tzOffsets, notes);
            inserted = accountIds.size();
        } catch (UnableToExecuteStatementException exception) {
            LOGGER.warn("Batch insert user labels fails for some reason");
        }

        return inserted;
    }

    @GET
    @Path("/label/{email}/{night}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserLabel> getLabels(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
                                     @PathParam("email") String email,
                                     @PathParam("night") String night) {
        final DateTime nightDate = DateTime.parse(night, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withTimeAtStartOfDay();
        LOGGER.debug("{} {}", email, nightDate);
        return userLabelDAO.getUserLabelsByEmailAndNight(email, nightDate);
    }

    // APIs for Benjo's analysis

    @GET
    @Path("/device_sensors_motion")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<JoinedSensorsMinuteData> getJoinedSensorDataByEmail(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                                @QueryParam("email") String email,
                                                                @QueryParam("account_id") Long accountId,
                                                                @QueryParam("from_ts") Long fromTimestamp) {

        if ( (email == null && accountId == null) || fromTimestamp == null) {
            throw new WebApplicationException(Response.status(400).entity(new JsonError(400,
                    "Missing query parameters, use email or account_id, and from_ts")).build());
        }

        Optional<Account> optionalAccount;
        if (email != null) {
            optionalAccount = accountDAO.getByEmail(email);
        } else {
            optionalAccount = accountDAO.getById(accountId);
        }

        if (!optionalAccount.isPresent() || !optionalAccount.get().id.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final Account account = optionalAccount.get();
        LOGGER.debug("Getting joined sensor minute data for account {} from_ts {}", account.id.get(), fromTimestamp);

        return getJoinedSensorData(account.id.get(), fromTimestamp);
    }

    private List<JoinedSensorsMinuteData> getJoinedSensorData(final Long accountId, final Long ts) {
        LOGGER.debug("Getting joined sensor minute data for account id {} after {}", accountId, ts);

        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if (!deviceAccountPairOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity("This account does not have a sense recently").build());
        }

        final DateTime startTs = new DateTime(ts, DateTimeZone.UTC);
        final DateTime endTs = startTs.plusDays(3); // return 3 days of data max.

        final ImmutableList<TrackerMotion> motionData = trackerMotionDAO.getBetween(
                accountId,
                startTs,
                endTs
        );

        final Map<Long, TrackerMotion> motionSamples = new HashMap<>();
        for (final TrackerMotion motion: motionData) {
            motionSamples.put(motion.timestamp, motion);
        }

        final int slotDurationInMinutes = 1;
        final Integer missingDataDefaultValue = 0;
        final AllSensorSampleList sensorSamples = deviceDataDAO.generateTimeSeriesByUTCTimeAllSensors(
                startTs.getMillis(),
                endTs.getMillis(),
                accountId,
                deviceAccountPairOptional.get().internalDeviceId,
                slotDurationInMinutes,
                missingDataDefaultValue
        );

        final List<Sample> lightSamples = sensorSamples.get(Sensor.LIGHT);
        final List<Sample> waveCounts = sensorSamples.get(Sensor.WAVE_COUNT);

        final int numSamples = lightSamples.size();

        final List<JoinedSensorsMinuteData> joinedSensorsMinuteData = new ArrayList<>();
        for (int i = 0; i < numSamples; i++) {
            final Long timestamp = lightSamples.get(i).dateTime;

            joinedSensorsMinuteData.add(new JoinedSensorsMinuteData(timestamp, accountId,
                    lightSamples.get(i).value,
                    sensorSamples.get(Sensor.SOUND_NUM_DISTURBANCES).get(i).value,
                    sensorSamples.get(Sensor.SOUND_PEAK_DISTURBANCE).get(i).value,
                    motionSamples.containsKey(timestamp) ? motionSamples.get(timestamp).value : null,
                    motionSamples.containsKey(timestamp) ? motionSamples.get(timestamp).kickOffCounts : null,
                    motionSamples.containsKey(timestamp) ? motionSamples.get(timestamp).motionRange : null,
                    motionSamples.containsKey(timestamp) ? motionSamples.get(timestamp).onDurationInSeconds : null,
                    (int)waveCounts.get(i).value,
                    lightSamples.get(i).offsetMillis));
        }

        return joinedSensorsMinuteData;
    }
    
}
