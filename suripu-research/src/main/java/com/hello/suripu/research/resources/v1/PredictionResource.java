package com.hello.suripu.research.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.algorithm.sleep.Vote;
import com.hello.suripu.api.datascience.SleepHmmProtos;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.SleepHmmDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.UserLabelDAO;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.FallingAsleepEvent;
import com.hello.suripu.core.models.Events.InBedEvent;
import com.hello.suripu.core.models.Events.OutOfBedEvent;
import com.hello.suripu.core.models.Events.WakeupEvent;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.MultiLightOutUtils;
import com.hello.suripu.core.util.PartnerDataUtils;
import com.hello.suripu.core.util.SleepHmmWithInterpretation;
import com.hello.suripu.core.util.SoundUtils;
import com.hello.suripu.core.util.TimelineUtils;
import com.hello.suripu.core.util.TrackerMotionUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by benjo on 3/6/15.
 */

@Path("/v1/prediction")
public class PredictionResource extends BaseResource {

    private static final String ALGORITHM_SLEEP_SCORED = "sleep_score";
    private static final String ALGORITHM_VOTING = "voting";
    private static final String ALGORITHM_HIDDEN_MARKOV = "hmm";

    private static final Integer MISSING_DATA_DEFAULT_VALUE = 0;
    private static final Integer SLOT_DURATION_MINUTES = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(DataScienceResource.class);
    private final AccountDAO accountDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final DeviceDAO deviceDAO;
    private final UserLabelDAO userLabelDAO;
    private final SleepHmmDAO sleepHmmDAO;


    public PredictionResource(final AccountDAO accountDAO,
                              final TrackerMotionDAO trackerMotionDAO,
                              final DeviceDataDAO deviceDataDAO,
                              final DeviceDAO deviceDAO,
                              final UserLabelDAO userLabelDAO,
                              final SleepHmmDAO sleepHmmDAO) {

        this.accountDAO = accountDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;
        this.userLabelDAO = userLabelDAO;
        this.sleepHmmDAO = sleepHmmDAO;
    }


    private List<TrackerMotion> getPartnerTrackerMotion(final Long accountId, final DateTime startTime, final DateTime endTime) {
        final Optional<Long> optionalPartnerAccountId = this.deviceDAO.getPartnerAccountId(accountId);
        if (optionalPartnerAccountId.isPresent()) {
            final Long partnerAccountId = optionalPartnerAccountId.get();
            LOGGER.debug("partner account {}", partnerAccountId);
            return this.trackerMotionDAO.getBetweenLocalUTC(partnerAccountId, startTime, endTime);
        }
        return Collections.EMPTY_LIST;
    }


    /*  Get sleep/wake events from the hidden markov model  */
    private ImmutableList<Event> getHmmEvents(final DateTime targetDate, final DateTime endDate,final long  currentTimeMillis,final long accountId,
                                     final AllSensorSampleList allSensorSampleList, final List<TrackerMotion> myMotion,final SleepHmmDAO hmmDAO) {


        LOGGER.info("Using HMM for account {}",accountId);

        final Optional<SleepHmmWithInterpretation> hmmOptional = hmmDAO.getLatestModelForDate(accountId, targetDate.getMillis());

        if (!hmmOptional.isPresent()) {
            return ImmutableList.copyOf(Collections.EMPTY_LIST);

        }

        final Optional<SleepHmmWithInterpretation.SleepHmmResult> optionalHmmPredictions = hmmOptional.get().getSleepEventsUsingHMM(
                    allSensorSampleList, myMotion,targetDate.getMillis(),endDate.getMillis(),currentTimeMillis);

        if (!optionalHmmPredictions.isPresent()) {
            return ImmutableList.copyOf(Collections.EMPTY_LIST);
        }


        SleepHmmWithInterpretation.SleepHmmResult res = optionalHmmPredictions.get();

        return res.sleepEvents;



    }

    private List<Event> getSleepScoreEvents(final DateTime targetDate,
                                            final AllSensorSampleList allSensorSampleList,
                                            final List<TrackerMotion> myMotion) {

        SleepEvents<Optional<Event>> sleepEventsFromAlgorithm = TimelineProcessor.fromAlgorithm(targetDate, myMotion,
                allSensorSampleList.get(Sensor.LIGHT),
                allSensorSampleList.get(Sensor.WAVE_COUNT));

        List<Optional<Event>> items = sleepEventsFromAlgorithm.toList();
        List<Event> returnedEvents = new ArrayList<>();

        for (Optional<Event> e : items) {
            if (e.isPresent()) {
                returnedEvents.add(e.get());
            }
        }

        return returnedEvents;

    }


    private List<Event> getVotingEvents(final AllSensorSampleList allSensorSampleList,
                                        final List<TrackerMotion> trackerMotions) {
        // compute lights-out and sound-disturbance events
        final Optional<DateTime> wakeUpWaveTimeOptional = TimelineUtils.getFirstAwakeWaveTime(trackerMotions.get(0).timestamp,
                trackerMotions.get(trackerMotions.size() - 1).timestamp,
                allSensorSampleList.get(Sensor.WAVE_COUNT));

        final List<Event> rawLightEvents = TimelineUtils.getLightEventsWithMultipleLightOut(allSensorSampleList.get(Sensor.LIGHT));
        final List<Event> smoothedLightEvents = MultiLightOutUtils.smoothLight(rawLightEvents, MultiLightOutUtils.DEFAULT_SMOOTH_GAP_MIN);
        final List<Event> lightOuts = MultiLightOutUtils.getValidLightOuts(smoothedLightEvents, trackerMotions, MultiLightOutUtils.DEFAULT_LIGHT_DELTA_WINDOW_MIN);

        final List<DateTime> lightOutTimes = MultiLightOutUtils.getLightOutTimes(lightOuts);
        final Vote vote = new Vote(TrackerMotionUtils.trackerMotionToAmplitudeData(trackerMotions),
                TrackerMotionUtils.trackerMotionToKickOffCounts(trackerMotions),
                SoundUtils.sampleToAmplitudeData(allSensorSampleList.get(Sensor.SOUND)),
                lightOutTimes, wakeUpWaveTimeOptional);

        final SleepEvents<Segment> sleepEvents = vote.getResult(false);
        final Segment goToBedSegment = sleepEvents.goToBed;
        final Segment fallAsleepSegment = sleepEvents.fallAsleep;
        final Segment wakeUpSegment = sleepEvents.wakeUp;
        final Segment outOfBedSegment = sleepEvents.outOfBed;

        //final int smoothWindowSizeInMillis = smoothWindowSizeInMinutes * DateTimeConstants.MILLIS_PER_MINUTE;
        final Event inBedEvent = new InBedEvent(goToBedSegment.getStartTimestamp(),
                goToBedSegment.getEndTimestamp(),
                goToBedSegment.getOffsetMillis());

        final Event fallAsleepEvent = new FallingAsleepEvent(fallAsleepSegment.getStartTimestamp(),
                fallAsleepSegment.getEndTimestamp(),
                fallAsleepSegment.getOffsetMillis());

        final Event wakeUpEvent = new WakeupEvent(wakeUpSegment.getStartTimestamp(),
                wakeUpSegment.getEndTimestamp(),
                wakeUpSegment.getOffsetMillis());

        final Event outOfBedEvent = new OutOfBedEvent(outOfBedSegment.getStartTimestamp(),
                outOfBedSegment.getEndTimestamp(),
                outOfBedSegment.getOffsetMillis());

        final SleepEvents<Event> events = SleepEvents.create(inBedEvent, fallAsleepEvent, wakeUpEvent, outOfBedEvent);
        final Optional<Event> goToBed = Optional.of(events.goToBed);
        final Optional<Event> sleep = Optional.of(events.fallAsleep);
        final Optional<Event> wakeUp = Optional.of(events.wakeUp);
        final Optional<Event> outOfBed = Optional.of(events.outOfBed);

        final SleepEvents<Optional<Event>> optionalSleepEvents = SleepEvents.create(goToBed, sleep, wakeUp, outOfBed);  //SleepEventSafeGuard.sleepEventsHeuristicFix(events, vote.getAggregatedFeatures());
        final List<Optional<Event>> items = optionalSleepEvents.toList();

        List<Event> returnedEvents = new ArrayList<>();

        for (Optional<Event> e : items) {
            if (e.isPresent()) {
                returnedEvents.add(e.get());
            }
        }

        return returnedEvents;

    }

    /* Takes protobuf data directly and decodes  */
    private class LocalSleepHmmDAO implements SleepHmmDAO {
        final Optional<SleepHmmWithInterpretation> hmm;

        public LocalSleepHmmDAO (final String base64data) {
            Optional<SleepHmmWithInterpretation> sleepHmm = Optional.absent();

            if (base64data.length() > 0) {

                BASE64Decoder decoder = new BASE64Decoder();

                try {
                    final byte[] decodedBytes = decoder.decodeBuffer(base64data);

                    final SleepHmmProtos.SleepHmmModelSet proto = SleepHmmProtos.SleepHmmModelSet.parseFrom(decodedBytes);

                    sleepHmm = SleepHmmWithInterpretation.createModelFromProtobuf(proto);


                } catch (IOException e) {
                    LOGGER.debug("failed to decode protobuf");
                }
            }

            hmm = sleepHmm;

        }

        public boolean isValid() {
            return hmm.isPresent();
        }

        @Override
        public Optional<SleepHmmWithInterpretation> getLatestModelForDate(long accountId, long timeOfInterestMillis) {
            return hmm;
        }
    }

    @GET
    @Path("/sleep_events/{account_id}/{query_date_local_utc}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public  List<Event> getSleepPredictionsByUserAndAlgorithm(

            @Scope({OAuthScope.RESEARCH}) final AccessToken accessToken,

            @PathParam("account_id") final  Long accountId,

            @PathParam("query_date_local_utc") final String strTargetDate,

            @DefaultValue(ALGORITHM_HIDDEN_MARKOV) @QueryParam("algorithm") final String algorithm,

            @DefaultValue("") @QueryParam("hmm_protobuf") final String protobuf,

            @DefaultValue("true") @QueryParam("partner_filter") final Boolean usePartnerFilter


    ) {

        /*  default return */
        List<Event> events = new ArrayList<Event>();


        /* deal with proto  */
        SleepHmmDAO hmmDAO = this.sleepHmmDAO;

        LocalSleepHmmDAO localSleepHmmDAO = new LocalSleepHmmDAO(protobuf);

        if (localSleepHmmDAO.isValid()) {
            hmmDAO = localSleepHmmDAO;
        }


        /*  Time stuff */
        final long  currentTimeMillis = DateTime.now().withZone(DateTimeZone.UTC).getMillis();

        final DateTime targetDate = DateTime.parse(strTargetDate, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withHourOfDay(20);
        final DateTime endDate = targetDate.plusHours(16);

        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        /* Get "Pill" data  */
        final List<TrackerMotion> myMotions = trackerMotionDAO.getBetweenLocalUTC(accountId, targetDate, endDate);
        final List<TrackerMotion> partnerMotions = getPartnerTrackerMotion(accountId, targetDate, endDate);

        LOGGER.debug("Length of trackerMotion: {}, partnerTrackerMotion: {}", myMotions.size(),partnerMotions.size());


        List<TrackerMotion> motions = new ArrayList<>();

        if (!partnerMotions.isEmpty() && usePartnerFilter ) {
            try {
                PartnerDataUtils.PartnerMotions separatedMotions = PartnerDataUtils.getMyMotion(myMotions, partnerMotions);
                motions.addAll(separatedMotions.myMotions);
            }
            catch (Exception e) {
                LOGGER.info(e.getMessage());
                motions.addAll(myMotions);
            }
        }
        else {
            motions.addAll(myMotions);
        }

        // get all sensor data, used for light and sound disturbances, and presleep-insights
        AllSensorSampleList allSensorSampleList = new AllSensorSampleList();

        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accountId);

        if (deviceId.isPresent()) {
            allSensorSampleList = deviceDataDAO.generateTimeSeriesByLocalTimeAllSensors(
                    targetDate.getMillis(), endDate.getMillis(),
                    accountId, deviceId.get(), SLOT_DURATION_MINUTES, MISSING_DATA_DEFAULT_VALUE);
        }

         /*  pull out algorithm type */

        switch (algorithm) {
            case ALGORITHM_VOTING:
                events = getVotingEvents(allSensorSampleList, myMotions);
                break;
            case ALGORITHM_SLEEP_SCORED:
                events = getSleepScoreEvents(targetDate, allSensorSampleList, myMotions);
                break;

            case ALGORITHM_HIDDEN_MARKOV:
                events = getHmmEvents(targetDate,endDate,currentTimeMillis,accountId,allSensorSampleList,myMotions,hmmDAO);
                break;

            default:
                events = getHmmEvents(targetDate,endDate,currentTimeMillis,accountId,allSensorSampleList,myMotions,hmmDAO);
                break;

        }


        return events;
        //throw new WebApplicationException(Response.Status.NOT_FOUND);

    }
}
