package com.hello.suripu.core.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.TrackerMotion;
import com.yammer.dropwizard.json.GuavaExtrasModule;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 3/11/15.
 */
public class MultiLightOutUtilsTest {

    private final ObjectMapper mapper = new ObjectMapper();


    @Before
    public void setUp(){
        this.mapper.registerModule(new GuavaModule());
        this.mapper.registerModule(new GuavaExtrasModule());
    }

    @Test
    public void testSmoothLightWithMultiSegments(){
        final URL fixtureJSONFile = Resources.getResource("fixtures/algorithm/ksg_light_event_2015_03_06.orig.json");
        final URL fixtureJSONFileSmooth = Resources.getResource("fixtures/algorithm/ksg_light_event_2015_03_06.smooth.json");
        try {
            final String jsonString = Resources.toString(fixtureJSONFile, Charsets.UTF_8);
            final List<Event> lightEvents = this.mapper.readValue(jsonString, new TypeReference<List<Event>>() {});

            final String smoothJSONString = Resources.toString(fixtureJSONFileSmooth, Charsets.UTF_8);
            final List<Event> lightEventsSmoothed = this.mapper.readValue(smoothJSONString, new TypeReference<List<Event>>() {});

            final List<Event> actual = MultiLightOutUtils.smoothLight(lightEvents, MultiLightOutUtils.DEFAULT_SMOOTH_GAP_MIN);
            assertThat(actual.size(), is(lightEventsSmoothed.size()));

            for(int i = 0; i < actual.size(); i++){
                assertThat(lightEventsSmoothed.get(i).getType(), is(actual.get(i).getType()));
                assertThat(lightEventsSmoothed.get(i).getStartTimestamp(), is(actual.get(i).getStartTimestamp()));
                assertThat(lightEventsSmoothed.get(i).getEndTimestamp(), is(actual.get(i).getEndTimestamp()));
                assertThat(lightEventsSmoothed.get(i).getDescription(), is(actual.get(i).getDescription()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            assertThat(true, is(false));
        }
    }


    @Test
    public void testValidLightWithMultiSegments(){
        final URL fixtureCSVFile = Resources.getResource("fixtures/algorithm/ksg_motion_2015_03_06_raw.csv");
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
            assertThat(true, is(false));
        }

        final URL fixtureJSONFile = Resources.getResource("fixtures/algorithm/ksg_light_event_2015_03_06.smooth.json");
        final URL fixtureJSONFileValid = Resources.getResource("fixtures/algorithm/ksg_light_event_2015_03_06.valid.json");
        try {
            final String jsonString = Resources.toString(fixtureJSONFile, Charsets.UTF_8);
            final List<Event> lightEvents = this.mapper.readValue(jsonString, new TypeReference<List<Event>>() {});

            final String validJSONString = Resources.toString(fixtureJSONFileValid, Charsets.UTF_8);
            final List<Event> lightEventsValid = this.mapper.readValue(validJSONString, new TypeReference<List<Event>>() {});

            final List<Event> actual = MultiLightOutUtils.getValidLightOuts(lightEvents, trackerMotions, MultiLightOutUtils.DEFAULT_LIGHT_DELTA_WINDOW_MIN);
            assertThat(actual.size(), is(lightEventsValid.size()));
            for(int i = 0; i < actual.size(); i++){
                assertThat(lightEventsValid.get(i).getType(), is(actual.get(i).getType()));
                assertThat(lightEventsValid.get(i).getStartTimestamp(), is(actual.get(i).getStartTimestamp()));
                assertThat(lightEventsValid.get(i).getEndTimestamp(), is(actual.get(i).getEndTimestamp()));
                assertThat(lightEventsValid.get(i).getDescription(), is(actual.get(i).getDescription()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            assertThat(true, is(false));
        }
    }



    @Test
    public void testSmoothLightWithTwoSegments(){
        final URL fixtureJSONFile = Resources.getResource("fixtures/algorithm/ryan_light_event_2015_03_09.orig.json");
        final URL fixtureJSONFileSmooth = Resources.getResource("fixtures/algorithm/ryan_light_event_2015_03_09.smooth.json");
        try {
            final String jsonString = Resources.toString(fixtureJSONFile, Charsets.UTF_8);
            final List<Event> lightEvents = this.mapper.readValue(jsonString, new TypeReference<List<Event>>() {});

            final String smoothJSONString = Resources.toString(fixtureJSONFileSmooth, Charsets.UTF_8);
            final List<Event> lightEventsSmoothed = this.mapper.readValue(smoothJSONString, new TypeReference<List<Event>>() {});

            final List<Event> actual = MultiLightOutUtils.smoothLight(lightEvents, MultiLightOutUtils.DEFAULT_SMOOTH_GAP_MIN);
            assertThat(actual.size(), is(lightEventsSmoothed.size()));

            for(int i = 0; i < actual.size(); i++){
                assertThat(lightEventsSmoothed.get(i).getType(), is(actual.get(i).getType()));
                assertThat(lightEventsSmoothed.get(i).getStartTimestamp(), is(actual.get(i).getStartTimestamp()));
                assertThat(lightEventsSmoothed.get(i).getEndTimestamp(), is(actual.get(i).getEndTimestamp()));
                assertThat(lightEventsSmoothed.get(i).getDescription(), is(actual.get(i).getDescription()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            assertThat(true, is(false));
        }
    }


    @Test
    public void testValidLightWithTwoSegments(){
        final URL fixtureCSVFile = Resources.getResource("fixtures/algorithm/ryan_motion_2015_03_09_raw.csv");
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
            assertThat(true, is(false));
        }

        final URL fixtureJSONFile = Resources.getResource("fixtures/algorithm/ryan_light_event_2015_03_09.smooth.json");
        final URL fixtureJSONFileValid = Resources.getResource("fixtures/algorithm/ryan_light_event_2015_03_09.valid.json");
        try {
            final String jsonString = Resources.toString(fixtureJSONFile, Charsets.UTF_8);
            final List<Event> lightEvents = this.mapper.readValue(jsonString, new TypeReference<List<Event>>() {});

            final String validJSONString = Resources.toString(fixtureJSONFileValid, Charsets.UTF_8);
            final List<Event> lightEventsValid = this.mapper.readValue(validJSONString, new TypeReference<List<Event>>() {});

            final List<Event> actual = MultiLightOutUtils.getValidLightOuts(lightEvents, trackerMotions, MultiLightOutUtils.DEFAULT_LIGHT_DELTA_WINDOW_MIN);
            assertThat(actual.size(), is(lightEventsValid.size()));
            for(int i = 0; i < actual.size(); i++){
                assertThat(lightEventsValid.get(i).getType(), is(actual.get(i).getType()));
                assertThat(lightEventsValid.get(i).getStartTimestamp(), is(actual.get(i).getStartTimestamp()));
                assertThat(lightEventsValid.get(i).getEndTimestamp(), is(actual.get(i).getEndTimestamp()));
                assertThat(lightEventsValid.get(i).getDescription(), is(actual.get(i).getDescription()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            assertThat(true, is(false));
        }
    }


    @Test
    public void testValidLightWithTwoSegmentsBeforeMidLight(){
        final URL fixtureCSVFile = Resources.getResource("fixtures/algorithm/turf_motion_2015_03_11_raw.csv");
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
            assertThat(true, is(false));
        }

        final URL fixtureJSONFile = Resources.getResource("fixtures/algorithm/turf_light_event_2015_03_11.orig.json");
        final URL fixtureJSONFileSmoothed = Resources.getResource("fixtures/algorithm/turf_light_event_2015_03_11.smooth.json");
        final URL fixtureJSONFileValid = Resources.getResource("fixtures/algorithm/turf_light_event_2015_03_11.valid.json");
        try {
            final String jsonString = Resources.toString(fixtureJSONFile, Charsets.UTF_8);
            final List<Event> lightEvents = this.mapper.readValue(jsonString, new TypeReference<List<Event>>() {});

            final String smoothJSONString = Resources.toString(fixtureJSONFileSmoothed, Charsets.UTF_8);
            final List<Event> lightEventsSmoothed = this.mapper.readValue(smoothJSONString, new TypeReference<List<Event>>() {});

            final String validJSONString = Resources.toString(fixtureJSONFileValid, Charsets.UTF_8);
            final List<Event> lightEventsValid = this.mapper.readValue(validJSONString, new TypeReference<List<Event>>() {});

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
        } catch (IOException e) {
            e.printStackTrace();
            assertThat(true, is(false));
        }
    }
}
