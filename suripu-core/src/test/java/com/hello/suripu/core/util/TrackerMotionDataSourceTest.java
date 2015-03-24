package com.hello.suripu.core.util;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 11/20/14.
 */
public class TrackerMotionDataSourceTest {


    @Test
    public void testGetQueryBoundary(){
        final DateTime dayOfNightLocalUTC = new DateTime(2014, 11, 1, 0, 0, DateTimeZone.UTC);
        final Map.Entry<DateTime, DateTime> boundary = TrackerMotionDataSource.getStartEndQueryTimeLocalUTC(dayOfNightLocalUTC, 20, 16);
        assertThat(boundary.getKey(), is(new DateTime(2014, 11, 1, 20, 0, DateTimeZone.UTC)));
        assertThat(boundary.getValue(), is(new DateTime(2014, 11, 2, 16, 0, DateTimeZone.UTC)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetQueryBoundaryInvalidDayOfNight(){
        final DateTime dayOfNightLocalUTC = new DateTime(2014, 11, 1, 0, 0, DateTimeZone.forOffsetMillis(-255200));
        final Map.Entry<DateTime, DateTime> boundary = TrackerMotionDataSource.getStartEndQueryTimeLocalUTC(dayOfNightLocalUTC, 20, 16);

    }


    @Test
    public void testJavaCodeWorksTheSameAsPythonPrototype(){
        final URL fixtureDBCSVFile = Resources.getResource("fixtures/algorithm/raw_pang_motion_2014_12_11.csv");
        final List<TrackerMotion> dataFromDB = new LinkedList<>();
        try {
            final String csvString = Resources.toString(fixtureDBCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 1; i < lines.length; i++){
                final String[] columns = lines[i].split(",");
                dataFromDB.add(new TrackerMotion(i, 0L, 1L, Long.valueOf(columns[0]), Integer.valueOf(columns[1]), Integer.valueOf(columns[2]),0L, 0L,0L));
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }


        final URL fixtureDataFromDataSourceCSVFile = Resources.getResource("fixtures/algorithm/pang_motion_2014_12_11_gap_filled.csv");
        final List<AmplitudeData> expected = new LinkedList<>();
        try {
            final String csvString = Resources.toString(fixtureDataFromDataSourceCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 1; i < lines.length; i++){
                final String[] columns = lines[i].split(",");
                expected.add(new AmplitudeData(Long.valueOf(columns[0]), Integer.valueOf(columns[1]), Integer.valueOf(columns[2])));
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }

        final TrackerMotionDataSource dataSource = new TrackerMotionDataSource(dataFromDB);
        final List<AmplitudeData> actual = dataSource.getDataForDate(new DateTime(2014, 12, 11, 0, 0, DateTimeZone.UTC));
        assertThat(expected.size(), is(actual.size()));

        for(int i = 0; i < actual.size(); i++){
            final AmplitudeData expectedItem = expected.get(i);
            final AmplitudeData actualItem = actual.get(i);
            assertThat(expectedItem.timestamp, is(actualItem.timestamp));
            assertThat(expectedItem.amplitude, is(actualItem.amplitude));
            assertThat(expectedItem.offsetMillis, is(actualItem.offsetMillis));
        }
    }
}
