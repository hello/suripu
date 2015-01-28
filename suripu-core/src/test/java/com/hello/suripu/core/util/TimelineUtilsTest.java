package com.hello.suripu.core.util;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.InBedEvent;
import com.hello.suripu.core.models.Events.MotionEvent;
import com.hello.suripu.core.models.Events.NullEvent;
import com.hello.suripu.core.models.Events.OutOfBedEvent;
import com.hello.suripu.core.models.Events.SleepEvent;
import com.hello.suripu.core.models.Events.WakeupEvent;
import com.hello.suripu.core.models.TrackerMotion;
import org.hamcrest.core.Is;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 12/18/14.
 */
public class TimelineUtilsTest {

    @Test
    public void testMerge() {
        final List<Event> sleepSegments = new ArrayList<>();
        final DateTime now = DateTime.now();
        sleepSegments.add(new SleepEvent(now.getMillis(), now.getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0));
        sleepSegments.add(new SleepEvent(now.plusMinutes(1).getMillis(), now.plusMinutes(1).getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0));
        sleepSegments.add(new SleepEvent(now.plusMinutes(2).getMillis(), now.plusMinutes(2).getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0));
        sleepSegments.add(new SleepEvent(now.plusMinutes(3).getMillis(), now.plusMinutes(3).getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0));

        final List<Event> mergedSegments = TimelineUtils.generateAlignedSegmentsByTypeWeight(sleepSegments,
                DateTimeConstants.MILLIS_PER_MINUTE,
                5, true);
        assertThat(mergedSegments.size(), Is.is(1));
    }


    @Test
    public void testMergeSegments() {

        final List<TrackerMotion> trackerMotions = new ArrayList<>();
        final DateTime now = DateTime.now();

        trackerMotions.add(new TrackerMotion(6L,99L,123L, now.getMillis(), 100, 0,0L, 0L, 0L));
        trackerMotions.add(new TrackerMotion(7L,99L,123L, now.plusMinutes(1).getMillis(), 50, 0,0L, 0L, 0L));
        trackerMotions.add(new TrackerMotion(8L,99L,123L, now.plusMinutes(2).getMillis(), 0, 0,0L, 0L, 0L));
        trackerMotions.add(new TrackerMotion(9L,99L,123L, now.plusMinutes(2).getMillis(), 100, 0,0L, 0L, 0L));

        final List<MotionEvent> motionEvents = TimelineUtils.generateMotionEvents(trackerMotions);
        final List<Event> segments = new LinkedList<>();
        segments.addAll(motionEvents);
        assertThat(segments.size(), Is.is(trackerMotions.size() - 1));

        final List<Event> mergedSegments = TimelineUtils.generateAlignedSegmentsByTypeWeight(segments,
                DateTimeConstants.MILLIS_PER_MINUTE,
                5,
                true);
        assertThat(mergedSegments.size(), Is.is(1));
    }


    @Test
    public void testMergeSomeSegments() {

        final List<TrackerMotion> trackerMotions = new ArrayList<>();
        final DateTime now = DateTime.now();

        trackerMotions.add(new TrackerMotion(6L,99L,123L, now.getMillis(), 100, 0,0L, 0L, 0L));
        trackerMotions.add(new TrackerMotion(7L,99L,123L, now.plusMinutes(1).getMillis(), 100, 0,0L, 0L, 0L));
        trackerMotions.add(new TrackerMotion(8L,99L,123L, now.plusMinutes(2).getMillis(), 0, 0,0L, 0L, 0L));
        trackerMotions.add(new TrackerMotion(9L,99L,123L, now.plusMinutes(2).getMillis(), 100, 0,0L, 0L, 0L));

        final List<MotionEvent> motionEvents = TimelineUtils.generateMotionEvents(trackerMotions);
        final List<Event> segments = new LinkedList<>();
        segments.addAll(motionEvents);
        final List<Event> mergedSegments = TimelineUtils.generateAlignedSegmentsByTypeWeight(segments,
                DateTimeConstants.MILLIS_PER_MINUTE,
                5, true);
        assertThat(mergedSegments.size(), Is.is(1));
    }



    @Test
    public void testEmptySleepSegments() {
        final List<Event> segments = new ArrayList<>();
        final List<Event> actual = TimelineUtils.generateAlignedSegmentsByTypeWeight(segments, DateTimeConstants.MILLIS_PER_MINUTE, 5, true);
        assertThat(actual.size(), Is.is(0));
    }


    @Test
    public void testMergeSlot() {
        final List<Event> sleepSegments = new ArrayList<>();
        final DateTime now = DateTime.now();
        //sleepSegments.add(new MotionEvent(millis, millis + DateTimeConstants.MILLIS_PER_MINUTE, 0, 99, 100));
        sleepSegments.add(new MotionEvent(now.getMillis(), now.getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0, 0));
        sleepSegments.add(new MotionEvent(now.plusMinutes(5).getMillis(), now.plusMinutes(5).getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0, 50));
        sleepSegments.add(new MotionEvent(now.plusMinutes(6).getMillis(), now.plusMinutes(6).getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0, 0));

        final List<Event> mergedSegments = TimelineUtils.generateAlignedSegmentsByTypeWeight(sleepSegments, DateTimeConstants.MILLIS_PER_MINUTE, 5, true);
        assertThat(mergedSegments.size(), Is.is(2));
    }



    public static String getFile(String resourceClassPathLocation) {
        try {
            return new File(Resources.getResource(resourceClassPathLocation)
                    .toURI()).getAbsolutePath();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }



    @Test
    public void testMergeByTimeBucket() {
        List<Event> sleepSegments = new ArrayList<>();
        final DateTime now = DateTime.now();
        final Long millis = now.getMillis();
        sleepSegments.add(new MotionEvent(millis, millis + DateTimeConstants.MILLIS_PER_MINUTE, 0, 99));
        sleepSegments.add(new MotionEvent(millis, millis + DateTimeConstants.MILLIS_PER_MINUTE, 0, 100));
        sleepSegments.add(new MotionEvent(millis, millis + DateTimeConstants.MILLIS_PER_MINUTE, 0, 71));
        sleepSegments.add(new MotionEvent(millis, millis + DateTimeConstants.MILLIS_PER_MINUTE, 0, 50));

        final List<Event> mergedSegments = TimelineUtils.generateAlignedSegmentsByTypeWeight(sleepSegments, DateTimeConstants.MILLIS_PER_MINUTE, 2, true);
        assertThat(mergedSegments.size(), Is.is(1));
    }


    @Test
    public void testGetFullSleepEventsWeekEndSleepLate(){
        final URL fixtureCSVFile = Resources.getResource("pang_motion_2015_01_17_raw.csv");
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

        final List<Event> sleepEvents = TimelineUtils.getSleepEvents(new DateTime(2015, 1, 17, 0, 0, DateTimeZone.UTC),
                trackerMotions,
                Optional.of(new DateTime(1421575200000L, DateTimeZone.UTC)),
                10);
        final SleepEvent sleepSegment = (SleepEvent) sleepEvents.get(1);
        final InBedEvent goToBedSegment = (InBedEvent) sleepEvents.get(0);
        final WakeupEvent wakeUpSegment = (WakeupEvent) sleepEvents.get(2);
        final OutOfBedEvent outOfBedSegment = (OutOfBedEvent) sleepEvents.get(3);

        // Out put from python script suripu_sum.py:
        /*
        in bed at 2014-12-03 01:22:00, prob: 1.11502650032, amp: 2967
        wake up at 2014-12-03 09:38:00, prob: 0.0631222110581, amp: 522
        */

        final DateTime goToBedTime = new DateTime(goToBedSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(goToBedSegment.getTimezoneOffset()));
        final DateTime sleepTime = new DateTime(sleepSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.getTimezoneOffset()));

        final DateTime wakeUpTime = new DateTime(wakeUpSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUpSegment.getTimezoneOffset()));
        final DateTime outOfBedTime = new DateTime(outOfBedSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBedSegment.getTimezoneOffset()));

        final DateTime goToBedLocalUTC = new DateTime(goToBedTime.getYear(), goToBedTime.getMonthOfYear(), goToBedTime.getDayOfMonth(), goToBedTime.getHourOfDay(), goToBedTime.getMinuteOfHour(), DateTimeZone.UTC);
        final DateTime sleepLocalUTC = new DateTime(sleepTime.getYear(), sleepTime.getMonthOfYear(), sleepTime.getDayOfMonth(), sleepTime.getHourOfDay(), sleepTime.getMinuteOfHour(), DateTimeZone.UTC);
        final DateTime wakeUpLocalUTC = new DateTime(wakeUpTime.getYear(), wakeUpTime.getMonthOfYear(), wakeUpTime.getDayOfMonth(), wakeUpTime.getHourOfDay(), wakeUpTime.getMinuteOfHour(), DateTimeZone.UTC);
        final DateTime outOfBedLocalUTC = new DateTime(outOfBedTime.getYear(), outOfBedTime.getMonthOfYear(), outOfBedTime.getDayOfMonth(), outOfBedTime.getHourOfDay(), outOfBedTime.getMinuteOfHour(), DateTimeZone.UTC);

        assertThat(goToBedLocalUTC, is(new DateTime(2015, 1, 18, 6, 11, DateTimeZone.UTC)));
        assertThat(sleepLocalUTC, is(new DateTime(2015, 1, 18, 6, 22, DateTimeZone.UTC)));
        assertThat(wakeUpLocalUTC, is(new DateTime(2015, 1, 18, 11, 41, DateTimeZone.UTC)));
        assertThat(outOfBedLocalUTC, is(new DateTime(2015, 1, 18, 11, 44, DateTimeZone.UTC)));
    }


    @Test
    public void testGetFullSleepEventsCannotFallAsleepAfterInBed(){
        final URL fixtureCSVFile = Resources.getResource("tim_motion_2015_01_04_raw.csv");
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

        final List<Event> sleepEvents = TimelineUtils.getSleepEvents(new DateTime(2015, 1, 04, 0, 0, DateTimeZone.UTC),
                trackerMotions,
                Optional.of(new DateTime(1420445760000L, DateTimeZone.UTC)),
                10);
        final SleepEvent sleepSegment = (SleepEvent) sleepEvents.get(1);
        final InBedEvent goToBedSegment = (InBedEvent) sleepEvents.get(0);
        final WakeupEvent wakeUpSegment = (WakeupEvent) sleepEvents.get(2);
        final OutOfBedEvent outOfBedSegment = (OutOfBedEvent) sleepEvents.get(3);

        // Out put from python script suripu_sum.py:
        /*
        in bed at 2014-12-03 01:22:00, prob: 1.11502650032, amp: 2967
        wake up at 2014-12-03 09:38:00, prob: 0.0631222110581, amp: 522
        */

        final DateTime goToBedTime = new DateTime(goToBedSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(goToBedSegment.getTimezoneOffset()));
        final DateTime sleepTime = new DateTime(sleepSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.getTimezoneOffset()));

        final DateTime wakeUpTime = new DateTime(wakeUpSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUpSegment.getTimezoneOffset()));
        final DateTime outOfBedTime = new DateTime(outOfBedSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBedSegment.getTimezoneOffset()));

        final DateTime goToBedLocalUTC = new DateTime(goToBedTime.getYear(), goToBedTime.getMonthOfYear(), goToBedTime.getDayOfMonth(), goToBedTime.getHourOfDay(), goToBedTime.getMinuteOfHour(), DateTimeZone.UTC);
        final DateTime sleepLocalUTC = new DateTime(sleepTime.getYear(), sleepTime.getMonthOfYear(), sleepTime.getDayOfMonth(), sleepTime.getHourOfDay(), sleepTime.getMinuteOfHour(), DateTimeZone.UTC);
        final DateTime wakeUpLocalUTC = new DateTime(wakeUpTime.getYear(), wakeUpTime.getMonthOfYear(), wakeUpTime.getDayOfMonth(), wakeUpTime.getHourOfDay(), wakeUpTime.getMinuteOfHour(), DateTimeZone.UTC);
        final DateTime outOfBedLocalUTC = new DateTime(outOfBedTime.getYear(), outOfBedTime.getMonthOfYear(), outOfBedTime.getDayOfMonth(), outOfBedTime.getHourOfDay(), outOfBedTime.getMinuteOfHour(), DateTimeZone.UTC);

        assertThat(goToBedLocalUTC, is(new DateTime(2015, 1, 5, 0, 18, DateTimeZone.UTC)));
        assertThat(sleepLocalUTC, is(new DateTime(2015, 1, 5, 1, 57, DateTimeZone.UTC)));
        assertThat(wakeUpLocalUTC, is(new DateTime(2015, 1, 5, 8, 16, DateTimeZone.UTC)));
        assertThat(outOfBedLocalUTC, is(new DateTime(2015, 1, 5, 8, 16, DateTimeZone.UTC)));
    }

    @Test
    public void testInsertOneMinuteEvent(){
        final ArrayList<Event> events = new ArrayList<>();

        final DateTime now = DateTime.now();
        events.add(new NullEvent(now.getMillis(), now.plusMinutes(2).getMillis(), 0, 0));

        List<Event> inserted = TimelineUtils.insertOneMinuteDurationEvents(events,
                new MotionEvent(now.getMillis(), now.plusMinutes(1).getMillis(), 0, 0));
        assertThat(inserted.size(), is(2));
        assertThat(inserted.get(0).getType(), is(Event.Type.MOTION));
        assertThat(inserted.get(0).getStartTimestamp(), is(now.getMillis()));
        assertThat(inserted.get(1).getStartTimestamp(), is(inserted.get(0).getEndTimestamp()));
    }

    @Test
    public void testInsertOneMinuteEventReplace(){
        final ArrayList<Event> events = new ArrayList<>();

        final DateTime now = DateTime.now();
        events.add(new NullEvent(now.getMillis(), now.plusMinutes(1).getMillis(), 0, 0));

        List<Event> inserted = TimelineUtils.insertOneMinuteDurationEvents(events,
                new MotionEvent(now.getMillis(), now.plusMinutes(1).getMillis(), 0, 0));
        assertThat(inserted.size(), is(1));
        assertThat(inserted.get(0).getType(), is(Event.Type.MOTION));
        assertThat(inserted.get(0).getStartTimestamp(), is(now.getMillis()));
    }

    @Test
    public void testInsertOneMinuteEventNoReplace(){
        final ArrayList<Event> events = new ArrayList<>();

        final DateTime now = DateTime.now();
        events.add(new MotionEvent(now.getMillis(), now.plusMinutes(1).getMillis(), 0, 0));

        List<Event> inserted = TimelineUtils.insertOneMinuteDurationEvents(events,
                new NullEvent(now.getMillis(), now.plusMinutes(1).getMillis(), 0, 0));
        assertThat(inserted.size(), is(1));
        assertThat(inserted.get(0).getType(), is(Event.Type.MOTION));
        assertThat(inserted.get(0).getStartTimestamp(), is(now.getMillis()));
    }


    @Test
    public void testInsertOneMinuteEventAddToHead(){
        final ArrayList<Event> events = new ArrayList<>();

        final DateTime now = DateTime.now();
        events.add(new MotionEvent(now.getMillis(), now.plusMinutes(3).getMillis(), 0, 0));

        List<Event> inserted = TimelineUtils.insertOneMinuteDurationEvents(events,
                new NullEvent(now.minusMinutes(1).getMillis(), now.getMillis(), 0, 0));
        assertThat(inserted.size(), is(2));
        assertThat(inserted.get(0).getType(), is(Event.Type.NONE));
        assertThat(inserted.get(0).getStartTimestamp(), is(now.minusMinutes(1).getMillis()));
        assertThat(inserted.get(1).getType(), is(Event.Type.MOTION));
        assertThat(inserted.get(1).getStartTimestamp(), is(now.getMillis()));
        
    }

    @Test
    public void testInsertOneMinuteEventAppendToEnd(){
        final ArrayList<Event> events = new ArrayList<>();

        final DateTime now = DateTime.now();
        events.add(new MotionEvent(now.getMillis(), now.plusMinutes(3).getMillis(), 0, 0));

        List<Event> inserted = TimelineUtils.insertOneMinuteDurationEvents(events,
                new NullEvent(now.plusMinutes(3).getMillis(), now.plusMinutes(4).getMillis(), 0, 0));
        assertThat(inserted.size(), is(2));
        assertThat(inserted.get(0).getType(), is(Event.Type.MOTION));
        assertThat(inserted.get(0).getStartTimestamp(), is(now.getMillis()));
        assertThat(inserted.get(1).getType(), is(Event.Type.NONE));
        assertThat(inserted.get(1).getStartTimestamp(), is(now.plusMinutes(3).getMillis()));
    }
}
