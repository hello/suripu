package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;
import com.hello.suripu.core.processors.OnlineHmmTest;
import com.hello.suripu.core.util.CSVLoader;
import com.hello.suripu.core.util.DateTimeUtil;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by benjo on 4/11/16.
 *  Misnomer, because we are just testing the ability to put sensor data for the neural net in the proper form
 *
 */
public class NeuralNetFourEventAlgTest extends NeuralNetFourEventAlgorithm{

    public NeuralNetFourEventAlgTest() {
        super(new NeuralNetEndpoint() {
            @Override
            public Optional<NeuralNetAlgorithmOutput> getNetOutput(String netId, double[][] sensorData) {
                final double [][] y = getNNOutput();
                return Optional.of(new NeuralNetAlgorithmOutput(y));
            }
        });
    }

    private static double [][] getNNOutput(){
        final int nbClasses = 9;
        final int timeSteps = 961;
        final double[][] y = new double[nbClasses][timeSteps];

        final URL fixtureCSVFile = Resources.getResource("fixtures/neuralNetFourEventOutput.csv");
        try{
            final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 1; i < lines.length; i++) {
                final String[] columns = lines[i].split(",");
                for (int colIndex = 0; colIndex < nbClasses; colIndex++) {
                    y[colIndex][i] = Float.parseFloat(columns[colIndex + 1].trim());
                }
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }
        return y;
    }

    private static double [][] getNNInput(){
        final int dataDim= 16;
        final int timeSteps = 961;
        final double[][] xx = new double[dataDim][timeSteps];

        final URL fixtureCSVFile = Resources.getResource("fixtures/neuralNetFourEventInput.csv");
        try{
            final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 1; i < lines.length; i++) {
                final String[] columns = lines[i].split(",");
                for (int colIndex = 0; colIndex < dataDim; colIndex++) {
                    xx[colIndex][i] = Float.parseFloat(columns[colIndex + 1].trim());
                }
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }
        return xx;
    }
    //testNight: 1751, 2016-05-20
    private static OneDaysSensorData getOneDaySensorData(){

        final ImmutableList<TrackerMotion> trackerMotions= ImmutableList.copyOf(CSVLoader.loadTrackerMotionFromCSV("fixtures/tracker_motion/nn_raw_tracker_motion.csv"));
        final ImmutableList<TrackerMotion> partnerTrackerMotions = ImmutableList.copyOf(new ArrayList<TrackerMotion>());
        final ImmutableList<TimelineFeedback> feedback = ImmutableList.copyOf(new ArrayList<TimelineFeedback>());
        final AllSensorSampleList allSensorSampleList = new AllSensorSampleList();
        final List<Sample> light = CSVLoader.loadSensorDataFromCSV("fixtures/algorithm/nn_raw_light.csv");
        final List<Sample> peakDisturbance =  CSVLoader.loadSensorDataFromCSV("fixtures/algorithm/nn_raw_peak_disturbance.csv");
        final List<Sample> numDisturbances =  CSVLoader.loadSensorDataFromCSV("fixtures/algorithm/nn_raw_noise_disturbance.csv");
        final List<Sample> waveCount = CSVLoader.loadSensorDataFromCSV("fixtures/algorithm/nn_raw_wave_count.csv");
        allSensorSampleList.add(Sensor.LIGHT, light);
        allSensorSampleList.add(Sensor.SOUND_PEAK_DISTURBANCE, peakDisturbance);
        allSensorSampleList.add(Sensor.SOUND_NUM_DISTURBANCES, numDisturbances);
        allSensorSampleList.add(Sensor.WAVE_COUNT, waveCount);

        final DateTime date = DateTime.parse("2016-05-20").withZone(DateTimeZone.UTC);
        final int tzOffsetMillis = -25200000;
        final double age = 0.322130898021 * 90;
        final int female = 0;
        final int male = 1;
        final int bmi = 0;
        final int partner = 0;
        final DateTime startTimeLocalUTC = date.withHourOfDay(20);
        final DateTime endTimeLocalUTC = date.plusDays(1).withHourOfDay(12);
        final DateTime currentTimeUTC = date.plusDays(1).withHourOfDay(13);

        return new OneDaysSensorData(allSensorSampleList,
                trackerMotions,partnerTrackerMotions , feedback, trackerMotions,partnerTrackerMotions ,
                date,startTimeLocalUTC,endTimeLocalUTC,currentTimeUTC,
                tzOffsetMillis, age, male, female, bmi, partner);

    }

    @Test
    public void testGetSensorData() throws Exception{
        final OneDaysSensorData oneDaysSensorData = getOneDaySensorData();
        final double [][] x = getSensorData(oneDaysSensorData);
        int i = 2;

    }

    @Test
    public void testGetPredictionWithPastTime() throws Exception {
        DateTime date = DateTimeUtil.ymdStringToDateTime("2015-09-01");
        DateTime startTime = date.withHourOfDay(18);
        DateTime endTime = startTime.plusHours(16);
        DateTime currentTime = endTime.plusHours(1);


        AllSensorSampleList senseData = OnlineHmmTest.getTypicalDayOfSense(startTime, endTime, 0);
        ImmutableList<TrackerMotion> pillData = OnlineHmmTest.getTypicalDayOfPill(startTime.minusHours(4), endTime.plusHours(4), 0);
        final ImmutableList<TimelineFeedback> emptyFeedback = ImmutableList.copyOf(Lists.<TimelineFeedback>newArrayList());
        final OneDaysSensorData oneDaysSensorData = new OneDaysSensorData(senseData, pillData, pillData, emptyFeedback, pillData, pillData, date, startTime, endTime, currentTime, DateTimeConstants.MILLIS_PER_HOUR);

        final Optional<TimelineAlgorithmResult> resultOptional = getTimelinePrediction(oneDaysSensorData, new TimelineLog(0L, 0L), 0L, false,Sets.<String>newHashSet());

        TestCase.assertTrue(resultOptional.isPresent());

        final TimelineAlgorithmResult result = resultOptional.get();
        TestCase.assertTrue(result.mainEvents.size() == 4);

        TestCase.assertEquals(endTime.getMillis(), resultOptional.get().mainEvents.get(Event.Type.WAKE_UP).getStartTimestamp(), 5 * DateTimeConstants.MILLIS_PER_MINUTE);

    }

    @Test
    public void testGetPredictionWithAtTime() throws Exception {
        DateTime date = DateTimeUtil.ymdStringToDateTime("2015-09-01");
        DateTime startTime = date.withHourOfDay(18);
        DateTime endTime = startTime.plusHours(16);
        DateTime currentTime = endTime;


        AllSensorSampleList senseData = OnlineHmmTest.getTypicalDayOfSense(startTime,endTime,0);
        ImmutableList<TrackerMotion> pillData = OnlineHmmTest.getTypicalDayOfPill(startTime.minusHours(4),endTime.plusHours(4),0);
        final ImmutableList<TimelineFeedback> emptyFeedback = ImmutableList.copyOf(Lists.<TimelineFeedback>newArrayList());
        final OneDaysSensorData oneDaysSensorData = new OneDaysSensorData(senseData,pillData,pillData,emptyFeedback,pillData,pillData,date,startTime,endTime,currentTime, DateTimeConstants.MILLIS_PER_HOUR);

        final Optional<TimelineAlgorithmResult> resultOptional = getTimelinePrediction(oneDaysSensorData,new TimelineLog(0L,0L),0L,false,Sets.<String>newHashSet());

        TestCase.assertTrue(resultOptional.isPresent());

        final TimelineAlgorithmResult result = resultOptional.get();
        TestCase.assertTrue(result.mainEvents.size() == 4);

        TestCase.assertEquals(currentTime.getMillis(),resultOptional.get().mainEvents.get(Event.Type.WAKE_UP).getStartTimestamp(),5*DateTimeConstants.MILLIS_PER_MINUTE);

    }

    @Test
    public void testGetPredictionWithShortenedTime() throws Exception {
        DateTime date = DateTimeUtil.ymdStringToDateTime("2015-09-01");
        DateTime startTime = date.withHourOfDay(18);
        DateTime endTime = startTime.plusHours(16);
        DateTime currentTime = startTime.plusHours(13);


        AllSensorSampleList senseData = OnlineHmmTest.getTypicalDayOfSense(startTime,endTime,0);
        ImmutableList<TrackerMotion> pillData = OnlineHmmTest.getTypicalDayOfPill(startTime.minusHours(4),endTime.plusHours(4),0);
        final ImmutableList<TimelineFeedback> emptyFeedback = ImmutableList.copyOf(Lists.<TimelineFeedback>newArrayList());
        final OneDaysSensorData oneDaysSensorData = new OneDaysSensorData(senseData,pillData,pillData,emptyFeedback,pillData,pillData,date,startTime,endTime,currentTime, DateTimeConstants.MILLIS_PER_HOUR);

        final Optional<TimelineAlgorithmResult> resultOptional = getTimelinePrediction(oneDaysSensorData,new TimelineLog(0L,0L),0L,false, Sets.<String>newHashSet());

        TestCase.assertTrue(resultOptional.isPresent());

        final TimelineAlgorithmResult result = resultOptional.get();
        TestCase.assertTrue(result.mainEvents.size() == 4);

        TestCase.assertEquals(currentTime.getMillis(),resultOptional.get().mainEvents.get(Event.Type.WAKE_UP).getStartTimestamp(),5*DateTimeConstants.MILLIS_PER_MINUTE);

    }


    @Test
    public void testDurationSafeguard() throws Exception {
        DateTime date = DateTimeUtil.ymdStringToDateTime("2015-09-01");
        DateTime startTime = date.withHourOfDay(18);
        DateTime endTime = startTime.plusHours(16);
        DateTime currentTime = startTime.plusHours(2).plusMillis(30);


        AllSensorSampleList senseData = OnlineHmmTest.getTypicalDayOfSense(startTime,endTime,0);
        ImmutableList<TrackerMotion> pillData = OnlineHmmTest.getTypicalDayOfPill(startTime.minusHours(4),endTime.plusHours(4),0);
        final ImmutableList<TimelineFeedback> emptyFeedback = ImmutableList.copyOf(Lists.<TimelineFeedback>newArrayList());
        final OneDaysSensorData oneDaysSensorData = new OneDaysSensorData(senseData,pillData,pillData,emptyFeedback,pillData,pillData,date,startTime,endTime,currentTime, DateTimeConstants.MILLIS_PER_HOUR);

        final TimelineLog log = new TimelineLog(0L,0L);
        final Optional<TimelineAlgorithmResult> resultOptional = getTimelinePrediction(oneDaysSensorData,log,0L,false, Sets.<String>newHashSet());

        TestCase.assertFalse(resultOptional.isPresent());

    }
}
