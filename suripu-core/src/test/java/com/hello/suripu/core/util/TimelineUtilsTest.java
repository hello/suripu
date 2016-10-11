package com.hello.suripu.core.util;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.MotionEvent;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.translations.English;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
    private List<TrackerMotion> trackerMotionList(String fixturePath) {
        final URL fixtureCSVFile = Resources.getResource(fixturePath);
        final List<TrackerMotion> trackerMotions = new ArrayList<>();
        try {
            final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 1; i < lines.length; i++){
                final String[] columns = lines[i].split(",");
                final TrackerMotion trackerMotion = new TrackerMotion(
                        Long.parseLong(columns[0].trim()), // id
                        Long.parseLong(columns[1].trim()), // account_id
                        Long.parseLong(columns[2].trim()), // tracker_id
                        DateTime.parse(columns[4].trim(), DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT)).getMillis(), // ts utc
                        Integer.valueOf(columns[3].trim()), // svm_no_gravity
                        Integer.valueOf(columns[5].trim()), // tz offset
                        // skipping local_utc
                        Long.parseLong(columns[7].trim()),
                        Long.parseLong(columns[8].trim()),
                        Long.parseLong(columns[9].trim())
                );
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
    public void testComputeStats(){
        //false positive night - motion at start and end of night
        final List<TrackerMotion> trackerMotions = trackerMotionList("fixtures/tracker_motion/2015-05-08.csv");
        final List<MotionEvent> motionEvents = timelineUtils.generateMotionEvents(trackerMotions);
        final long sleepTime = trackerMotions.get(0).timestamp;
        final long wakeTime =  trackerMotions.get(0).timestamp + 72000000L;

        final Optional<Event> sleep=Optional.of( Event.createFromType(Event.Type.SLEEP,sleepTime,sleepTime,-25200000, Optional.of(English.IN_BED_MESSAGE), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent()));
        final Optional<Event> wake= Optional.of(Event.createFromType(Event.Type.WAKE_UP,wakeTime,wakeTime, -25200000, Optional.of(English.OUT_OF_BED_MESSAGE), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent()));

        final List<Event> mainEvents = new ArrayList<Event>();

        mainEvents.add(sleep.get());
        mainEvents.add(wake.get());


        final Map<Long, Event> timelineEvents = TimelineRefactored.populateTimeline(motionEvents);
        for (final Event event : mainEvents) {
            timelineEvents.put(event.getStartTimestamp(), event);
        }
        final List<Event> eventsWithSleepEvents = TimelineRefactored.mergeEvents(timelineEvents);
        final List<Event> smoothedEvents = timelineUtils.smoothEvents(eventsWithSleepEvents);

        List<Event> cleanedUpEvents;
        cleanedUpEvents = timelineUtils.removeMotionEventsOutsideBedPeriod(smoothedEvents, sleep, wake);
        final List<Event> greyEvents = timelineUtils.greyNullEventsOutsideBedPeriod(cleanedUpEvents, sleep, wake, true);
        final List<Event> nonSignificantFilteredEvents = timelineUtils.removeEventBeforeSignificant(greyEvents);
        final List<SleepSegment> sleepSegments = timelineUtils.eventsToSegments(nonSignificantFilteredEvents);
        final SleepStats sleepStats = TimelineUtils.computeStats(sleepSegments, trackerMotions,70, true);
        final Integer uninterruptedDuration = 1110;
        assertThat(sleepStats.uninterruptedSleepDurationInMinutes, is(uninterruptedDuration));
    }

}