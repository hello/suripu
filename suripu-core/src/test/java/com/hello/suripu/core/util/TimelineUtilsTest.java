package com.hello.suripu.core.util;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.FallingAsleepEvent;
import com.hello.suripu.core.models.Events.InBedEvent;
import com.hello.suripu.core.models.Events.MotionEvent;
import com.hello.suripu.core.models.Events.NullEvent;
import com.hello.suripu.core.models.Events.OutOfBedEvent;
import com.hello.suripu.core.models.Events.WakeupEvent;
import com.hello.suripu.core.models.Insight;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 12/18/14.
 */
public class TimelineUtilsTest {


    @Test
    public void testConvertLightMotionToNone() {
        final List<Event> events = Lists.newArrayList();
        final DateTime now = DateTime.now();
        events.add(new MotionEvent(now.getMillis(), now.getMillis() + DateTimeConstants.MILLIS_PER_MINUTE,0,5));
        events.add(new MotionEvent(now.getMillis(), now.getMillis() + DateTimeConstants.MILLIS_PER_MINUTE,0,50));

        final List<Event> filteredEvents = TimelineUtils.convertLightMotionToNone(events, 10);
        assertThat(filteredEvents.size(), is(events.size()));
        assertThat(filteredEvents.get(0).getType(), is(Event.Type.MOTION));
        assertThat(filteredEvents.get(1).getType(), is(Event.Type.NONE));
    }

    @Test
    public void testRemoveMotionEventsOutsideBedPeriod() {
        final List<Event> events = Lists.newArrayList();
        final DateTime now = DateTime.now();
        events.add(new MotionEvent(now.getMillis(), now.plusMinutes(1).getMillis(),0,5));
        events.add(new InBedEvent(now.plusMinutes(1).getMillis(), now.plusMinutes(2).getMillis(),0));
        events.add(new NullEvent(now.plusMinutes(2).getMillis(), now.plusMinutes(3).getMillis(),0,100));
        events.add(new OutOfBedEvent(now.plusMinutes(3).getMillis(), now.plusMinutes(4).getMillis(),0));
        events.add(new MotionEvent(now.plusMinutes(4).getMillis(), now.plusMinutes(5).getMillis(),0,10));
        events.add(new MotionEvent(now.plusMinutes(5).getMillis(), now.plusMinutes(6).getMillis(),0,1));

        final Optional<Event> inBedOptional = Optional.of(events.get(1));
        final Optional<Event> outBedOptional = Optional.of(events.get(3));

        final List<Event> filteredEvents = TimelineUtils.removeMotionEventsOutsideBedPeriod(events, inBedOptional, outBedOptional);
        assertThat(filteredEvents.size(), is(events.size()));
        assertThat(filteredEvents.get(0).getType(), is(Event.Type.NONE));
        assertThat(filteredEvents.get(2).getType(), is(Event.Type.NONE));
        assertThat(filteredEvents.get(filteredEvents.size() - 1).getType(), is(Event.Type.NONE));

    }

    @Test
    public void testRemoveMotionEventsOutsideInBedPeriod() {
        final List<Event> events = Lists.newArrayList();
        final DateTime now = DateTime.now();
        events.add(new MotionEvent(now.getMillis(), now.plusMinutes(1).getMillis(),0,5));
        events.add(new InBedEvent(now.plusMinutes(1).getMillis(), now.plusMinutes(2).getMillis(),0));
        events.add(new NullEvent(now.plusMinutes(2).getMillis(), now.plusMinutes(3).getMillis(),0,100));
        events.add(new OutOfBedEvent(now.plusMinutes(3).getMillis(), now.plusMinutes(4).getMillis(),0));
        events.add(new MotionEvent(now.plusMinutes(4).getMillis(), now.plusMinutes(5).getMillis(),0,10));
        events.add(new MotionEvent(now.plusMinutes(5).getMillis(), now.plusMinutes(6).getMillis(),0,1));

        final Optional<Event> inBedOptional = Optional.of(events.get(1));
        final Optional<Event> outBedOptional = Optional.absent();

        final List<Event> filteredEvents = TimelineUtils.removeMotionEventsOutsideBedPeriod(events, inBedOptional, outBedOptional);
        assertThat(filteredEvents.size(), is(events.size()));
        assertThat(filteredEvents.get(0).getType(), is(Event.Type.NONE));
        assertThat(filteredEvents.get(2).getType(), is(Event.Type.NONE));
        assertThat(filteredEvents.get(filteredEvents.size() - 1).getType(), is(Event.Type.MOTION));

    }

    @Test
    public void testRemoveMotionEventsOutsideOutBedPeriod() {
        final List<Event> events = Lists.newArrayList();
        final DateTime now = DateTime.now();
        events.add(new MotionEvent(now.getMillis(), now.plusMinutes(1).getMillis(),0,5));
        events.add(new InBedEvent(now.plusMinutes(1).getMillis(), now.plusMinutes(2).getMillis(),0));
        events.add(new NullEvent(now.plusMinutes(2).getMillis(), now.plusMinutes(3).getMillis(),0,100));
        events.add(new OutOfBedEvent(now.plusMinutes(3).getMillis(), now.plusMinutes(4).getMillis(),0));
        events.add(new MotionEvent(now.plusMinutes(4).getMillis(), now.plusMinutes(5).getMillis(),0,10));
        events.add(new MotionEvent(now.plusMinutes(5).getMillis(), now.plusMinutes(6).getMillis(),0,1));

        final Optional<Event> inBedOptional = Optional.absent();
        final Optional<Event> outBedOptional = Optional.of(events.get(3));

        final List<Event> filteredEvents = TimelineUtils.removeMotionEventsOutsideBedPeriod(events, inBedOptional, outBedOptional);
        assertThat(filteredEvents.size(), is(events.size()));
        assertThat(filteredEvents.get(0).getType(), is(Event.Type.MOTION));
        assertThat(filteredEvents.get(2).getType(), is(Event.Type.NONE));
        assertThat(filteredEvents.get(filteredEvents.size() - 1).getType(), is(Event.Type.NONE));

    }

    @Test
    public void testRemoveMotionEventsBedPeriodRemovedBySafeGuard() {
        final List<Event> events = Lists.newArrayList();
        final DateTime now = DateTime.now();
        events.add(new MotionEvent(now.getMillis(), now.plusMinutes(1).getMillis(),0,5));
        events.add(new InBedEvent(now.plusMinutes(1).getMillis(), now.plusMinutes(2).getMillis(),0));
        events.add(new NullEvent(now.plusMinutes(2).getMillis(), now.plusMinutes(3).getMillis(),0,100));
        events.add(new OutOfBedEvent(now.plusMinutes(3).getMillis(), now.plusMinutes(4).getMillis(),0));
        events.add(new MotionEvent(now.plusMinutes(4).getMillis(), now.plusMinutes(5).getMillis(),0,10));
        events.add(new MotionEvent(now.plusMinutes(5).getMillis(), now.plusMinutes(6).getMillis(),0,1));

        final Optional<Event> inBedOptional = Optional.absent();
        final Optional<Event> outBedOptional = Optional.absent();

        final List<Event> filteredEvents = TimelineUtils.removeMotionEventsOutsideBedPeriod(events, inBedOptional, outBedOptional);
        assertThat(filteredEvents.size(), is(events.size()));
        assertThat(filteredEvents.get(0).getType(), is(Event.Type.MOTION));
        assertThat(filteredEvents.get(2).getType(), is(Event.Type.NONE));
        assertThat(filteredEvents.get(filteredEvents.size() - 1).getType(), is(Event.Type.MOTION));

    }

    @Test
    public void testGreyNullEventsOutsideBedPeriod() {
        final List<Event> events = Lists.newArrayList();
        final DateTime now = DateTime.now();
        events.add(new MotionEvent(now.getMillis(), now.plusMinutes(1).getMillis(),0,5));
        events.add(new InBedEvent(now.plusMinutes(1).getMillis(), now.plusMinutes(2).getMillis(),0));
        events.add(new NullEvent(now.plusMinutes(2).getMillis(), now.plusMinutes(3).getMillis(),0,100));
        events.add(new OutOfBedEvent(now.plusMinutes(3).getMillis(), now.plusMinutes(4).getMillis(),0));
        events.add(new MotionEvent(now.plusMinutes(4).getMillis(), now.plusMinutes(5).getMillis(),0,10));
        events.add(new MotionEvent(now.plusMinutes(5).getMillis(), now.plusMinutes(6).getMillis(),0,1));

        final Optional<Event> inBedOptional = Optional.of(events.get(1));
        final Optional<Event> outBedOptional = Optional.of(events.get(3));

        final List<Event> filteredEvents = TimelineUtils.removeMotionEventsOutsideBedPeriod(events, inBedOptional, outBedOptional);
        final List<Event> greyEvents = TimelineUtils.greyNullEventsOutsideBedPeriod(filteredEvents, inBedOptional, outBedOptional);

        assertThat(greyEvents.size(), is(events.size()));
        assertThat(greyEvents.get(0).getType(), is(Event.Type.NONE));
        assertThat(greyEvents.get(2).getType(), is(Event.Type.SLEEPING));
        assertThat(greyEvents.get(filteredEvents.size() - 1).getType(), is(Event.Type.NONE));

    }

    @Test
    public void testGreyNullEventsOutsideInBedPeriod() {
        final List<Event> events = Lists.newArrayList();
        final DateTime now = DateTime.now();
        events.add(new MotionEvent(now.getMillis(), now.plusMinutes(1).getMillis(),0,5));
        events.add(new InBedEvent(now.plusMinutes(1).getMillis(), now.plusMinutes(2).getMillis(),0));
        events.add(new NullEvent(now.plusMinutes(2).getMillis(), now.plusMinutes(3).getMillis(),0,100));
        events.add(new OutOfBedEvent(now.plusMinutes(3).getMillis(), now.plusMinutes(4).getMillis(),0));
        events.add(new MotionEvent(now.plusMinutes(4).getMillis(), now.plusMinutes(5).getMillis(),0,10));
        events.add(new NullEvent(now.plusMinutes(5).getMillis(), now.plusMinutes(6).getMillis(),0,1));

        final Optional<Event> inBedOptional = Optional.of(events.get(1));
        final Optional<Event> outBedOptional = Optional.absent();

        final List<Event> filteredEvents = TimelineUtils.removeMotionEventsOutsideBedPeriod(events, inBedOptional, outBedOptional);
        final List<Event> greyEvents = TimelineUtils.greyNullEventsOutsideBedPeriod(filteredEvents, inBedOptional, outBedOptional);

        assertThat(greyEvents.size(), is(events.size()));
        assertThat(greyEvents.get(0).getType(), is(Event.Type.NONE));
        assertThat(greyEvents.get(2).getType(), is(Event.Type.SLEEPING));
        assertThat(greyEvents.get(filteredEvents.size() - 2).getType(), is(Event.Type.MOTION));
        assertThat(greyEvents.get(filteredEvents.size() - 1).getType(), is(Event.Type.SLEEPING));

    }

    @Test
    public void testGreyNullEventsOutsideOutBedPeriod() {
        final List<Event> events = Lists.newArrayList();
        final DateTime now = DateTime.now();
        events.add(new MotionEvent(now.getMillis(), now.plusMinutes(1).getMillis(),0,5));
        events.add(new InBedEvent(now.plusMinutes(1).getMillis(), now.plusMinutes(2).getMillis(),0));
        events.add(new NullEvent(now.plusMinutes(2).getMillis(), now.plusMinutes(3).getMillis(),0,100));
        events.add(new OutOfBedEvent(now.plusMinutes(3).getMillis(), now.plusMinutes(4).getMillis(),0));
        events.add(new MotionEvent(now.plusMinutes(4).getMillis(), now.plusMinutes(5).getMillis(),0,10));
        events.add(new NullEvent(now.plusMinutes(5).getMillis(), now.plusMinutes(6).getMillis(),0,1));

        final Optional<Event> inBedOptional = Optional.absent();
        final Optional<Event> outBedOptional = Optional.of(events.get(3));

        final List<Event> filteredEvents = TimelineUtils.removeMotionEventsOutsideBedPeriod(events, inBedOptional, outBedOptional);
        final List<Event> greyEvents = TimelineUtils.greyNullEventsOutsideBedPeriod(filteredEvents, inBedOptional, outBedOptional);

        assertThat(greyEvents.size(), is(events.size()));
        assertThat(greyEvents.get(0).getType(), is(Event.Type.MOTION));
        assertThat(greyEvents.get(2).getType(), is(Event.Type.SLEEPING));
        assertThat(greyEvents.get(filteredEvents.size() - 2).getType(), is(Event.Type.NONE));
        assertThat(greyEvents.get(filteredEvents.size() - 1).getType(), is(Event.Type.NONE));

    }

    @Test
    public void testGreyNullEventsBedEventsRemovedBySafeGuard() {
        final List<Event> events = Lists.newArrayList();
        final DateTime now = DateTime.now();
        events.add(new MotionEvent(now.getMillis(), now.plusMinutes(1).getMillis(),0,5));
        events.add(new InBedEvent(now.plusMinutes(1).getMillis(), now.plusMinutes(2).getMillis(),0));
        events.add(new NullEvent(now.plusMinutes(2).getMillis(), now.plusMinutes(3).getMillis(),0,100));
        events.add(new OutOfBedEvent(now.plusMinutes(3).getMillis(), now.plusMinutes(4).getMillis(),0));
        events.add(new MotionEvent(now.plusMinutes(4).getMillis(), now.plusMinutes(5).getMillis(),0,10));
        events.add(new MotionEvent(now.plusMinutes(5).getMillis(), now.plusMinutes(6).getMillis(),0,1));

        final Optional<Event> inBedOptional = Optional.absent();
        final Optional<Event> outBedOptional = Optional.absent();

        final List<Event> filteredEvents = TimelineUtils.removeMotionEventsOutsideBedPeriod(events, inBedOptional, outBedOptional);
        final List<Event> greyEvents = TimelineUtils.greyNullEventsOutsideBedPeriod(filteredEvents, inBedOptional, outBedOptional);

        for(int i = 0; i < events.size(); i++){
            assertThat(greyEvents.get(i).getType() == Event.Type.NONE, is(false));
        }
        assertThat(greyEvents.get(2).getType(), is(Event.Type.SLEEPING));

    }

    @Test
    public void testMerge() {
        final List<Event> sleepSegments = new ArrayList<>();
        final DateTime now = DateTime.now();
        sleepSegments.add(new FallingAsleepEvent(now.getMillis(), now.getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0));
        sleepSegments.add(new FallingAsleepEvent(now.plusMinutes(1).getMillis(), now.plusMinutes(1).getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0));
        sleepSegments.add(new FallingAsleepEvent(now.plusMinutes(2).getMillis(), now.plusMinutes(2).getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0));
        sleepSegments.add(new FallingAsleepEvent(now.plusMinutes(3).getMillis(), now.plusMinutes(3).getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0));

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
        final URL fixtureCSVFile = Resources.getResource("fixtures/algorithm/pang_motion_2015_01_17_raw.csv");
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

        final List<Optional<Event>> sleepEvents = TimelineUtils.getSleepEvents(new DateTime(2015, 1, 17, 0, 0, DateTimeZone.UTC),
                trackerMotions,
                Optional.of(new DateTime(1421575200000L, DateTimeZone.UTC)),
                Optional.<DateTime>absent(),
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                true);
        final FallingAsleepEvent sleepSegment = (FallingAsleepEvent) sleepEvents.get(1).get();
        final InBedEvent goToBedSegment = (InBedEvent) sleepEvents.get(0).get();
        final WakeupEvent wakeUpSegment = (WakeupEvent) sleepEvents.get(2).get();
        final OutOfBedEvent outOfBedSegment = (OutOfBedEvent) sleepEvents.get(3).get();


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

        assertThat(goToBedLocalUTC, is(new DateTime(2015, 1, 18, 2, 4, DateTimeZone.UTC)));
        assertThat(sleepLocalUTC, is(new DateTime(2015, 1, 18, 2, 26, DateTimeZone.UTC)));
        assertThat(wakeUpLocalUTC, is(new DateTime(2015, 1, 18, 11, 43, DateTimeZone.UTC)));
        assertThat(outOfBedLocalUTC, is(new DateTime(2015, 1, 18, 11, 44, DateTimeZone.UTC)));
    }


    @Test
    public void testGetFullSleepEventsLightIsOnlyStrongIndicator(){
        final URL fixtureCSVFile = Resources.getResource("fixtures/algorithm/ksg_motion_2015_01_26_raw.csv");
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

        final List<Optional<Event>> sleepEvents = TimelineUtils.getSleepEvents(new DateTime(2015, 1, 26, 0, 0, DateTimeZone.UTC),
                trackerMotions,
                Optional.of(new DateTime(1422346440000L, DateTimeZone.UTC)),
                Optional.<DateTime>absent(),
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                true);
        final FallingAsleepEvent sleepSegment = (FallingAsleepEvent) sleepEvents.get(1).get();
        final InBedEvent goToBedSegment = (InBedEvent) sleepEvents.get(0).get();
        final WakeupEvent wakeUpSegment = (WakeupEvent) sleepEvents.get(2).get();
        final OutOfBedEvent outOfBedSegment = (OutOfBedEvent) sleepEvents.get(3).get();


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

        assertThat(goToBedLocalUTC, is(new DateTime(2015, 1, 27, 0, 18, DateTimeZone.UTC)));
        assertThat(sleepLocalUTC, is(new DateTime(2015, 1, 27, 0, 29, DateTimeZone.UTC)));
        assertThat(wakeUpLocalUTC, is(new DateTime(2015, 1, 27, 8, 44, DateTimeZone.UTC)));
        assertThat(outOfBedLocalUTC, is(new DateTime(2015, 1, 27, 8, 45, DateTimeZone.UTC)));
    }

    @Test
    public void testGetFullSleepEventsBigMotionBeforeGetIntoBed() throws IOException {
        final URL fixtureCSVFile = Resources.getResource("fixtures/algorithm/pang_motion_2015_01_24_raw.csv");
        final List<TrackerMotion> trackerMotions = new ArrayList<>();
        final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
        final String[] lines = csvString.split("\\n");
        for(int i = 1; i < lines.length; i++){
            final String[] columns = lines[i].split(",");
            final TrackerMotion trackerMotion = new TrackerMotion(0L, 0L, 0L, Long.valueOf(columns[0]), Integer.valueOf(columns[1]), Integer.valueOf(columns[2]), 0L, 0L,0L);
            //if(trackerMotion.value > 0){
            trackerMotions.add(trackerMotion);
            //}
        }

        final List<Optional<Event>> sleepEvents = TimelineUtils.getSleepEvents(new DateTime(2015, 1, 24, 0, 0, DateTimeZone.UTC),
                trackerMotions,
                Optional.of(new DateTime(1422181740000L, DateTimeZone.UTC)),
                Optional.<DateTime>absent(),
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                true);

        final FallingAsleepEvent sleepSegment = (FallingAsleepEvent) sleepEvents.get(1).get();
        final InBedEvent goToBedSegment = (InBedEvent) sleepEvents.get(0).get();
        final WakeupEvent wakeUpSegment = (WakeupEvent) sleepEvents.get(2).get();
        final OutOfBedEvent outOfBedSegment = (OutOfBedEvent) sleepEvents.get(3).get();

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

        assertThat(goToBedLocalUTC, is(new DateTime(2015, 1, 25, 2, 33, DateTimeZone.UTC)));
        //assertThat(sleepLocalUTC, is(new DateTime(2015, 1, 25, 1, 43, DateTimeZone.UTC)));
        assertThat(sleepLocalUTC, is(new DateTime(2015, 1, 25, 2, 44, DateTimeZone.UTC)));
        assertThat(wakeUpLocalUTC, is(new DateTime(2015, 1, 25, 8, 58, DateTimeZone.UTC)));
        assertThat(outOfBedLocalUTC, is(new DateTime(2015, 1, 25, 9, 20, DateTimeZone.UTC)));
    }


    @Test
    public void testGetFullSleepEventsWaveCanImprove(){
        final URL fixtureCSVFile = Resources.getResource("fixtures/algorithm/pang_motion_2015_01_21_raw.csv");
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

        final List<Optional<Event>> sleepEvents = TimelineUtils.getSleepEvents(new DateTime(2015, 1, 21, 0, 0, DateTimeZone.UTC),
                trackerMotions,
                Optional.of(new DateTime(1421913780000L, DateTimeZone.UTC)),
                Optional.of(new DateTime(1421940660000L, DateTimeZone.UTC)),
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                true);

        final FallingAsleepEvent sleepSegment = (FallingAsleepEvent) sleepEvents.get(1).get();
        final InBedEvent goToBedSegment = (InBedEvent) sleepEvents.get(0).get();

        //assertThat(sleepEvents.get(2).isPresent(), is(false));
        //assertThat(sleepEvents.get(3).isPresent(), is(false));

        final WakeupEvent wakeUpSegment = (WakeupEvent) sleepEvents.get(2).get();
        final OutOfBedEvent outOfBedSegment = (OutOfBedEvent) sleepEvents.get(3).get();

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

        assertThat(goToBedLocalUTC, is(new DateTime(2015, 1, 22, 0, 13, DateTimeZone.UTC)));
        //assertThat(sleepLocalUTC, is(new DateTime(2015, 1, 25, 1, 43, DateTimeZone.UTC)));
        assertThat(sleepLocalUTC, is(new DateTime(2015, 1, 22, 0, 14, DateTimeZone.UTC)));
        assertThat(wakeUpLocalUTC, is(new DateTime(2015, 1, 22, 7, 22, DateTimeZone.UTC)));
        assertThat(outOfBedLocalUTC, is(new DateTime(2015, 1, 22, 7, 44, DateTimeZone.UTC)));
    }


    @Test
    public void testGetFullSleepEventsCannotFallAsleepAfterInBed(){
        final URL fixtureCSVFile = Resources.getResource("fixtures/algorithm/tim_motion_2015_01_04_raw.csv");
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

        final List<Optional<Event>> sleepEvents = TimelineUtils.getSleepEvents(new DateTime(2015, 1, 04, 0, 0, DateTimeZone.UTC),
                trackerMotions,
                Optional.of(new DateTime(1420445760000L, DateTimeZone.UTC)),
                Optional.of(new DateTime(1420473540000L, DateTimeZone.UTC)),
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                true);

        final FallingAsleepEvent sleepSegment = (FallingAsleepEvent) sleepEvents.get(1).get();
        final InBedEvent goToBedSegment = (InBedEvent) sleepEvents.get(0).get();
        final WakeupEvent wakeUpSegment = (WakeupEvent) sleepEvents.get(2).get();
        final OutOfBedEvent outOfBedSegment = (OutOfBedEvent) sleepEvents.get(3).get();


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

        /*
        Same thing, cannot deal with wake up at the middle of night. because we use light and time as a feature.
         */
        assertThat(goToBedLocalUTC, is(new DateTime(2015, 1, 5, 0, 20, DateTimeZone.UTC)));
        assertThat(sleepLocalUTC, is(new DateTime(2015, 1, 5, 0, 21, DateTimeZone.UTC)));
        assertThat(wakeUpLocalUTC, is(new DateTime(2015, 1, 5, 8, 13, DateTimeZone.UTC)));
        assertThat(outOfBedLocalUTC, is(new DateTime(2015, 1, 5, 8, 14, DateTimeZone.UTC)));  //heuristic
    }


    @Test
    public void testGetFullSleepEventsWeekend() throws IOException {
        final URL fixtureCSVFile = Resources.getResource("fixtures/algorithm/pang_motion_2015_02_01_raw.csv");
        final List<TrackerMotion> trackerMotions = new ArrayList<>();

        final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
        final String[] lines = csvString.split("\\n");
        for(int i = 1; i < lines.length; i++){
            final String[] columns = lines[i].split(",");
            final TrackerMotion trackerMotion = new TrackerMotion(0L, 0L, 0L, Long.valueOf(columns[0]), Integer.valueOf(columns[1]), Integer.valueOf(columns[2]), 0L, 0L,0L);
            //if(trackerMotion.value > 0){
            trackerMotions.add(trackerMotion);
            //}
        }

        final List<Optional<Event>> sleepEvents = TimelineUtils.getSleepEvents(new DateTime(2015, 2, 1, 0, 0, DateTimeZone.UTC),
                trackerMotions,
                Optional.of(new DateTime(1422866580000L, DateTimeZone.UTC)),
                Optional.of(new DateTime(1422891060000L, DateTimeZone.UTC)),
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                true);
        final FallingAsleepEvent sleepSegment = (FallingAsleepEvent) sleepEvents.get(1).get();
        final InBedEvent goToBedSegment = (InBedEvent) sleepEvents.get(0).get();
        final WakeupEvent wakeUpSegment = (WakeupEvent) sleepEvents.get(2).get();
        final OutOfBedEvent outOfBedSegment = (OutOfBedEvent) sleepEvents.get(3).get();

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

        /*
        Same thing, cannot deal with wake up at the middle of night. because we use light and time as a feature.
         */
        assertThat(goToBedLocalUTC, is(new DateTime(2015, 2, 2, 0, 43, DateTimeZone.UTC)));
        assertThat(sleepLocalUTC, is(new DateTime(2015, 2, 2, 1, 38, DateTimeZone.UTC)));
        assertThat(wakeUpLocalUTC, is(new DateTime(2015, 2, 2, 8, 25, DateTimeZone.UTC)));
        assertThat(outOfBedLocalUTC, is(new DateTime(2015, 2, 2, 8, 47, DateTimeZone.UTC)));  //heuristic
    }

    @Test
    public void testInsertOneMinuteEvent(){
        final ArrayList<Event> events = new ArrayList<>();

        final DateTime now = DateTime.now();
        events.add(new NullEvent(now.getMillis(), now.plusMinutes(2).getMillis(), 0, 0));

        final List<Event> inserted = TimelineUtils.insertOneMinuteDurationEvents(events,
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

        final List<Event> inserted = TimelineUtils.insertOneMinuteDurationEvents(events,
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

        final List<Event> inserted = TimelineUtils.insertOneMinuteDurationEvents(events,
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

        final List<Event> inserted = TimelineUtils.insertOneMinuteDurationEvents(events,
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

        final List<Event> inserted = TimelineUtils.insertOneMinuteDurationEvents(events,
                new NullEvent(now.plusMinutes(3).getMillis(), now.plusMinutes(4).getMillis(), 0, 0));
        assertThat(inserted.size(), is(2));
        assertThat(inserted.get(0).getType(), is(Event.Type.MOTION));
        assertThat(inserted.get(0).getStartTimestamp(), is(now.getMillis()));
        assertThat(inserted.get(1).getType(), is(Event.Type.NONE));
        assertThat(inserted.get(1).getStartTimestamp(), is(now.plusMinutes(3).getMillis()));
    }

    @Test
    public void testGeneratePreSleepInsightsMissingSensorData() {
        final AllSensorSampleList allSensorSampleList = new AllSensorSampleList();
        final List<Insight> insights = TimelineUtils.generatePreSleepInsights(allSensorSampleList, 0L, 999L);
        assertThat(insights.isEmpty(), is(true));
    }


    @Test
    public void testGeneratePreSleepInsightsEmptyData() {
        final AllSensorSampleList allSensorSampleList = new AllSensorSampleList();
        allSensorSampleList.add(Sensor.LIGHT, Collections.EMPTY_LIST);
        final List<Insight> insights = TimelineUtils.generatePreSleepInsights(allSensorSampleList, 0L, 999L);
        assertThat(insights.isEmpty(), is(true));
    }

    @Test
    public void testGeneratePreSleepInsightsWithData() {
        final AllSensorSampleList allSensorSampleList = new AllSensorSampleList();
        assertThat(allSensorSampleList.isEmpty(), is(true));

        allSensorSampleList.add(Sensor.LIGHT, Lists.newArrayList(new Sample(DateTime.now().getMillis(), 10.0f, 0)));
        assertThat(allSensorSampleList.get(Sensor.LIGHT).isEmpty(), is(false));
        assertThat(allSensorSampleList.isEmpty(), is(false));
        assertThat(allSensorSampleList.get(Sensor.TEMPERATURE).isEmpty(), is(true));

    }


    @Test
    public void testAlarmInTimelineEmptyRingTime() {
        final RingTime emptyRingTime = RingTime.createEmpty();
        final List<RingTime> ringTimes = Lists.newArrayList(emptyRingTime);
        final DateTime now = DateTime.now();
        final List<Event> events = TimelineUtils.getAlarmEvents(ringTimes, now.minusHours(12), now, 0);
        assertThat(events.isEmpty(), is(true));
    }


    @Test
    public void testAlarmInTimelineRingTimeInThePast() {
        final DateTime now = DateTime.now();
        final RingTime ringTime = new RingTime(now.minusDays(1).getMillis(), now.minusDays(1).plusMinutes(10).getMillis(), 0L, false);
        final List<RingTime> ringTimes = Lists.newArrayList(ringTime);
        final List<Event> events = TimelineUtils.getAlarmEvents(ringTimes, now.plusDays(1), now.plusDays(5), 0);
        assertThat(events.isEmpty(), is(true));
    }

    @Test
    public void testAlarmInTimelineRingTimeInTheFuture() {
        final DateTime now = DateTime.now();
        final RingTime ringTime = new RingTime(now.plusDays(1).getMillis(), now.plusDays(1).getMillis(), 0L, false);
        final List<RingTime> ringTimes = Lists.newArrayList(ringTime);
        final List<Event> events = TimelineUtils.getAlarmEvents(ringTimes, now.minusHours(12), now, 0);
        assertThat(events.isEmpty(), is(true));
    }


    @Test
    public void testAlarmInTimelineRingTimeValid() {
        final DateTime now = DateTime.now();
        final RingTime ringTime = new RingTime(now.getMillis(), now.getMillis(), 0L, false);
        final List<RingTime> ringTimes = Lists.newArrayList(ringTime);
        final List<Event> events = TimelineUtils.getAlarmEvents(ringTimes, now.minusHours(12), now.plusHours(1), 0);
        assertThat(events.size(), is(1));
    }
}
