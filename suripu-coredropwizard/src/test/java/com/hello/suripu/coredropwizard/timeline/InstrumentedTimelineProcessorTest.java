package com.hello.suripu.coredropwizard.timeline;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.db.FeedbackReadDAO;
import com.hello.suripu.core.db.HistoricalPairingDAO;
import com.hello.suripu.core.db.PairingDAO;
import com.hello.suripu.core.db.SenseDataDAODynamoDB;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.models.DataCompleteness;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.SleepScore;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TimelineResult;
import com.hello.suripu.core.util.DateTimeUtil;
import com.librato.rollout.RolloutAdapter;
import com.librato.rollout.RolloutClient;
import dagger.Module;
import dagger.Provides;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by benjo on 1/21/16.
 */
public class InstrumentedTimelineProcessorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstrumentedTimelineProcessorTest.class);

    public final Map<String,Boolean> features = Maps.newHashMap();

    public void setFeature(final String feat,boolean on) {
        if (features.containsKey(feat) && !on) {
            features.remove(feat);
        }

        if (on) {
            features.put(feat,true);
        }
    }

    final public RolloutAdapter rolloutAdapter = new RolloutAdapter() {


        @Override
        public boolean userFeatureActive(String feature, long userId, List<String> userGroups) {
            Boolean hasFeature = features.get(feature);

            if (hasFeature == null) {
                hasFeature = Boolean.FALSE;
            }

            LOGGER.info("userFeatureActive {}={}",feature,hasFeature);
            return hasFeature;
        }

        @Override
        public boolean deviceFeatureActive(String feature, String deviceId, List<String> userGroups) {
            Boolean hasFeature = features.get(feature);

            if (hasFeature == null) {
                hasFeature = Boolean.FALSE;
            }

            LOGGER.info("deviceFeatureActive {}={}",feature,hasFeature);
            return hasFeature;
        }
    };


    @Module(injects = InstrumentedTimelineProcessor.class, library = true)
    public class RolloutLocalModule {
        @Provides @Singleton
        RolloutAdapter providesRolloutAdapter() {
            return rolloutAdapter;
        }

        @Provides @Singleton
        RolloutClient providesRolloutClient(RolloutAdapter adapter) {
            return new RolloutClient(adapter);
        }

    }


    private InstrumentedTimelineProcessor instrumentedTimelineProcessor;
    private static InstrumentedTimelineProcessorHelpers helpers = new InstrumentedTimelineProcessorHelpers();

    @Before
    public void setup() {

        ObjectGraphRoot.getInstance().init(new RolloutLocalModule());
        features.clear();

        final PairingDAO pairingDAO = new HistoricalPairingDAO(helpers.deviceReadDAO, helpers.deviceDataReadAllSensorsDAO);



        instrumentedTimelineProcessor = InstrumentedTimelineProcessor.createTimelineProcessor(
                helpers.pillDataReadDAO,helpers.deviceReadDAO,helpers.deviceDataReadAllSensorsDAO,
                helpers.ringTimeHistoryDAODynamoDB,helpers.feedbackDAO, helpers.sleepHmmDAO,helpers.accountDAO,helpers.sleepStatsDAO, helpers.mainEventTimesDAO,
                new SenseDataDAODynamoDB(pairingDAO, helpers.deviceDataReadAllSensorsDAO, helpers.senseColorDAO, helpers.calibrationDAO),helpers.timeZoneHistoryDAO, helpers.priorsDAO,helpers.featureExtractionModelsDAO,
                helpers.defaultModelEnsembleDAO,helpers.userTimelineTestGroupDAO,
                helpers.sleepScoreParametersDAO,
                Maps.newHashMap(),helpers.algorithmConfiguration, helpers.metric);
    }


    final public static List<LoggingProtos.TimelineLog> getLogsFromTimeline(InstrumentedTimelineProcessor timelineProcessor) {

        final TimelineResult timelineResult = timelineProcessor.retrieveTimelinesFast(0L,DateTime.now(DateTimeZone.UTC),Optional.<TimelineFeedback>absent());

        TestCase.assertTrue(timelineResult.timelines.size() > 0);
        TestCase.assertTrue(timelineResult.logV2.isPresent());

        try {
            final LoggingProtos.BatchLogMessage batchLogMessage = LoggingProtos.BatchLogMessage.newBuilder().mergeFrom(timelineResult.logV2.get().toProtoBuf()).build();

            final List<LoggingProtos.TimelineLog> logs = batchLogMessage.getTimelineLogList();

            return logs;


        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return Lists.newArrayList();

    }



   // @Test
    public void testTimelineProcessorSimple() {

        {
            final List<LoggingProtos.TimelineLog> logs = getLogsFromTimeline(instrumentedTimelineProcessor);

            TestCase.assertTrue(logs.size() >= 2);

            //HMM first, fail, then VOTING
            TestCase.assertTrue(logs.get(0).getAlgorithm().equals(LoggingProtos.TimelineLog.AlgType.HMM));
            TestCase.assertTrue(logs.get(1).getAlgorithm().equals(LoggingProtos.TimelineLog.AlgType.VOTING));
        }

        features.put(FeatureFlipper.ONLINE_HMM_ALGORITHM,true);
        features.put(FeatureFlipper.PILL_PAIR_MOTION_FILTER,true);

        {
            final List<LoggingProtos.TimelineLog> logs = getLogsFromTimeline(instrumentedTimelineProcessor);
            TestCase.assertTrue(logs.size() >= 3);

            TestCase.assertTrue(logs.get(0).getAlgorithm().equals(LoggingProtos.TimelineLog.AlgType.ONLINE_HMM));
            TestCase.assertTrue(logs.get(1).getAlgorithm().equals(LoggingProtos.TimelineLog.AlgType.HMM));
            TestCase.assertTrue(logs.get(2).getAlgorithm().equals(LoggingProtos.TimelineLog.AlgType.VOTING));
        }

    }


    @Test
    public void testTimelineWithDayLightSavings1() {
        final String timeZoneId = "Europe/Berlin";
        final DateTime targetDate = DateTimeUtil.ymdStringToDateTime("2016-10-29");
        final long accountId = 62801L;
        boolean inBedEvent = false;
        boolean asleepEvent = false;
        boolean wakeEvent = false;
        boolean outOfBedEvent = false;
        boolean alarmEvent = false;

        features.put(FeatureFlipper.ONLINE_HMM_ALGORITHM,true);
        features.put(FeatureFlipper.PILL_PAIR_MOTION_FILTER,true);
        FeedbackReadDAO feedbackReadDAO = helpers.feedbackDAO;
        final Optional <TimelineFeedback> timelineFeedback;
        timelineFeedback = Optional.of(feedbackReadDAO.getCorrectedForNight(accountId, targetDate).get(0));
        final TimelineResult timelineResult = instrumentedTimelineProcessor.retrieveTimelinesFast(accountId,targetDate,timelineFeedback);
        for (final SleepSegment segment: timelineResult.timelines.get(0).events) {
            final int offset = segment.getOffsetMillis();
            final long timeUTC = segment.getTimestamp();
            final Event.Type eventType = segment.getEvent().getType();
            switch (eventType){
                case IN_BED:
                    inBedEvent = true;
                    continue;
                case SLEEP:
                    asleepEvent = true;
                    continue;
                case WAKE_UP:
                    wakeEvent = true;
                    continue;
                case OUT_OF_BED:
                    outOfBedEvent = true;
                    continue;
                case ALARM:
                    alarmEvent = true;
                    continue;

            }
            TestCase.assertTrue(offset == DateTimeZone.forID(timeZoneId).getOffset(timeUTC));
        }
        TestCase.assertTrue(inBedEvent);
        TestCase.assertTrue(outOfBedEvent);
        TestCase.assertTrue(asleepEvent);
        TestCase.assertTrue(wakeEvent);TestCase.assertTrue(alarmEvent);
    }

    @Test
    public void testTimelineWithDayLightSavings2() {
        final String timeZoneId = "America/Los_Angeles";
        final DateTime targetDate = DateTimeUtil.ymdStringToDateTime("2016-11-05");
        final long accountId = 66376L;
        boolean inBedEvent = false;
        boolean asleepEvent = false;
        boolean wakeEvent = false;
        boolean outOfBedEvent = false;
        boolean alarmEvent = false;

        features.put(FeatureFlipper.ONLINE_HMM_ALGORITHM,true);
        features.put(FeatureFlipper.PILL_PAIR_MOTION_FILTER,true);
        FeedbackReadDAO feedbackReadDAO = helpers.feedbackDAO;
        final Optional <TimelineFeedback> timelineFeedback;
        timelineFeedback = Optional.of(feedbackReadDAO.getCorrectedForNight(accountId, targetDate).get(0));
        final TimelineResult timelineResult = instrumentedTimelineProcessor.retrieveTimelinesFast(accountId,targetDate,timelineFeedback);
        for (final SleepSegment segment: timelineResult.timelines.get(0).events) {
            final int offset = segment.getOffsetMillis();
            final long timeUTC = segment.getTimestamp();
            final Event.Type eventType = segment.getEvent().getType();
            switch (eventType){
                case IN_BED:
                    inBedEvent = true;
                    continue;
                case SLEEP:
                    asleepEvent = true;
                    continue;
                case WAKE_UP:
                    wakeEvent = true;
                    continue;
                case OUT_OF_BED:
                    outOfBedEvent = true;
                    continue;
                case ALARM:
                    alarmEvent = true;
                    continue;

            }
            TestCase.assertTrue(offset == DateTimeZone.forID(timeZoneId).getOffset(timeUTC));
        }
        TestCase.assertTrue(inBedEvent);
        TestCase.assertTrue(outOfBedEvent);
        TestCase.assertTrue(asleepEvent);
        TestCase.assertTrue(wakeEvent);TestCase.assertTrue(alarmEvent);
    }

    @Test
    public void testPairingFilterErrorFix() {
        final DateTime targetDate = DateTimeUtil.ymdStringToDateTime("2017-03-28");
        final long accountId = 80935;

        features.put(FeatureFlipper.PILL_PAIR_MOTION_FILTER,true);
        features.put(FeatureFlipper.OUTLIER_FILTER,true);
        features.put(FeatureFlipper.HMM_PARTNER_FILTER, true);


        final TimelineResult timelineResult = instrumentedTimelineProcessor.retrieveTimelinesFast(accountId,targetDate,Optional.absent());
        assert(timelineResult.logV2.isPresent());
        assert(timelineResult.dataCompleteness == DataCompleteness.NO_DATA);
    }

    //@Test
    public void testcomputeSleepScoreVersionTransition(){
        final MotionScore testMotionScore = new MotionScore(19, 20, 3500F, 25000, 75);
        final SleepScore testSleepScoreV2 = new SleepScore(88, testMotionScore, 89, 100, 0, "v2");
        final SleepScore testSleepScoreV4 = new SleepScore(92, testMotionScore, 89, 100, 0, "v4" );
        final float testV2V4Weighting = 0.2f;
        final SleepScore testSleepScoreTransition = InstrumentedTimelineProcessor.computeSleepScoreVersionTransition(testSleepScoreV2, testSleepScoreV4, testV2V4Weighting, "v2-V4");
        assertThat(testSleepScoreTransition.value, is(89));
    }
}
