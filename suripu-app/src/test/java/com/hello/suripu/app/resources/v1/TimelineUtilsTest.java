package com.hello.suripu.app.resources.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SensorReading;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.TimelineUtils;
import org.joda.time.DateTime;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TimelineUtilsTest {

    @Test
    public void testMerge() {
        final List<SleepSegment> sleepSegments = new ArrayList<>();
        final DateTime now = DateTime.now();
        sleepSegments.add(new SleepSegment(1L, now.getMillis(), 0, 60, 100, Event.Type.MOTION, new ArrayList<SensorReading>(), null));
        sleepSegments.add(new SleepSegment(2L, now.plusMinutes(1).getMillis(), 0, 60, 100, Event.Type.MOTION, new ArrayList<SensorReading>(), null));
        sleepSegments.add(new SleepSegment(3L, now.plusMinutes(2).getMillis(), 0, 60, 100, Event.Type.MOTION, new ArrayList<SensorReading>(), null));
        sleepSegments.add(new SleepSegment(33L, now.plusMinutes(3).getMillis(), 0, 60, 100, Event.Type.MOTION, new ArrayList<SensorReading>(), null));

        final List<SleepSegment> mergedSegments = TimelineUtils.mergeConsecutiveSleepSegments(sleepSegments, 5);
        assertThat(mergedSegments.size(), is(1));
    }


    @Test
    public void testNoMerge() {
        final List<SleepSegment> sleepSegments = new ArrayList<>();
        final DateTime now = DateTime.now();
        sleepSegments.add(new SleepSegment(3L, now.getMillis(), 0, 60, 100, Event.Type.MOTION, new ArrayList<SensorReading>(), null));
        sleepSegments.add(new SleepSegment(4L, now.plusMinutes(1).getMillis(), 0, 60, 50, Event.Type.MOTION, new ArrayList<SensorReading>(), null));
        sleepSegments.add(new SleepSegment(5L, now.plusMinutes(2).getMillis(), 0, 60, 0, Event.Type.MOTION, new ArrayList<SensorReading>(), null));
        sleepSegments.add(new SleepSegment(55L, now.plusMinutes(3).getMillis(), 0, 60, 10, Event.Type.MOTION, new ArrayList<SensorReading>(), null));

        final List<SleepSegment> mergedSegments = TimelineUtils.mergeConsecutiveSleepSegments(sleepSegments, 5);
        assertThat(mergedSegments.size(), is(sleepSegments.size()));
    }


    @Test
    public void testMergeSegments() {

        final List<TrackerMotion> trackerMotions = new ArrayList<>();
        final DateTime now = DateTime.now();

        trackerMotions.add(new TrackerMotion(6L,99L,123L, now.getMillis(), 100, 0));
        trackerMotions.add(new TrackerMotion(7L,99L,123L, now.plusMinutes(1).getMillis(), 50, 0));
        trackerMotions.add(new TrackerMotion(8L,99L,123L, now.plusMinutes(2).getMillis(), 0, 0));
        trackerMotions.add(new TrackerMotion(9L,99L,123L, now.plusMinutes(2).getMillis(), 100, 0));

        List<SleepSegment> segments = TimelineUtils.generateSleepSegments(trackerMotions, 5, true);
        assertThat(segments.size(), is(trackerMotions.size()));

        final List<SleepSegment> mergedSegments = TimelineUtils.mergeConsecutiveSleepSegments(segments, 5);
        assertThat(mergedSegments.size(), is(trackerMotions.size()));
    }


    @Test
    public void testMergeSomeSegments() {

        final List<TrackerMotion> trackerMotions = new ArrayList<>();
        final DateTime now = DateTime.now();

        trackerMotions.add(new TrackerMotion(6L,99L,123L, now.getMillis(), 100, 0));
        trackerMotions.add(new TrackerMotion(7L,99L,123L, now.plusMinutes(1).getMillis(), 100, 0));
        trackerMotions.add(new TrackerMotion(8L,99L,123L, now.plusMinutes(2).getMillis(), 0, 0));
        trackerMotions.add(new TrackerMotion(9L,99L,123L, now.plusMinutes(2).getMillis(), 100, 0));

        List<SleepSegment> segments = TimelineUtils.generateSleepSegments(trackerMotions, 5, true);
        final List<SleepSegment> mergedSegments = TimelineUtils.mergeConsecutiveSleepSegments(segments, 5);
        assertThat(mergedSegments.size(), is(3));
    }


    @Test
    public void testConvertTrackerMotionToSleepSegments() {
        final List<TrackerMotion> trackerMotions = new ArrayList<>();
        final DateTime now = DateTime.now();

        trackerMotions.add(new TrackerMotion(6L,99L,123L, now.getMillis(), 100, 0));
        trackerMotions.add(new TrackerMotion(7L,99L,123L, now.plusMinutes(1).getMillis(), 100, 0));
        trackerMotions.add(new TrackerMotion(8L,99L,123L, now.plusMinutes(2).getMillis(), 0, 0));
        trackerMotions.add(new TrackerMotion(9L,99L,123L, now.plusMinutes(3).getMillis(), 100, 0));

        final List<SleepSegment> segments = TimelineUtils.generateSleepSegments(trackerMotions, 5, true);
        assertThat(segments.size(), is(trackerMotions.size()));
    }

    @Test
    public void testEmptyTrackerMotionData() {
        final List<TrackerMotion> trackerMotions = new ArrayList<>();
        final List<SleepSegment> segments = TimelineUtils.generateSleepSegments(trackerMotions, 5, true);
        assertThat(segments.size(), is(0));
    }

    @Test(expected = RuntimeException.class)
    public void testEmptySleepSegments() {
        final List<SleepSegment> segments = new ArrayList<>();
        TimelineUtils.mergeConsecutiveSleepSegments(segments, 5);
    }

    @Test
    public void testTooCloseTimestamps() {
        final List<SleepSegment> sleepSegments = new ArrayList<>();
        final DateTime now = DateTime.now();
        final Long millis = now.getMillis();
        sleepSegments.add(new SleepSegment(3L, millis, 0, 60, 100, Event.Type.MOTION, new ArrayList<SensorReading>(), null));
        sleepSegments.add(new SleepSegment(4L, millis, 0, 60, 50, Event.Type.MOTION, new ArrayList<SensorReading>(), null));
        sleepSegments.add(new SleepSegment(5L, millis, 0, 60, 0, Event.Type.MOTION, new ArrayList<SensorReading>(), null));
        sleepSegments.add(new SleepSegment(55L, millis, 0, 60, 10, Event.Type.MOTION, new ArrayList<SensorReading>(), null));

        final SleepSegment mergedSegment = TimelineUtils.merge(sleepSegments, 5);
        assertThat(mergedSegment.getDurationInSeconds(), is(5 * 60));
    }

    @Test
    public void testNormalizedSleepDepth() {
        final List<SleepSegment> sleepSegments = new ArrayList<>();
        final DateTime now = DateTime.now();
        final Long millis = now.getMillis();

        sleepSegments.add(new SleepSegment(3L, millis, 0, 60, 99, Event.Type.MOTION, new ArrayList<SensorReading>(), null));
        sleepSegments.add(new SleepSegment(4L, millis, 0, 60, 100, Event.Type.MOTION, new ArrayList<SensorReading>(), null));
        sleepSegments.add(new SleepSegment(5L, millis, 0, 60, 91, Event.Type.MOTION, new ArrayList<SensorReading>(), null));

        final List<SleepSegment> normalized = TimelineUtils.categorizeSleepDepth(sleepSegments);
        for(final SleepSegment segment : normalized) {
            assertThat(segment.sleepDepth, is(TimelineUtils.HIGHEST_SLEEP_DEPTH));
        }
    }


    public static String getFile(String resourceClassPathLocation) {
        try {
            return new File(Resources.getResource(resourceClassPathLocation)
                    .toURI()).getAbsolutePath();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void testTrackerMotion() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final URL url = Resources.getResource("trackermotion.json");
        final String content = Resources.toString(url, Charsets.UTF_8);
        final List<TrackerMotion> trackerMotions = objectMapper.readValue(content, new TypeReference<List<TrackerMotion>>() {});
        final List<SleepSegment> segments = TimelineUtils.generateSleepSegments(trackerMotions, 5, true);

        final long timeDiffInMins = (trackerMotions.get(trackerMotions.size() - 1).timestamp - trackerMotions.get(0).timestamp) / 60000L + 1L;

        assertThat(segments.size(), is((int)timeDiffInMins));

        final List<SleepSegment> categorized = TimelineUtils.categorizeSleepDepth(segments);
        final List<SleepSegment> merged = TimelineUtils.mergeConsecutiveSleepSegments(categorized, 5);
//        assertThat(merged.size(), is(19));
//        System.out.println("-----");
//        for(SleepSegment segment : merged) {
//            System.out.println(segment.toString());
//        }
    }

    @Test
    public void testMergeByTimeBucket() {
        List<SleepSegment> sleepSegments = new ArrayList<>();
        final DateTime now = DateTime.now();
        final Long millis = now.getMillis();
        sleepSegments.add(new SleepSegment(3L, millis, 0, 60, 99, Event.Type.MOTION, new ArrayList<SensorReading>(), null));
        sleepSegments.add(new SleepSegment(4L, millis, 0, 60, 100, Event.Type.MOTION, new ArrayList<SensorReading>(), null));
        sleepSegments.add(new SleepSegment(5L, millis, 0, 60, 71, Event.Type.MOTION, new ArrayList<SensorReading>(), null));
        sleepSegments.add(new SleepSegment(6L, millis, 0, 60, 50, Event.Type.MOTION, new ArrayList<SensorReading>(), null));

        final List<SleepSegment> mergedSegments = TimelineUtils.mergeByTimeBucket(sleepSegments, 2);
        assertThat(mergedSegments.size(), is(2));
    }


    @Test
    public void simpleMerge() {
        List<SleepSegment> sleepSegments = new ArrayList<>();
        final DateTime now = DateTime.now();
        final Long millis = now.getMillis();
        sleepSegments.add(new SleepSegment(3L, millis, 0, 60, 99, Event.Type.MOTION, new ArrayList<SensorReading>(), null));
        sleepSegments.add(new SleepSegment(4L, millis, 0, 60, 100, Event.Type.MOTION, new ArrayList<SensorReading>(), null));
        sleepSegments.add(new SleepSegment(5L, millis, 0, 60, 71, Event.Type.MOTION, new ArrayList<SensorReading>(), null));

        SleepSegment mergedSegment = TimelineUtils.merge(sleepSegments);
        assertThat(mergedSegment.sleepDepth, is(71));
    }
}
