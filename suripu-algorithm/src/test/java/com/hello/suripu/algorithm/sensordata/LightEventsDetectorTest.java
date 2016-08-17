package com.hello.suripu.algorithm.sensordata;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.LightSegment;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
/**
 * Created by jarredheinrich on 8/9/16.
 */
public class LightEventsDetectorTest {

    private LinkedList<AmplitudeData> lightList(String fixturePath) {
        final URL fixtureCSVFile = Resources.getResource(fixturePath);
        final LinkedList<AmplitudeData> lightList = new LinkedList<>();
        try {
            final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 1; i < lines.length; i++){
                final String[] columns = lines[i].split(",");
                final AmplitudeData lightAmplitude = new AmplitudeData(
                        Long.parseLong(columns[0].trim()), // id
                        Double.parseDouble(columns[1].trim()), // account_id
                        Integer.parseInt(columns[2].trim())  // tracker_id
                );
                lightList.add(lightAmplitude);
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }

        return lightList;
    }

    @Test
    public void testValidLightsOut(){
        LinkedList<AmplitudeData> lightAmplitudeData = lightList("fixtures/light_amplitude_2016_08_07.csv");
        final double darknessThreshold = 1.1; // DVT unit ALS is very sensitive
        final int approxSunsetHour = 17;
        final int approxSunriseHour = 6;
        final int smoothingDegree = 5; // think of it as minutes
        final Optional<Long> testSleepTime = Optional.of(1470639360000L);

        final LightEventsDetector testDetector = new LightEventsDetector(approxSunriseHour, approxSunsetHour, darknessThreshold, smoothingDegree);
        final LinkedList<LightSegment> lightSegments = testDetector.process(lightAmplitudeData, testSleepTime);
        assertThat(lightSegments.size(), is(2));
        assertThat(lightSegments.get(0).segmentType, is(LightSegment.Type.LIGHTS_OUT));
    }

    @Test
    public void testRemoveLightsOutAfterCutOff(){
        LinkedList<AmplitudeData> lightAmplitudeData = lightList("fixtures/light_amplitude_2016_07_26.csv");
        final double darknessThreshold = 1.1; // DVT unit ALS is very sensitive
        final int approxSunsetHour = 17;
        final int approxSunriseHour = 6;
        final int smoothingDegree = 5; // think of it as minutes
        final Optional<Long> testSleepTime = Optional.of(1469503500000L);

        final LightEventsDetector testDetector = new LightEventsDetector(approxSunriseHour, approxSunsetHour, darknessThreshold, smoothingDegree);
        final LinkedList<LightSegment> lightSegments = testDetector.process(lightAmplitudeData, testSleepTime);
        assertThat(lightSegments.size(), is(2));
        assertThat(lightSegments.get(0).segmentType, is(LightSegment.Type.NONE));
        assertThat(lightSegments.get(1).segmentType, is(LightSegment.Type.NONE));
    }

    @Test
    public void testMissingSleepTime(){
        LinkedList<AmplitudeData> lightAmplitudeData = lightList("fixtures/light_amplitude_2016_07_26.csv");
        final double darknessThreshold = 1.1; // DVT unit ALS is very sensitive
        final int approxSunsetHour = 17;
        final int approxSunriseHour = 6;
        final int smoothingDegree = 5; // think of it as minutes

        final LightEventsDetector testDetector = new LightEventsDetector(approxSunriseHour, approxSunsetHour, darknessThreshold, smoothingDegree);
        final LinkedList<LightSegment> lightSegments = testDetector.process(lightAmplitudeData, Optional.<Long>absent());
        assertThat(lightSegments.size(), is(2));
        assertThat(lightSegments.get(1).segmentType, is(LightSegment.Type.LIGHTS_OUT));
    }

}
