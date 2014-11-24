package com.hello.suripu.app.resources.v1;

import com.google.common.io.Resources;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.MotionEvent;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.TimelineUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TimelineUtilsTest {

    @Test
    public void testMerge() {
        final List<Event> sleepSegments = new ArrayList<>();
        final DateTime now = DateTime.now();
        sleepSegments.add(new Event(Event.Type.MOTION, now.getMillis(), now.getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0));
        sleepSegments.add(new Event(Event.Type.MOTION, now.plusMinutes(1).getMillis(), now.plusMinutes(1).getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0));
        sleepSegments.add(new Event(Event.Type.MOTION, now.plusMinutes(2).getMillis(), now.plusMinutes(2).getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0));
        sleepSegments.add(new Event(Event.Type.MOTION, now.plusMinutes(3).getMillis(), now.plusMinutes(3).getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0));

        final List<Event> mergedSegments = TimelineUtils.generateAlignedSegmentsByTypeWeight(sleepSegments,
                DateTimeConstants.MILLIS_PER_MINUTE,
                5, true);
        assertThat(mergedSegments.size(), is(1));
    }


    @Test
    public void testMergeSegments() {

        final List<TrackerMotion> trackerMotions = new ArrayList<>();
        final DateTime now = DateTime.now();

        trackerMotions.add(new TrackerMotion(6L,99L,123L, now.getMillis(), 100, 0));
        trackerMotions.add(new TrackerMotion(7L,99L,123L, now.plusMinutes(1).getMillis(), 50, 0));
        trackerMotions.add(new TrackerMotion(8L,99L,123L, now.plusMinutes(2).getMillis(), 0, 0));
        trackerMotions.add(new TrackerMotion(9L,99L,123L, now.plusMinutes(2).getMillis(), 100, 0));

        final List<MotionEvent> motionEvents = TimelineUtils.generateMotionEvents(trackerMotions);
        final List<Event> segments = new LinkedList<>();
        segments.addAll(motionEvents);
        assertThat(segments.size(), is(trackerMotions.size()));

        final List<Event> mergedSegments = TimelineUtils.generateAlignedSegmentsByTypeWeight(segments,
                DateTimeConstants.MILLIS_PER_MINUTE,
                5,
                true);
        assertThat(mergedSegments.size(), is(1));
    }


    @Test
    public void testMergeSomeSegments() {

        final List<TrackerMotion> trackerMotions = new ArrayList<>();
        final DateTime now = DateTime.now();

        trackerMotions.add(new TrackerMotion(6L,99L,123L, now.getMillis(), 100, 0));
        trackerMotions.add(new TrackerMotion(7L,99L,123L, now.plusMinutes(1).getMillis(), 100, 0));
        trackerMotions.add(new TrackerMotion(8L,99L,123L, now.plusMinutes(2).getMillis(), 0, 0));
        trackerMotions.add(new TrackerMotion(9L,99L,123L, now.plusMinutes(2).getMillis(), 100, 0));

        final List<MotionEvent> motionEvents = TimelineUtils.generateMotionEvents(trackerMotions);
        final List<Event> segments = new LinkedList<>();
        segments.addAll(motionEvents);
        final List<Event> mergedSegments = TimelineUtils.generateAlignedSegmentsByTypeWeight(segments,
                DateTimeConstants.MILLIS_PER_MINUTE,
                5, true);
        assertThat(mergedSegments.size(), is(1));
    }



    @Test
    public void testEmptySleepSegments() {
        final List<Event> segments = new ArrayList<>();
        final List<Event> actual = TimelineUtils.generateAlignedSegmentsByTypeWeight(segments, DateTimeConstants.MILLIS_PER_MINUTE, 5, true);
        assertThat(actual.size(), is(0));
    }


    @Test
    public void testMergeSlot() {
        final List<Event> sleepSegments = new ArrayList<>();
        final DateTime now = DateTime.now();
        //sleepSegments.add(new MotionEvent(millis, millis + DateTimeConstants.MILLIS_PER_MINUTE, 0, 99, 100));
        sleepSegments.add(new MotionEvent(now.getMillis(), now.getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0, 100, 100));
        sleepSegments.add(new MotionEvent(now.plusMinutes(5).getMillis(), now.plusMinutes(5).getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0, 50, 100));
        sleepSegments.add(new MotionEvent(now.plusMinutes(6).getMillis(), now.plusMinutes(6).getMillis() + DateTimeConstants.MILLIS_PER_MINUTE, 0, 0, 100));

        final List<Event> mergedSegments = TimelineUtils.generateAlignedSegmentsByTypeWeight(sleepSegments, DateTimeConstants.MILLIS_PER_MINUTE, 5, true);
        assertThat(mergedSegments.size(), is(2));
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
        sleepSegments.add(new MotionEvent(millis, millis + DateTimeConstants.MILLIS_PER_MINUTE, 0, 99, 100));
        sleepSegments.add(new MotionEvent(millis, millis + DateTimeConstants.MILLIS_PER_MINUTE, 0, 100, 100));
        sleepSegments.add(new MotionEvent(millis, millis + DateTimeConstants.MILLIS_PER_MINUTE, 0, 71, 100));
        sleepSegments.add(new MotionEvent(millis, millis + DateTimeConstants.MILLIS_PER_MINUTE, 0, 50, 100));

        final List<Event> mergedSegments = TimelineUtils.generateAlignedSegmentsByTypeWeight(sleepSegments, DateTimeConstants.MILLIS_PER_MINUTE, 2, true);
        assertThat(mergedSegments.size(), is(1));
    }

}
