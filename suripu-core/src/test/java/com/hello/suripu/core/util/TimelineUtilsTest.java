package com.hello.suripu.core.util;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.MotionEvent;
import com.hello.suripu.core.models.Events.SleepEvent;
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

        trackerMotions.add(new TrackerMotion(6L,99L,123L, now.getMillis(), 100, 0));
        trackerMotions.add(new TrackerMotion(7L,99L,123L, now.plusMinutes(1).getMillis(), 50, 0));
        trackerMotions.add(new TrackerMotion(8L,99L,123L, now.plusMinutes(2).getMillis(), 0, 0));
        trackerMotions.add(new TrackerMotion(9L,99L,123L, now.plusMinutes(2).getMillis(), 100, 0));

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
    public void testGetSleepPeriod(){
        final URL fixtureCSVFile = Resources.getResource("pang_motion_2014_12_02_raw.csv");
        final List<TrackerMotion> trackerMotions = new ArrayList<>();
        try {
            final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 1; i < lines.length; i++){
                final String[] columns = lines[i].split(",");
                trackerMotions.add(new TrackerMotion(0L, 0L, 0L, Long.valueOf(columns[0]), Integer.valueOf(columns[1]), Integer.valueOf(columns[2])));
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }

        final Segment sleepSegment = TimelineUtils.getSleepPeriod(new DateTime(2014, 12, 02, 0, 0, DateTimeZone.UTC),
                trackerMotions,
                Optional.of(new DateTime(1417598760000L, DateTimeZone.UTC)));

        // Out put from python script suripu_light_test.py:
        /*
        sleep at 2014-12-03 01:39:00, prob: 1.06747942852, amp: 5471
        wake up at 2014-12-03 07:09:00, prob: 0.0924221378596, amp: 518
        */

        final DateTime sleepTime = new DateTime(sleepSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.getOffsetMillis()));
        final DateTime wakeUpTime = new DateTime(sleepSegment.getEndTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.getOffsetMillis()));
        final DateTime sleepLocalUTC = new DateTime(sleepTime.getYear(), sleepTime.getMonthOfYear(), sleepTime.getDayOfMonth(), sleepTime.getHourOfDay(), sleepTime.getMinuteOfHour(), DateTimeZone.UTC);
        final DateTime wakeUpLocalUTC = new DateTime(wakeUpTime.getYear(), wakeUpTime.getMonthOfYear(), wakeUpTime.getDayOfMonth(), wakeUpTime.getHourOfDay(), wakeUpTime.getMinuteOfHour(), DateTimeZone.UTC);

        assertThat(sleepLocalUTC, is(new DateTime(2014, 12, 03, 1, 39, DateTimeZone.UTC)));
        assertThat(wakeUpLocalUTC, is(new DateTime(2014, 12, 03, 7, 9, DateTimeZone.UTC)));
    }
}
