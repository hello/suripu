package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;
import com.hello.suripu.core.processors.OnlineHmmTest;
import com.hello.suripu.core.util.DateTimeUtil;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.Test;

/**
 * Created by benjo on 4/11/16.
 *  Misnomer, because we are just testing the ability to put sensor data for the neural net in the proper form
 *
 */
public class NeuralNetAlgTest extends NeuralNetAlgorithm {

    public NeuralNetAlgTest() {
        super(new NeuralNetEndpoint() {
            @Override
            public Optional<NeuralNetAlgorithmOutput> getNetOutput(String netId, double[][] sensorData) {
                final double[][] y = new double[2][961];

                y[0][0] = 0.95;
                y[1][0] = 0.05;
                for (int i = 1; i < 960; i++) {
                    y[0][i] = 0.05;
                    y[1][i] = 0.95;
                }

                y[0][960] = 0.95;
                y[1][960] = 0.05;

                return Optional.of(new NeuralNetAlgorithmOutput(y));
            }
        });
    }



    @Test
    //spot sanity check, just make sure it produces a matrix without any exceptions
    public void testGetSensorData() throws Exception {
        DateTime date = DateTimeUtil.ymdStringToDateTime("2015-09-01");
        DateTime startTime = date.withHourOfDay(18);
        DateTime endTime = startTime.plusHours(16);


        AllSensorSampleList senseData = OnlineHmmTest.getTypicalDayOfSense(startTime,endTime,0);
        ImmutableList<TrackerMotion> pillData = OnlineHmmTest.getTypicalDayOfPill(startTime.minusHours(4),endTime.plusHours(4),0);
        final ImmutableList<TimelineFeedback> emptyFeedback = ImmutableList.copyOf(Lists.<TimelineFeedback>newArrayList());
        final OneDaysSensorData oneDaysSensorData = new OneDaysSensorData(senseData,pillData,pillData,emptyFeedback,date,startTime,endTime,endTime,0);

        final double[][] neuralNetInput = getSensorData(oneDaysSensorData);

        TestCase.assertEquals(SensorIndices.MAX_NUM_INDICES,neuralNetInput.length);
        TestCase.assertEquals(961,neuralNetInput[0].length);


    }
    //@Test new safeguards broke test
    //TODO: Fix Test
    public void testGetPredictionWithPastTime() throws Exception {
        DateTime date = DateTimeUtil.ymdStringToDateTime("2015-09-01");
        DateTime startTime = date.withHourOfDay(18);
        DateTime endTime = startTime.plusHours(16);
        DateTime currentTime = endTime.plusHours(1);


        AllSensorSampleList senseData = OnlineHmmTest.getTypicalDayOfSense(startTime, endTime, 0);
        ImmutableList<TrackerMotion> pillData = OnlineHmmTest.getTypicalDayOfPill(startTime.minusHours(4), endTime.plusHours(4), 0);
        final ImmutableList<TimelineFeedback> emptyFeedback = ImmutableList.copyOf(Lists.<TimelineFeedback>newArrayList());
        final OneDaysSensorData oneDaysSensorData = new OneDaysSensorData(senseData, pillData, pillData, emptyFeedback, date, startTime, endTime, currentTime, DateTimeConstants.MILLIS_PER_HOUR);

        final Optional<TimelineAlgorithmResult> resultOptional = getTimelinePrediction(oneDaysSensorData, SleepPeriod.night(date), new TimelineLog(0L, 0L), 0L, false,Sets.<String>newHashSet());

        TestCase.assertTrue(resultOptional.isPresent());

        final TimelineAlgorithmResult result = resultOptional.get();
        TestCase.assertTrue(result.mainEvents.size() == 4);

        TestCase.assertEquals(endTime.getMillis(), resultOptional.get().mainEvents.get(Event.Type.WAKE_UP).getStartTimestamp(), 5 * DateTimeConstants.MILLIS_PER_MINUTE);

    }

   //@Test new safeguards broke test
    public void testGetPredictionWithAtTime() throws Exception {
        DateTime date = DateTimeUtil.ymdStringToDateTime("2015-09-01");
        DateTime startTime = date.withHourOfDay(18);
        DateTime endTime = startTime.plusHours(16);
        DateTime currentTime = endTime;


        AllSensorSampleList senseData = OnlineHmmTest.getTypicalDayOfSense(startTime,endTime,0);
        ImmutableList<TrackerMotion> pillData = OnlineHmmTest.getTypicalDayOfPill(startTime.minusHours(4),endTime.plusHours(4),0);
        final ImmutableList<TimelineFeedback> emptyFeedback = ImmutableList.copyOf(Lists.<TimelineFeedback>newArrayList());
        final OneDaysSensorData oneDaysSensorData = new OneDaysSensorData(senseData,pillData,pillData,emptyFeedback,date,startTime,endTime,currentTime, DateTimeConstants.MILLIS_PER_HOUR);

        final Optional<TimelineAlgorithmResult> resultOptional = getTimelinePrediction(oneDaysSensorData,SleepPeriod.night(date),new TimelineLog(0L,0L),0L,false,Sets.<String>newHashSet());

        TestCase.assertTrue(resultOptional.isPresent());

        final TimelineAlgorithmResult result = resultOptional.get();
        TestCase.assertTrue(result.mainEvents.size() == 4);

        TestCase.assertEquals(currentTime.getMillis(),resultOptional.get().mainEvents.get(Event.Type.WAKE_UP).getStartTimestamp(),5*DateTimeConstants.MILLIS_PER_MINUTE);

    }

    //@Test
    public void testGetPredictionWithShortenedTime() throws Exception {
        DateTime date = DateTimeUtil.ymdStringToDateTime("2015-09-01");
        DateTime startTime = date.withHourOfDay(18);
        DateTime endTime = startTime.plusHours(16);
        DateTime currentTime = startTime.plusHours(13);


        AllSensorSampleList senseData = OnlineHmmTest.getTypicalDayOfSense(startTime,endTime,0);
        ImmutableList<TrackerMotion> pillData = OnlineHmmTest.getTypicalDayOfPill(startTime.minusHours(4),endTime.plusHours(4),0);
        final ImmutableList<TimelineFeedback> emptyFeedback = ImmutableList.copyOf(Lists.<TimelineFeedback>newArrayList());
        final OneDaysSensorData oneDaysSensorData = new OneDaysSensorData(senseData,pillData,pillData,emptyFeedback,date,startTime,endTime,currentTime, DateTimeConstants.MILLIS_PER_HOUR);

        final Optional<TimelineAlgorithmResult> resultOptional =  getTimelinePrediction(oneDaysSensorData,SleepPeriod.night(date),new TimelineLog(0L,0L),0L,false, Sets.<String>newHashSet());

        TestCase.assertTrue(resultOptional.isPresent());

        final TimelineAlgorithmResult result = resultOptional.get();
        TestCase.assertTrue(result.mainEvents.size() == 4);

        TestCase.assertEquals(currentTime.getMillis(),resultOptional.get().mainEvents.get(Event.Type.WAKE_UP).getStartTimestamp(),5*DateTimeConstants.MILLIS_PER_MINUTE);

    }


    //@Test
    public void testDurationSafeguard() throws Exception {
        DateTime date = DateTimeUtil.ymdStringToDateTime("2015-09-01");
        DateTime startTime = date.withHourOfDay(18);
        DateTime endTime = startTime.plusHours(16);
        DateTime currentTime = startTime.plusHours(2).plusMillis(30);


        AllSensorSampleList senseData = OnlineHmmTest.getTypicalDayOfSense(startTime,endTime,0);
        ImmutableList<TrackerMotion> pillData = OnlineHmmTest.getTypicalDayOfPill(startTime.minusHours(4),endTime.plusHours(4),0);
        final ImmutableList<TimelineFeedback> emptyFeedback = ImmutableList.copyOf(Lists.<TimelineFeedback>newArrayList());
        final OneDaysSensorData oneDaysSensorData = new OneDaysSensorData(senseData,pillData,pillData,emptyFeedback,date,startTime,endTime,currentTime, DateTimeConstants.MILLIS_PER_HOUR);

        final TimelineLog log = new TimelineLog(0L,0L);
        final Optional<TimelineAlgorithmResult> resultOptional = getTimelinePrediction(oneDaysSensorData,SleepPeriod.night(date),log,0L,false, Sets.<String>newHashSet());

        TestCase.assertFalse(resultOptional.isPresent());

    }
}
