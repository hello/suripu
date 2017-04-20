package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.UserBioInfo;
import com.hello.suripu.core.util.CSVLoader;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

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
                final double [][] y = getOutput(9,1201,"fixtures/neuralNet/neuralNetFourEventOutput.csv");
                return Optional.of(new NeuralNetAlgorithmOutput(y));
            }
        });
    }

    private static double [][] getOutput(final int nCol, final int nRow, final String fileName){

        final double[][] y = new double[nCol][nRow];

        final URL fixtureCSVFile = Resources.getResource(fileName);
        try{
            final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 0; i < nRow; i++) {
                final String[] columns = lines[i].split(",");
                for (int colIndex = 0; colIndex < nCol; colIndex++) {
                    y[colIndex][i] = Float.parseFloat(columns[colIndex + 1].trim());
                }
            }
        }catch (IOException ex){
        }
        return y;
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
        final UserBioInfo userBioInfo = new UserBioInfo(age, bmi, male, female, partner);
        final DateTime startTimeLocalUTC = date.withHourOfDay(20);
        final DateTime endTimeLocalUTC = date.plusDays(1).withHourOfDay(12);
        final DateTime currentTimeUTC = date.plusDays(1).withHourOfDay(13);

        final OneDaysTrackerMotion oneDaysTrackerMotion = new OneDaysTrackerMotion(trackerMotions);
        final OneDaysTrackerMotion oneDaysPartnerMotion = new OneDaysTrackerMotion(partnerTrackerMotions);

        return new OneDaysSensorData(allSensorSampleList,
                oneDaysTrackerMotion, oneDaysPartnerMotion, feedback,
                date,startTimeLocalUTC,endTimeLocalUTC,currentTimeUTC,
                tzOffsetMillis, userBioInfo);

    }

    @Test
    public void testGetEventTimesFromNNOutput() throws Exception {
        final DateTime date = DateTime.parse("2016-05-20").withZone(DateTimeZone.UTC);
        final OneDaysSensorData oneDaysSensorData = getOneDaySensorData();
        final double[][] x = getSensorData(oneDaysSensorData, SleepPeriod.night(date));
        final double[][] output = getOutput(9,1201,"fixtures/neuralNet/neuralNetFourEventOutput.csv");
        final DateTime startTimeLocalUTC = date.withHourOfDay(20);


        final List<Sample> light = oneDaysSensorData.allSensorSampleList.get(Sensor.LIGHT);
        final TreeMap<Long, Integer> offsetMap = Maps.newTreeMap();

        final double[][] xPartial = new double[5][1201];
        xPartial[PartialSensorIndices.MY_MOTION_DURATION.index()] = Arrays.copyOfRange(x[SensorIndices.MY_MOTION_DURATION.index()], 0, 1201);
        xPartial[PartialSensorIndices.MY_MOTION_MAX_AMPLITUDE.index()] = Arrays.copyOfRange(x[SensorIndices.MY_MOTION_MAX_AMPLITUDE.index()], 0, 1201);
        xPartial[PartialSensorIndices.DIFFLIGHTSMOOTHED.index()] = Arrays.copyOfRange(x[SensorIndices.DIFFLIGHTSMOOTHED.index()], 0, 1201);
        xPartial[PartialSensorIndices.SOUND_VOLUME.index()] = Arrays.copyOfRange(x[SensorIndices.SOUND_VOLUME.index()], 0, 1201);
        xPartial[PartialSensorIndices.WAVES.index()] = Arrays.copyOfRange(x[SensorIndices.WAVES.index()], 0, 1201);


        final Integer [] sleepSegments = getSleepSegments(0L, output, 916);

        for (final Sample s : light) {
            offsetMap.put(s.dateTime, s.offsetMillis);
        }


        final List<Event> eventTimes = getEventTimes(offsetMap, startTimeLocalUTC.getMillis(), SleepPeriod.night(date), sleepSegments, xPartial);
        assert(eventTimes.size() == 4);
        assert(eventTimes.get(0).getStartTimestamp() == 1463786220000L);
        assert(eventTimes.get(1).getStartTimestamp() == 1463787660000L);
        assert(eventTimes.get(2).getStartTimestamp() == 1463810820000L);
        assert(eventTimes.get(3).getStartTimestamp() == 1463813040000L);
    }

}
