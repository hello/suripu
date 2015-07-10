package com.hello.suripu.coredw.util;

import com.hello.suripu.coredw.FixtureTest;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.MultiLightOutUtils;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 3/11/15.
 */
public class MultiLightOutUtilsTest extends FixtureTest {


    @Test
    public void testSmoothLightWithMultiSegments(){
        final List<Event> lightEvents = loadLightEventsFromJSON("fixtures/algorithm/ksg_light_event_2015_03_06.orig.json");
        final List<Event> lightEventsSmoothed = loadLightEventsFromJSON("fixtures/algorithm/ksg_light_event_2015_03_06.smooth.json");

        final List<Event> actual = MultiLightOutUtils.smoothLight(lightEvents, MultiLightOutUtils.DEFAULT_SMOOTH_GAP_MIN);
        assertThat(actual.size(), is(lightEventsSmoothed.size()));

        for(int i = 0; i < actual.size(); i++){
            assertThat(lightEventsSmoothed.get(i).getType(), is(actual.get(i).getType()));
            assertThat(lightEventsSmoothed.get(i).getStartTimestamp(), is(actual.get(i).getStartTimestamp()));
            assertThat(lightEventsSmoothed.get(i).getEndTimestamp(), is(actual.get(i).getEndTimestamp()));
            assertThat(lightEventsSmoothed.get(i).getDescription(), is(actual.get(i).getDescription()));
        }
    }


    @Test
    public void testValidLightWithMultiSegments(){
        final List<TrackerMotion> trackerMotions = loadTrackerMotionFromCSV("fixtures/algorithm/ksg_motion_2015_03_06_raw.csv");

        final List<Event> lightEvents = loadLightEventsFromJSON("fixtures/algorithm/ksg_light_event_2015_03_06.smooth.json");
        final List<Event> lightEventsValid = loadLightEventsFromJSON("fixtures/algorithm/ksg_light_event_2015_03_06.valid.json");

        final List<Event> actual = MultiLightOutUtils.getValidLightOuts(lightEvents, trackerMotions, MultiLightOutUtils.DEFAULT_LIGHT_DELTA_WINDOW_MIN);
        assertThat(actual.size(), is(lightEventsValid.size()));
        for(int i = 0; i < actual.size(); i++){
            assertThat(lightEventsValid.get(i).getType(), is(actual.get(i).getType()));
            assertThat(lightEventsValid.get(i).getStartTimestamp(), is(actual.get(i).getStartTimestamp()));
            assertThat(lightEventsValid.get(i).getEndTimestamp(), is(actual.get(i).getEndTimestamp()));
            assertThat(lightEventsValid.get(i).getDescription(), is(actual.get(i).getDescription()));
        }

    }



    @Test
    public void testSmoothLightWithTwoSegments(){
        final List<Event> lightEvents = loadLightEventsFromJSON("fixtures/algorithm/ryan_light_event_2015_03_09.orig.json");
        final List<Event> lightEventsSmoothed = loadLightEventsFromJSON("fixtures/algorithm/ryan_light_event_2015_03_09.smooth.json");

        final List<Event> actual = MultiLightOutUtils.smoothLight(lightEvents, MultiLightOutUtils.DEFAULT_SMOOTH_GAP_MIN);
        assertThat(actual.size(), is(lightEventsSmoothed.size()));

        for(int i = 0; i < actual.size(); i++){
            assertThat(lightEventsSmoothed.get(i).getType(), is(actual.get(i).getType()));
            assertThat(lightEventsSmoothed.get(i).getStartTimestamp(), is(actual.get(i).getStartTimestamp()));
            assertThat(lightEventsSmoothed.get(i).getEndTimestamp(), is(actual.get(i).getEndTimestamp()));
            assertThat(lightEventsSmoothed.get(i).getDescription(), is(actual.get(i).getDescription()));
        }
    }


    @Test
    public void testValidLightWithTwoSegments(){
        final List<TrackerMotion> trackerMotions = loadTrackerMotionFromCSV("fixtures/algorithm/ryan_motion_2015_03_09_raw.csv");

        final List<Event> lightEvents = loadLightEventsFromJSON("fixtures/algorithm/ryan_light_event_2015_03_09.smooth.json");
        final List<Event> lightEventsValid = loadLightEventsFromJSON("fixtures/algorithm/ryan_light_event_2015_03_09.valid.json");

        final List<Event> actual = MultiLightOutUtils.getValidLightOuts(lightEvents, trackerMotions, MultiLightOutUtils.DEFAULT_LIGHT_DELTA_WINDOW_MIN);
        assertThat(actual.size(), is(lightEventsValid.size()));
        for(int i = 0; i < actual.size(); i++){
            assertThat(lightEventsValid.get(i).getType(), is(actual.get(i).getType()));
            assertThat(lightEventsValid.get(i).getStartTimestamp(), is(actual.get(i).getStartTimestamp()));
            assertThat(lightEventsValid.get(i).getEndTimestamp(), is(actual.get(i).getEndTimestamp()));
            assertThat(lightEventsValid.get(i).getDescription(), is(actual.get(i).getDescription()));
        }
    }


    @Test
    public void testValidLightWithTwoSegmentsBeforeMidLight(){
        final List<TrackerMotion> trackerMotions = loadTrackerMotionFromCSV("fixtures/algorithm/turf_motion_2015_03_11_raw.csv");

        final List<Event> lightEvents = loadLightEventsFromJSON("fixtures/algorithm/turf_light_event_2015_03_11.orig.json");
        final List<Event> lightEventsSmoothed = loadLightEventsFromJSON("fixtures/algorithm/turf_light_event_2015_03_11.smooth.json");
        final List<Event> lightEventsValid = loadLightEventsFromJSON("fixtures/algorithm/turf_light_event_2015_03_11.valid.json");

        final List<Event> smoothLight = MultiLightOutUtils.smoothLight(lightEvents, MultiLightOutUtils.DEFAULT_SMOOTH_GAP_MIN);

        assertThat(smoothLight.size(), is(lightEventsSmoothed.size()));
        for(int i = 0; i < smoothLight.size(); i++){
            assertThat(lightEventsSmoothed.get(i).getType(), is(smoothLight.get(i).getType()));
            assertThat(lightEventsSmoothed.get(i).getStartTimestamp(), is(smoothLight.get(i).getStartTimestamp()));
            assertThat(lightEventsSmoothed.get(i).getEndTimestamp(), is(smoothLight.get(i).getEndTimestamp()));
            assertThat(lightEventsSmoothed.get(i).getDescription(), is(smoothLight.get(i).getDescription()));
        }

        final List<Event> actual = MultiLightOutUtils.getValidLightOuts(smoothLight, trackerMotions,MultiLightOutUtils.DEFAULT_LIGHT_DELTA_WINDOW_MIN);

        assertThat(actual.size(), is(lightEventsValid.size()));

        for(int i = 0; i < actual.size(); i++){
            assertThat(lightEventsValid.get(i).getType(), is(actual.get(i).getType()));
            assertThat(lightEventsValid.get(i).getStartTimestamp(), is(actual.get(i).getStartTimestamp()));
            assertThat(lightEventsValid.get(i).getEndTimestamp(), is(actual.get(i).getEndTimestamp()));
            assertThat(lightEventsValid.get(i).getDescription(), is(actual.get(i).getDescription()));
        }
    }
}
