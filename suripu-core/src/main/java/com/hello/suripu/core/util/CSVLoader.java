package com.hello.suripu.core.util;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.TrackerMotion;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jarredheinrich on 11/3/16.
 */
public class CSVLoader {

    public static List<TrackerMotion> loadTrackerMotionFromCSV(final String resource){
    final URL fixtureCSVFile = Resources.getResource(resource);
    final List<TrackerMotion> trackerMotions = new ArrayList<>();
    try {
        final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
        final String[] lines = csvString.split("\\n");
        for(int i = 1; i < lines.length; i++){
            final String[] columns = lines[i].split(",");
            final TrackerMotion trackerMotion;
            if (columns.length >=11) {
                final TrackerMotion.Builder trackerMotionBuilder = new TrackerMotion.Builder();
                trackerMotion = trackerMotionBuilder.withAccountId(Long.parseLong(columns[1].trim()))
                        .withExternalTrackerId("0")
                        .withTimestampMillis(Long.parseLong(columns[3].trim())) //timestamp
                        .withValue(Integer.parseInt(columns[4].trim())) //value
                        .withOffsetMillis(Integer.parseInt(columns[5].trim())) // offset
                        .withMotionRange(Long.parseLong(columns[6].trim())) //motion_range
                        .withKickOffCounts(Long.parseLong(columns[7].trim())) //kickoff count
                        .withOnDurationInSeconds(Long.parseLong(columns[8].trim())) //on duration
                        .withMotionMask(Long.parseLong(columns[9].trim()))
                        .withCosTheta(Long.parseLong(columns[10].trim()))
                        .build();
            }else{
                trackerMotion = new TrackerMotion(
                        Long.parseLong(columns[0].trim()), //id
                        Long.parseLong(columns[1].trim()), //account id
                        0L, // tracker_id
                        Long.parseLong(columns[3].trim()), //timestamp
                        Integer.parseInt(columns[4].trim()), //value
                        Integer.parseInt(columns[5].trim()), // offset
                        Long.parseLong(columns[6].trim()), //motion_range
                        Long.parseLong(columns[7].trim()), //kickoff count
                        Long.parseLong(columns[8].trim()) //on duration
                );

            }
            trackerMotions.add(trackerMotion);
            //}
        }
    }catch (IOException ex){
        ex.printStackTrace();
    }

    return trackerMotions;
}
    public static List<Sample> loadSensorDataFromCSV(final String resource) {
        final URL fixtureCSVFile = Resources.getResource(resource);
        final List<Sample> sampleList = new ArrayList<>();
        try {
            final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 1; i < lines.length; i++){
                final String[] columns = lines[i].split(",");
                final Sample sensor = new Sample(
                        Long.parseLong(columns[1].trim()), // long datetime
                        Float.parseFloat(columns[2].trim()), // sensor value
                        Integer.parseInt(columns[3].trim())  // Offset
                );
                sampleList.add(sensor);
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }

        return sampleList;
    }
}
