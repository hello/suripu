package com.hello.suripu.core.util;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.TrackerMotion;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by jarredheinrich on 10/7/16.
 */

public class TimelineUtilsTest {

    final private TimelineUtils timelineUtils = new TimelineUtils();

    private List<TrackerMotion> loadTrackerMotionFromCSV(final String resource){
        final URL fixtureCSVFile = Resources.getResource(resource);
        final List<TrackerMotion> trackerMotions = new ArrayList<>();
        try {
            final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 1; i < lines.length; i++){
                final String[] columns = lines[i].split(",");
                final TrackerMotion trackerMotion = new TrackerMotion(0L, 0L, 0L, Long.valueOf(columns[0]), Integer.valueOf(columns[1]), Integer.valueOf(columns[2]), 0L, 0L,0L);
                //if(trackerMotion.value > 0){
                trackerMotions.add(trackerMotion);
                //}
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }

        return trackerMotions;
    }


    @Test
    public void testMotionDuringSleepCheck(){
        //false positive night - motion at start and end of night
        List<TrackerMotion> trackerMotions = loadTrackerMotionFromCSV("fixtures/algorithm/false_night_2016_08_01.csv");
        long fallAsleepTime = 1470114360000L;
        long wakeUpTime = 1470139440000L;
        boolean testMotionDuringSleep = timelineUtils.motionDuringSleepCheck(trackerMotions, fallAsleepTime, wakeUpTime);
        assertThat(testMotionDuringSleep, is(false));
        //okay night
        trackerMotions = loadTrackerMotionFromCSV("fixtures/algorithm/millionaires_challenge_2015_02_20_raw.csv");
        fallAsleepTime = 1424496240000L;
        wakeUpTime = 1424528700000L;
        testMotionDuringSleep = timelineUtils.motionDuringSleepCheck(trackerMotions, fallAsleepTime, wakeUpTime);
        assertThat(testMotionDuringSleep, is(true));

    }

}