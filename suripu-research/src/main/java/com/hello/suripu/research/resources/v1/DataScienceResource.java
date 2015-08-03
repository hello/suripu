package com.hello.suripu.research.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.FeedbackDAO;
import com.hello.suripu.core.db.TimelineLogDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.UserLabelDAO;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.DataScience.JoinedSensorsMinuteData;
import com.hello.suripu.core.models.DataScience.UserLabel;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TimelineLog;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.FeedbackUtils;
import com.hello.suripu.core.util.HmmBayesNetMeasurementParameters;
import com.hello.suripu.core.util.JsonError;
import com.hello.suripu.core.util.NamedSleepHmmModel;
import com.hello.suripu.core.util.PartnerDataUtils;
import com.hello.suripu.core.util.SleepHmmBayesNetSensorDataBinning;
import com.hello.suripu.core.util.SleepHmmSensorDataBinning;
import com.hello.suripu.core.util.TimelineUtils;
import com.hello.suripu.core.util.TrackerMotionUtils;
import com.hello.suripu.research.db.LabelDAO;
import com.hello.suripu.research.models.BinnedSensorData;
import com.hello.suripu.research.models.FeedbackUtc;
import com.hello.suripu.research.models.MatchedFeedback;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


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
    private final FeedbackDAO feedbackDAO;
    private final UserLabelDAO userLabelDAO;
    private final TimelineLogDAO timelineLogDAO;
    private final LabelDAO labelDAO;
    private final SenseColorDAO senseColorDAO;

    public static final String BINNING_SOURCE_SLEEPHMM = "hmm";
    public static final String BINNING_SOURCE_BAYES = "bayes";

    public DataScienceResource(final AccountDAO accountDAO,
                               final TrackerMotionDAO trackerMotionDAO,
                               final DeviceDataDAO deviceDataDAO,
                               final DeviceDAO deviceDAO,
                               final UserLabelDAO userLabelDAO,
                               final FeedbackDAO feedbackDAO,
                               final TimelineLogDAO timelineLogDAO,
                               final LabelDAO labelDAO,
                               final SenseColorDAO senseColorDAO) {
        this.accountDAO = accountDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;
        this.userLabelDAO = userLabelDAO;
        this.feedbackDAO = feedbackDAO;
        this.timelineLogDAO = timelineLogDAO;
        this.labelDAO = labelDAO;
        this.senseColorDAO = senseColorDAO;
    }

    @GET
    @Path("/pill/{email}/{query_date_local_utc}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TrackerMotion> getMotion(@Scope({OAuthScope.SENSORS_BASIC, OAuthScope.RESEARCH}) final AccessToken accessToken,
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
    @Path("/motion/{id}/{query_date_local_utc}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TrackerMotion> getMotion(@Scope({OAuthScope.SENSORS_BASIC, OAuthScope.RESEARCH}) final AccessToken accessToken,
                                         @PathParam("query_date_local_utc") final String date,
                                         @PathParam("id") final Long id) {
        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withHourOfDay(20);
        final DateTime endDate = targetDate.plusHours(16);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        final List<TrackerMotion> trackerMotions = trackerMotionDAO.getBetweenLocalUTC(id, targetDate, endDate);
        LOGGER.debug("Length of trackerMotion: {}", trackerMotions.size());

        return trackerMotions;
    }

    @GET
    @Path("/pill/partner/{email}/{query_date_local_utc}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TrackerMotion> getMotionWithPartnerMotionFiltered(@Scope({OAuthScope.SENSORS_BASIC, OAuthScope.RESEARCH}) final AccessToken accessToken,
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

        final List<TrackerMotion> originalTrackerMotions = trackerMotionDAO.getBetweenLocalUTC(accountId.get(), targetDate, endDate);
        final Optional<Long> optionalPartnerAccountId = this.deviceDAO.getPartnerAccountId(accountId.get());

        final List<TrackerMotion> myMotions = new ArrayList<>();

        if (optionalPartnerAccountId.isPresent()) {
            final Long partnerAccountId = optionalPartnerAccountId.get();
            LOGGER.debug("partner account {}", partnerAccountId);
            final List<TrackerMotion> partnerMotions = this.trackerMotionDAO.getBetweenLocalUTC(partnerAccountId, targetDate, endDate);
            if(partnerMotions.isEmpty()){
                myMotions.addAll(originalTrackerMotions);
            }else{
                final PartnerDataUtils partnerDataUtils = new PartnerDataUtils();
                myMotions.addAll(partnerDataUtils.getMyMotion(originalTrackerMotions, partnerMotions).myMotions);
            }
        }

        return myMotions;
    }

    @GET
    @Path("/light/{email}/{query_date_local_utc}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Event> getLightOut(@Scope({OAuthScope.SENSORS_BASIC, OAuthScope.RESEARCH}) final AccessToken accessToken,
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
                    missingDataDefaultValue(accountId.get()),
                    Optional.<Device.Color>absent());
            final List<Sample> lightData = sensorData.get(Sensor.LIGHT);
            final TimelineUtils timelineUtils = new TimelineUtils();
            final List<Event> lightEvents = timelineUtils.getLightEventsWithMultipleLightOut(lightData);
            return lightEvents;
        }

        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }


    @GET
    @Path("/sensors/{email}/{query_date_local_utc}/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getSensors(@Scope({OAuthScope.SENSORS_BASIC, OAuthScope.RESEARCH}) final AccessToken accessToken,
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
                    missingDataDefaultValue(accountId.get()),
                    Optional.<Device.Color>absent());
            final List<Sample> data = sensorData.get(Sensor.valueOf(dataType));
            return data;
        }
        throw new WebApplicationException(Response.Status.NOT_FOUND);
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

    @GET
    @Path("/feedback/{email}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<TimelineFeedback> getFeedbackFromUser(@Scope(OAuthScope.RESEARCH) final AccessToken accessToken,
                                     @PathParam("email") final String email) {
        final Optional<Long> accountId = getAccountIdByEmail(email);
        if (!accountId.isPresent()) {
            LOGGER.debug("ID not found for account {}", email);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final List<TimelineFeedback> feedback = this.feedbackDAO.getForAccount(accountId.get());
        return feedback;
    }

    @GET
    @Path("/timelinelog")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ImmutableList<TimelineLog> getTimelineLogByUser(@Scope({OAuthScope.RESEARCH}) final AccessToken accessToken,
                                                                    @QueryParam("account_id") final Long accountId,
                                                                    @QueryParam("from_ts_utc")final Long fromTimestamp,
                                                                    @DefaultValue("3") @QueryParam("num_days")final  Integer numDays
    ) {
        //day--should be something like 00:00 of evening of interest in UTC
        final DateTime startTs = new DateTime(fromTimestamp, DateTimeZone.UTC);

        return timelineLogDAO.getLogsForUserAndDay(accountId,startTs,Optional.of(numDays));
    }

    @GET
    @Path("/activeusers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ImmutableList<Long> getUserIdsFromTimelineLogByDateRange(@Scope({OAuthScope.RESEARCH}) final AccessToken accessToken,
                                                           @QueryParam("from_ts_utc")final Long fromTimestamp,
                                                           @DefaultValue("3") @QueryParam("num_days")final  Integer numDays
    ) {
        //day--should be something like 00:00 of evening of interest in UTC
        final DateTime startTs = new DateTime(fromTimestamp, DateTimeZone.UTC);

        return timelineLogDAO.getActiveUers(startTs,Optional.of(numDays));
    }


    @GET
    @Path("/matchedfeedback")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ImmutableList<MatchedFeedback> getAllMatchedFeedbackData(@Scope({OAuthScope.RESEARCH}) final AccessToken accessToken,
                                                                  @QueryParam("from_ts_utc")final Long fromTimestamp,
                                                                  @DefaultValue("3") @QueryParam("num_days")final  Integer numDays
    ) {


        List<MatchedFeedback> results = new ArrayList<>();

        //day--should be something like 00:00 of evening of interest in UTC
        final DateTime startTs = new DateTime(fromTimestamp, DateTimeZone.UTC);
        final DateTime endTs = startTs.plusDays(numDays).withHourOfDay(DateTimeUtil.DAY_ENDS_AT_HOUR);

        final ImmutableList<TimelineFeedback> feedbacks = feedbackDAO.getForTimeRange(startTs, endTs);

        //get unique account Ids that provided feedback in date range
        final Set<Long> accountIds = new HashSet<>();

        for (final TimelineFeedback feedback : feedbacks) {

            if (!feedback.accountId.isPresent()) {
                continue;
            }

            accountIds.add(feedback.accountId.get());
        }

        LOGGER.info("Found {} accounts that provided {} items of feedback between {} and {}",accountIds.size(),feedbacks.size(),startTs,endTs);

        final Map<Long,Map<Long,TimelineLog>> logByAccountIdThenDate = new HashMap<>();

        //EXPENSIVE!  go through each account Id and retrieve for date range

        for (final Long accountId : accountIds) {
            final ImmutableList<TimelineLog> logs = timelineLogDAO.getLogsForUserAndDay(accountId, startTs, Optional.of(numDays));

           // for (final TimelineLog log : logs) {
           //     LOGGER.debug("timeline log {} -- {} -- {}", accountId, log.algorithm, new DateTime(log.targetDate).withZone(DateTimeZone.UTC).withTimeAtStartOfDay());
           // }

                //populate map of accountid, then date, then alg
            for (final TimelineLog log : logs) {

                final DateTime targetDateTime = new DateTime(log.targetDate).withZone(DateTimeZone.UTC).withTimeAtStartOfDay();
                final Long targetDate = targetDateTime.getMillis();

                if (!logByAccountIdThenDate.containsKey(accountId)) {
                    logByAccountIdThenDate.put(accountId, new HashMap<Long, TimelineLog>());
                }


                final Map<Long,TimelineLog> entryForThisAccount = logByAccountIdThenDate.get(accountId);

                //if you find multiple logs for the same target date, take the one with the oldest created date
                //this should protect us against changing algorithms
                if (entryForThisAccount.containsKey(targetDate)) {
                    final TimelineLog entry = entryForThisAccount.get(targetDate);

                    //is existing entry older than the proposed entry?
                    if (entry.createdDate < log.createdDate) {
                        //LOGGER.debug("skipping {} {} {} {} {} because its created date is older",accountId,targetDate,log.algorithm,log.version,log.createdDate);
                        continue;
                    }
                }

                //LOGGER.debug("putting {} {} {}",accountId,targetDate,log.algorithm);
                entryForThisAccount.put(targetDate, log);

                //put back into the map
                logByAccountIdThenDate.put(accountId,entryForThisAccount);

            }

        }

        LOGGER.info("{} of {} accounts had logs between {} and {}",logByAccountIdThenDate.size(),accountIds.size(),startTs,endTs);

        //go through feedbacks and figure out which algorithm they came from
        for (final TimelineFeedback feedback : feedbacks) {
            if (!feedback.accountId.isPresent()) {
                continue;
            }


            final Long accountId = feedback.accountId.get();
            final Long date = feedback.dateOfNight.withZone(DateTimeZone.UTC).withTimeAtStartOfDay().getMillis();

           // LOGGER.debug("feedback {} -- {}",accountId,new DateTime(date).withZone(DateTimeZone.UTC).withTimeAtStartOfDay());

            final Map<Long,TimelineLog> entryForThisAccount = logByAccountIdThenDate.get(accountId);

            if (entryForThisAccount == null) {
                continue;
            }

            if (entryForThisAccount.containsKey(date)) {
                final TimelineLog log = entryForThisAccount.get(date);


                final Optional<DateTime> oldTime = FeedbackUtils.convertFeedbackToDateTimeByOldTime(feedback, 0);
                final Optional<DateTime> newTime = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback, 0);

                if (oldTime.isPresent() && newTime.isPresent()) {
                    //populate the results
                    Integer delta = (int) (newTime.get().getMillis() - oldTime.get().getMillis());
                    results.add(new MatchedFeedback(accountId,date,feedback.eventType.toString(),delta,log.algorithm,log.version));
                }

            }

        }

        LOGGER.info("returning {} results",results.size());
        return ImmutableList.copyOf(results);

    }

    @GET
    @Path("/label/{email}/{night}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserLabel> getLabels(@Scope(OAuthScope.RESEARCH) final AccessToken accessToken,
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
    public List<JoinedSensorsMinuteData> getJoinedSensorDataByEmail(@Scope({OAuthScope.RESEARCH}) final AccessToken accessToken,
                                                                @QueryParam("email") String email,
                                                                @QueryParam("account_id") Long accountId,
                                                                @QueryParam("from_ts") Long fromTimestamp,
                                                                @DefaultValue("3") @QueryParam("num_days") Integer numDays) {

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

        return getJoinedSensorData(account.id.get(), fromTimestamp,numDays);
    }

    private List<JoinedSensorsMinuteData> getJoinedSensorData(final Long accountId, final Long ts, final int numDays) {

        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if (!deviceAccountPairOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity("This account does not have a sense recently").build());
        }


        final DateTime date = new DateTime(ts,DateTimeZone.UTC);

        final DateTime startTs = date.withTimeAtStartOfDay().withHourOfDay(DateTimeUtil.DAY_STARTS_AT_HOUR);
        final DateTime endTs = date.withTimeAtStartOfDay().plusDays(numDays).withHourOfDay(DateTimeUtil.DAY_ENDS_AT_HOUR);


        LOGGER.debug("Getting joined sensor minute data for account id {} between {} and {}", accountId, startTs,endTs);

        final ImmutableList<TrackerMotion> motionData = trackerMotionDAO.getBetweenLocalUTC(
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
        final AllSensorSampleList sensorSamples = deviceDataDAO.generateTimeSeriesByLocalTimeAllSensors(
                startTs.getMillis(),
                endTs.getMillis(),
                accountId,
                deviceAccountPairOptional.get().internalDeviceId,
                slotDurationInMinutes,
                missingDataDefaultValue,
                Optional.<Device.Color>absent()
        );

        final List<Sample> lightSamples = sensorSamples.get(Sensor.LIGHT);
        final List<Sample> waveCount = sensorSamples.get(Sensor.WAVE_COUNT);

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
                    (int)waveCount.get(i).value,
                    lightSamples.get(i).offsetMillis));
        }

        return joinedSensorsMinuteData;
    }


    private static List<List<Double>> getMatrix(double [][] x) {
        List<List<Double>> matrix = Lists.newArrayList();
        for (int j = 0; j < x.length; j++) {
            final double [] row = x[j];
            final List<Double> rowList = Lists.newArrayList();
            for (int i = 0; i < x[0].length; i++) {
                rowList.add(row[i]);
            }

            matrix.add(rowList);
        }

        return matrix;
    }

    @GET
    @Path("/binneddata")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BinnedSensorData getBinnedSensorDataByEmailOrAccountID(@Scope({OAuthScope.RESEARCH}) final AccessToken accessToken,
                                                                  @QueryParam("email") String email,
                                                                  @QueryParam("account_id") Long accountId,
                                                                  @QueryParam("from_ts") Long fromTimestamp,
                                                                  @DefaultValue("3") @QueryParam("num_days") Integer numDays,
                                                                  @QueryParam("pill_threshold_counts") Double pillThreshold,
                                                                  @QueryParam("sound_threshold_db") Double soundThreshold,
                                                                  @QueryParam("nat_light_start_hour") Double naturalLightStartHour,
                                                                  @QueryParam("nat_light_stop_hour") Double naturalLightStopHour,
                                                                  @QueryParam("meas_period") Integer numMinutesInMeasPeriod,
                                                                  @DefaultValue(BINNING_SOURCE_BAYES) @QueryParam("source") String binningSource
    ) {


       /* String modelName,
        ImmutableSet<Integer> sleepStates,
        ImmutableSet<Integer> onBedStates,
        ImmutableSet<Integer> allowableEndingStates,
        ImmutableList<Integer> sleepDepthsByState,
        double soundDisturbanceThresholdDB,
        double pillMagnitudeDisturbanceThresholdLsb,
        double naturalLightFilterStartHour,
        double naturalLightFilterStopHour,
        int numMinutesInMeasPeriod,
        boolean isUsingIntervalSearch) */

        final NamedSleepHmmModel model = new NamedSleepHmmModel(null,"dummy", ImmutableSet.copyOf(Collections.EMPTY_SET),ImmutableSet.copyOf(Collections.EMPTY_SET),ImmutableSet.copyOf(Collections.EMPTY_SET),ImmutableList.copyOf(Collections.EMPTY_LIST),
                soundThreshold,pillThreshold,naturalLightStartHour,naturalLightStopHour,numMinutesInMeasPeriod,false,1.0,0.0);

        final HmmBayesNetMeasurementParameters bayesNetMeasurementParameters = new HmmBayesNetMeasurementParameters(false,0.0,1.0,pillThreshold,naturalLightStartHour,naturalLightStopHour,true,numMinutesInMeasPeriod);


        if ( (email == null && accountId == null) || fromTimestamp == null) {
            throw new WebApplicationException(Response.status(400).entity(new JsonError(400,
                    "Missing query parameters, use email or account_id, and from_ts")).build());
        }

        if (numDays == null || pillThreshold == null || soundThreshold == null || naturalLightStartHour == null || naturalLightStopHour == null || numMinutesInMeasPeriod == null) {
            throw new WebApplicationException(Response.status(400).entity(new JsonError(400,
                    "Missing query parameters")).build());
        }

        // GET ACCOUNT ID VIA EMAIL OR DIRECTLY
        final Optional<Account> optionalAccount;
        if (email != null) {
            optionalAccount = accountDAO.getByEmail(email);
        } else {
            optionalAccount = accountDAO.getById(accountId);
        }

        if (!optionalAccount.isPresent() || !optionalAccount.get().id.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final Account account = optionalAccount.get();



        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if (!deviceAccountPairOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonError(500,"This account does not have a sense recently")).build());
        }

        //day--should be something like 00:00 of evening of interest in UTC
        final DateTime date = new DateTime(fromTimestamp,DateTimeZone.UTC);

        final DateTime startTs = date.withTimeAtStartOfDay().withHourOfDay(DateTimeUtil.DAY_STARTS_AT_HOUR);
        final DateTime endTs = date.withTimeAtStartOfDay().plusDays(numDays).withHourOfDay(DateTimeUtil.DAY_ENDS_AT_HOUR);


        LOGGER.debug("Getting binned sensor minute data for account id {} between {} and {}", accountId, startTs,endTs);



        final ImmutableList<TrackerMotion> motionData = trackerMotionDAO.getBetweenLocalUTC(
                accountId,
                startTs,
                endTs
        );


        if (motionData.isEmpty()) {
            LOGGER.warn("No data on day {} for user {}",date,accountId);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonError(500,"No data on this day")).build());
        }

        final Optional<Long> optionalPartnerAccountId = this.deviceDAO.getPartnerAccountId(accountId);

        List<TrackerMotion> filteredPartnerData = Lists.newArrayList();

        if (optionalPartnerAccountId.isPresent()) {
            final Long partnerAccountId = optionalPartnerAccountId.get();
            LOGGER.debug("partner account {}", partnerAccountId);
            ImmutableList<TrackerMotion> partnerMotions = this.trackerMotionDAO.getBetweenLocalUTC(partnerAccountId, startTs, endTs);

            filteredPartnerData = TrackerMotionUtils.removeDuplicatesAndInvalidValues(partnerMotions.asList());

        }

        final List<TrackerMotion> filteredTrackerData = TrackerMotionUtils.removeDuplicatesAndInvalidValues(motionData.asList());

        final int timezoneOffset = filteredTrackerData.get(0).offsetMillis;

        final int slotDurationInMinutes = 1;
        final Integer missingDataDefaultValue = 0;

        final Optional<Device.Color> color = senseColorDAO.getColorForSense(deviceAccountPairOptional.get().externalDeviceId);

        final AllSensorSampleList sensorSamples = deviceDataDAO.generateTimeSeriesByUTCTimeAllSensors(
                startTs.minusMillis(timezoneOffset).getMillis(),
                endTs.minusMillis(timezoneOffset).getMillis(),
                accountId,
                deviceAccountPairOptional.get().internalDeviceId,
                slotDurationInMinutes,
                missingDataDefaultValue,
                color
        );

        final long startTimeUTC = startTs.getMillis() - timezoneOffset;
        final long stopTimeUTC = endTs.getMillis() - timezoneOffset;


        switch (binningSource) {

            case BINNING_SOURCE_SLEEPHMM:
            {
                LOGGER.debug("GETTING YE OLDE HMM SOURCE");

                Optional<SleepHmmSensorDataBinning.BinnedData> optionalBinnedData = Optional.absent();
                try {
                    optionalBinnedData = SleepHmmSensorDataBinning.getBinnedSensorData(sensorSamples, filteredTrackerData, model, startTimeUTC, stopTimeUTC, timezoneOffset);
                } catch (Exception e) {
                    LOGGER.debug(e.toString());

                    throw new WebApplicationException(Response.status(Response.Status.NO_CONTENT)
                            .entity(new JsonError(204, "something failed when getting the binned sensor data.  go check the logs")).build());
                }

                if (!optionalBinnedData.isPresent()) {
                    throw new WebApplicationException(Response.status(Response.Status.NO_CONTENT)
                            .entity(new JsonError(204, "failed to get binned data")).build());
                }


                final SleepHmmSensorDataBinning.BinnedData result = optionalBinnedData.get();

                final List<List<Double>> matrix = getMatrix(result.data);

                List<Long> times = Lists.newArrayList();
                for (int i = 0; i < result.data[0].length; i++) {
                    times.add(startTimeUTC + i * numMinutesInMeasPeriod * 60000L);
                }

                LOGGER.debug("returning matrix of {} x {}",matrix.size(),matrix.get(0).size());
                return new BinnedSensorData(accountId,matrix,times,timezoneOffset);

            }


            case BINNING_SOURCE_BAYES:
            default:
            {
                LOGGER.debug("GETTING BINNED DATA FROM BAYES SOURCE");
                final Optional<SleepHmmBayesNetSensorDataBinning.BinnedData> binnedDataOptional = SleepHmmBayesNetSensorDataBinning.getBinnedSensorData(sensorSamples, filteredTrackerData, filteredPartnerData, bayesNetMeasurementParameters, startTimeUTC, stopTimeUTC, timezoneOffset);

                if (!binnedDataOptional.isPresent()) {
                    throw new WebApplicationException(Response.status(Response.Status.NO_CONTENT)
                            .entity(new JsonError(204, "failed to get binned data")).build());
                }

                final SleepHmmBayesNetSensorDataBinning.BinnedData binnedData = binnedDataOptional.get();

                final List<List<Double>> matrix = getMatrix(binnedData.data);

                List<Long> times = Lists.newArrayList();
                for (int i = 0; i < binnedData.data[0].length; i++) {
                    times.add(startTimeUTC + i * numMinutesInMeasPeriod * 60000L);
                }

                LOGGER.debug("returning matrix of {} x {}",matrix.size(),matrix.get(0).size());
                return new BinnedSensorData(accountId, matrix, times, timezoneOffset);
            }

        }



        /*
        throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new JsonError(500, "failed to get binned data")).build());
        */



    }

    @GET
    @Path("/partneraccountswithfeedback/{query_date_utc}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Long> getPartnerAccountsWithFeedback(@Scope({OAuthScope.RESEARCH}) final AccessToken accessToken,
                                                              @PathParam("query_date_utc") final String date,
                                                              @DefaultValue("1") @QueryParam("num_days") final Integer numDays) {

        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC);

        final DateTime endDate = targetDate.plusDays(numDays);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);


        return labelDAO.getPartnerAccountsThatHadFeedback(targetDate,endDate);


    }


    private Map<Long,Integer> getTimezonesForAccounts(final Set<Long> accountIds) {
        final Map<Long,Integer> timezones = Maps.newHashMap();

        for (final Long accountId : accountIds) {
            final Optional<Account> accountOptional = accountDAO.getById(accountId);

            if (!accountOptional.isPresent()) {
                continue;
            }

            timezones.put(accountId,accountOptional.get().tzOffsetMillis);
        }

        return timezones;
    }


    @GET
    @Path("/partnerfeedback/{query_date_utc}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FeedbackUtc> getPartnerFeedbacks(@Scope({OAuthScope.RESEARCH}) final AccessToken accessToken,
                                                     @PathParam("query_date_utc") final String date,
                                                     @DefaultValue("1") @QueryParam("num_days") final Integer numDays,
                                                     @DefaultValue("ALL_EVENTS")  @QueryParam("event_type") final String eventType) {

        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC);

        final DateTime endDate = targetDate.plusDays(numDays);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        List<TimelineFeedback> feedbacks = Lists.newArrayList();

        if (eventType.equals("ALL_EVENTS")) {
            LOGGER.info("pulling all events");
            feedbacks = labelDAO.getAllPartnerFeedback(targetDate, endDate);
        }
        else {
            LOGGER.info("pulling events for type {}",eventType);

            feedbacks = labelDAO.getPartnerFeedbackByType(targetDate, endDate, eventType);
        }

        //get timezones for each account
        final Set<Long> accountIds = Sets.newHashSet();
        for (final TimelineFeedback timelineFeedback : feedbacks) {
            if (!timelineFeedback.accountId.isPresent()) {
                continue;
            }

            accountIds.add(timelineFeedback.accountId.get());
        }


        final Map<Long,Integer> timezones = getTimezonesForAccounts(accountIds);

        final List<FeedbackUtc> feedbackUtcs = Lists.newArrayList();

        for (final TimelineFeedback timelineFeedback : feedbacks) {
            if (!timelineFeedback.accountId.isPresent()) {
                continue;
            }

            final Long accountId = timelineFeedback.accountId.get();

            if (timezones.get(accountId) == null) {
                continue;
            }

            final Integer tzOffset = timezones.get(accountId);

            final Optional<DateTime> oldTime = FeedbackUtils.convertFeedbackToDateTimeByOldTime(timelineFeedback, tzOffset);
            final Optional<DateTime> newTime = FeedbackUtils.convertFeedbackToDateTimeByNewTime(timelineFeedback, tzOffset);

            ;

            if (!oldTime.isPresent() || !newTime.isPresent()) {
                continue;
            }

            feedbackUtcs.add(new FeedbackUtc(timelineFeedback.eventType.name(), oldTime.get().getMillis(), newTime.get().getMillis(), accountId,DateTimeUtil.dateToYmdString(timelineFeedback.dateOfNight)));

        }

        return feedbackUtcs;
    }

}
