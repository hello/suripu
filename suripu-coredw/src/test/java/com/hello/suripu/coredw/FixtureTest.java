package com.hello.suripu.coredw;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.TrackerMotion;
import com.yammer.dropwizard.json.GuavaExtrasModule;
import org.junit.Before;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 3/24/15.
 * I listen to you Tim, CSV library will come later
 */
public class FixtureTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp(){
        this.mapper.registerModule(new GuavaModule());
        this.mapper.registerModule(new GuavaExtrasModule());
    }

    public List<TrackerMotion> loadTrackerMotionFromCSV(final String resource){
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


    public List<Event> loadLightEventsFromJSON(final String resource){
        final URL fixtureJSONFileValid = Resources.getResource(resource);
        final List<Event> lightOutTimes = new ArrayList<>();
        try {

            final String validJSONString = Resources.toString(fixtureJSONFileValid, Charsets.UTF_8);
            final List<Event> lightEventsValid = this.mapper.readValue(validJSONString, new TypeReference<List<Event>>() {});
            return lightEventsValid;
        } catch (IOException e) {
            e.printStackTrace();
            assertThat(true, is(false));
        }

        return lightOutTimes;
    }
}
