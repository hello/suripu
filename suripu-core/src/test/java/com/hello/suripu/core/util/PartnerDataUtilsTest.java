package com.hello.suripu.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
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
public class PartnerDataUtilsTest {


    final private TimelineUtils timelineUtils = new TimelineUtils();
    private final ObjectMapper mapper = new ObjectMapper();

    private ImmutableList<TrackerMotion> loadTrackerMotionFromCSV(final String resource){
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

        return ImmutableList.copyOf(trackerMotions);
    }
//get user and partner tracker motion data

    @Test
    public void testPercentUniqueMotions(){
        ImmutableList<TrackerMotion> userMotions_a = loadTrackerMotionFromCSV("fixtures/algorithm/false_night_2016_08_01.csv");
        ImmutableList<TrackerMotion> partnerMotions_a = loadTrackerMotionFromCSV("fixtures/algorithm/false_night_2016_08_01.csv");
        float uniqueMovements = PartnerDataUtils.getPercentUniqueMovements(userMotions_a, partnerMotions_a);
        assertThat(uniqueMovements, is (0.0f));

        ImmutableList<TrackerMotion> userMotions_b = loadTrackerMotionFromCSV("fixtures/algorithm/false_night_2016_08_01_b.csv");
        ImmutableList<TrackerMotion> partnerMotions_b = loadTrackerMotionFromCSV("fixtures/algorithm/false_night_2016_08_01.csv");
        uniqueMovements = PartnerDataUtils.getPercentUniqueMovements(userMotions_b, partnerMotions_b);
        assertThat(uniqueMovements, is (0.4f));

    }
}
