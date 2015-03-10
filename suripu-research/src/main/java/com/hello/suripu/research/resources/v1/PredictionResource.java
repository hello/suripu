package com.hello.suripu.research.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.api.datascience.SleepHmmProtos;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.SleepHmmDAO;
import com.hello.suripu.core.db.SleepLabelDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.PartnerDataUtils;
import com.hello.suripu.core.util.SleepHmmWithInterpretation;
import com.hello.suripu.core.util.TimelineUtils;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    private static final String ALGORITHM_HIDDEN_MARKOV = "hmm";

    private static final Integer MISSING_DATA_DEFAULT_VALUE = 0;
    private static final Integer SLOT_DURATION_MINUTES = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(DataScienceResource.class);
    private final AccountDAO accountDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final DeviceDAO deviceDAO;
    private final SleepLabelDAO sleepLabelDAO;
    private final SleepHmmDAO sleepHmmDAO;


    public PredictionResource(final AccountDAO accountDAO,
                              final TrackerMotionDAO trackerMotionDAO,
                              final DeviceDataDAO deviceDataDAO,
                              final DeviceDAO deviceDAO,
                              final SleepLabelDAO sleepLabelDAO,
                              final SleepHmmDAO sleepHmmDAO) {

        this.accountDAO = accountDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;
        this.sleepLabelDAO = sleepLabelDAO;
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

        return res.events;



    }

    private SleepEvents<Optional<Event>> fromAlgorithm(final DateTime targetDate, final List<TrackerMotion> trackerMotions, final Optional<DateTime> lightOutTimeOptional, final Optional<DateTime> wakeUpWaveTimeOptional) {
        Optional<Segment> sleepSegmentOptional;
        Optional<Segment> inBedSegmentOptional = Optional.absent();
        SleepEvents<Optional<Event>> sleepEventsFromAlgorithm = SleepEvents.create(Optional.<Event>absent(),
                Optional.<Event>absent(),
                Optional.<Event>absent(),
                Optional.<Event>absent());

        // A day starts with 8pm local time and ends with 4pm local time next day
        try {
            sleepEventsFromAlgorithm = TimelineUtils.getSleepEvents(targetDate,
                    trackerMotions,
                    lightOutTimeOptional,
                    wakeUpWaveTimeOptional,
                    MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                    MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                    MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                    false);



            if(sleepEventsFromAlgorithm.fallAsleep.isPresent() && sleepEventsFromAlgorithm.wakeUp.isPresent()){
                sleepSegmentOptional = Optional.of(new Segment(sleepEventsFromAlgorithm.fallAsleep.get().getStartTimestamp(),
                        sleepEventsFromAlgorithm.wakeUp.get().getStartTimestamp(),
                        sleepEventsFromAlgorithm.wakeUp.get().getTimezoneOffset()));

                LOGGER.info("Sleep Time From Awake Detection Algorithm: {} - {}",
                        new DateTime(sleepSegmentOptional.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepSegmentOptional.get().getOffsetMillis())),
                        new DateTime(sleepSegmentOptional.get().getEndTimestamp(), DateTimeZone.forOffsetMillis(sleepSegmentOptional.get().getOffsetMillis())));
            }

            if(sleepEventsFromAlgorithm.goToBed.isPresent() && sleepEventsFromAlgorithm.outOfBed.isPresent()){
                inBedSegmentOptional = Optional.of(new Segment(sleepEventsFromAlgorithm.goToBed.get().getStartTimestamp(),
                        sleepEventsFromAlgorithm.outOfBed.get().getStartTimestamp(),
                        sleepEventsFromAlgorithm.outOfBed.get().getTimezoneOffset()));
                LOGGER.info("In Bed Time From Awake Detection Algorithm: {} - {}",
                        new DateTime(inBedSegmentOptional.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(inBedSegmentOptional.get().getOffsetMillis())),
                        new DateTime(inBedSegmentOptional.get().getEndTimestamp(), DateTimeZone.forOffsetMillis(inBedSegmentOptional.get().getOffsetMillis())));
            }


        }catch (Exception ex){ //TODO : catch a more specific exception
            LOGGER.error("Generate sleep period from Awake Detection Algorithm failed: {}", ex.getMessage());
        }

        return  sleepEventsFromAlgorithm;
    }

    private List<Event> getSleepScoreEvents(final DateTime targetDate, final DateTime endDate,final long  currentTimeMillis,final long accountId,
                                            final AllSensorSampleList allSensorSampleList, final List<TrackerMotion> myMotion) {
        // compute lights-out and sound-disturbance events
        Optional<DateTime> lightOutTimeOptional = Optional.absent();
        Optional<DateTime> wakeUpWaveTimeOptional = Optional.absent();
        final List<Event> lightEvents = Lists.newArrayList();

        if (!allSensorSampleList.isEmpty()) {

            // Light
            lightEvents.addAll(TimelineUtils.getLightEvents(allSensorSampleList.get(Sensor.LIGHT)));
            if (lightEvents.size() > 0) {
                lightOutTimeOptional = TimelineUtils.getLightsOutTime(lightEvents);
            }

            // TODO: refactor

            if(!allSensorSampleList.get(Sensor.WAVE_COUNT).isEmpty() && myMotion.size() > 0){
                wakeUpWaveTimeOptional = TimelineUtils.getFirstAwakeWaveTime(myMotion.get(0).timestamp,
                        myMotion.get(myMotion.size() - 1).timestamp,
                        allSensorSampleList.get(Sensor.WAVE_COUNT));
            }
        }
        /*  This can get overided by the HMM if the feature is enabled */
        SleepEvents<Optional<Event>> sleepEventsFromAlgorithm = fromAlgorithm(targetDate, myMotion, lightOutTimeOptional, wakeUpWaveTimeOptional);


        List<Optional<Event>> items = sleepEventsFromAlgorithm.toList();

        List<Event> returnedEvents = new ArrayList<Event>();

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

                    final SleepHmmProtos.SleepHmm proto = SleepHmmProtos.SleepHmm.parseFrom(decodedBytes);

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
            case ALGORITHM_SLEEP_SCORED:
                events = getSleepScoreEvents(targetDate,endDate,currentTimeMillis,accountId,allSensorSampleList,myMotions);
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
