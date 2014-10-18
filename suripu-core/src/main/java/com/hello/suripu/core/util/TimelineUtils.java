package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.hello.suripu.core.models.CurrentRoomState;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Insight;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.SensorReading;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.models.TrackerMotion;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkNotNull;

public class TimelineUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineUtils.class);
    private static final long ONE_MIN_IN_MILLIS = 60000L;
    /**
     * Merge a List<Segment> to a single segment
     * The minimum duration is 60 seconds
     * @param segments
     * @param threshold defines the min. duration of a segment
     * @return
     */
    @Timed
    public static SleepSegment merge(final List<SleepSegment> segments, int threshold) {
        checkNotNull(segments, "segments can not be null");
        if(segments.isEmpty()) {
            throw new RuntimeException("segments can not be empty");
        }

        final Long timestamp = segments.get(0).timestamp;
        final Long id = segments.get(0).id;
        float total = 0f;
        for (final SleepSegment segment : segments) {
            total += (float) segment.sleepDepth;
        }
        final Integer sleepDepth = TimelineUtils.categorizeSleepDepth((int) (total / segments.size()));
        final Integer offsetMillis = segments.get(0).offsetMillis;
        final String eventType = segments.get(0).eventType;
        final String message = segments.get(0).message;
        final List<SensorReading> sensors = segments.get(0).sensors;

        Long durationInMillis = segments.get(segments.size() -1).timestamp - segments.get(0).timestamp +  segments.get(segments.size() -1).durationInSeconds * 1000;


        if (durationInMillis < threshold * ONE_MIN_IN_MILLIS) {
            durationInMillis = threshold * ONE_MIN_IN_MILLIS;
        }
        return new SleepSegment(id, timestamp, offsetMillis, Math.round(durationInMillis / 1000), sleepDepth, eventType, message, sensors);
    }


    /**
     * Generate Sleep Segments from the TrackerMotion data
     * @param trackerMotions
     * @param threshold
     * @param createMotionlessSegment
     * @return
     */
    @Timed
    public static List<SleepSegment> generateSleepSegments(final List<TrackerMotion> trackerMotions, final int threshold, final boolean createMotionlessSegment) {

        return generateSleepSegments(trackerMotions, threshold, createMotionlessSegment, Optional.<DeviceData>absent());
    }

    public static List<SleepSegment> generateSleepSegments(final List<TrackerMotion> trackerMotions, final int threshold, final boolean createMotionlessSegment, final Optional<DeviceData> deviceData) {
        final List<SleepSegment> sleepSegments = new ArrayList<>();


        if(trackerMotions.isEmpty()) {
            return sleepSegments;
        }

        Long maxSVM = 0L;
        for(final TrackerMotion trackerMotion : trackerMotions) {
            maxSVM = Math.max(maxSVM, trackerMotion.value);
        }

        LOGGER.debug("Max SVM = {}", maxSVM);

        final Long trackerId = trackerMotions.get(0).trackerId;

        Long lastTimestamp = trackerMotions.get(0).timestamp;
        for(final TrackerMotion trackerMotion : trackerMotions) {
            if (!trackerMotion.trackerId.equals(trackerId)) {
                LOGGER.warn("User has multiple pills: {} and {}", trackerId, trackerMotion.trackerId);
                break; // if user has multiple pill, only use data from the latest tracker_id
            }

            if (createMotionlessSegment && trackerMotion.timestamp != lastTimestamp) {
                // pad with 1-min segments with no movement
                final int durationInSeconds = (int) (trackerMotion.timestamp - lastTimestamp) / 1000;
                final int segmentDuration = 60;
                final int numSegments = durationInSeconds / 60;
                for (int j = 0; j < numSegments; j++) {
                    final SleepSegment sleepSegment = new SleepSegment(trackerMotion.id,
                            lastTimestamp + (j * segmentDuration * 1000), // millis
                            trackerMotion.offsetMillis,
                            segmentDuration, // seconds
                            100, // depth 100 => motionless
                            Event.Type.NONE.toString(),
                            "",
                            Collections.<SensorReading>emptyList());
                    sleepSegments.add(sleepSegment);
                }


            }

            int sleepDepth = normalizeSleepDepth(trackerMotion.value, maxSVM);

            String eventType = (sleepDepth <= threshold) ? Event.Type.MOTION.toString() : Event.Type.NONE.toString(); // TODO: put these in a config file or DB

            String eventMessage = "";

            if(eventType.equals(Event.Type.MOTION.toString())) {
                eventMessage = Event.getMessage(Event.Type.MOTION, new DateTime(trackerMotion.timestamp, DateTimeZone.UTC).plusMillis(trackerMotion.offsetMillis));
            }

            // TODO: make this work
            if (trackerMotion.value == maxSVM) {
                eventType = Event.Type.MOTION.toString();

            }

            final List<SensorReading> readings = new ArrayList<>();
            if(deviceData.isPresent()) {
                readings.addAll(SensorReading.fromDeviceData(deviceData.get()));
            }

            final SleepSegment sleepSegment = new SleepSegment(
                    trackerMotion.id,
                    trackerMotion.timestamp,
                    trackerMotion.offsetMillis,
                    60, // in 1 minute segment
                    sleepDepth,
                    eventType,
                    eventMessage,
                    readings);
            sleepSegments.add(sleepSegment);
            lastTimestamp = trackerMotion.timestamp + ONE_MIN_IN_MILLIS;
        }
        LOGGER.debug("Generated {} segments from {} tracker motion samples", sleepSegments.size(), trackerMotions.size());

        return sleepSegments;
    }


    /**
     * Merge consecutive Sleep Segments
     * Naive implementation
     * @param segments
     * @return
     */
    @Timed
    public static List<SleepSegment> mergeConsecutiveSleepSegments(final List<SleepSegment> segments, int threshold) {
        if(segments.isEmpty()) {
            throw new RuntimeException("segments can not be empty");
        }

        final List<SleepSegment> mergedSegments = new ArrayList<>();
        int previousSleepDepth = segments.get(0).sleepDepth;
        String previousEventType = segments.get(0).eventType;

        final List<SleepSegment> buffer = new ArrayList<>();
        float trackAvgDepth = 0F;

        for(final SleepSegment segment : segments) {
            // if(segment.sleepDepth != previousSleepDepth || !previousEventType.equals(segment.eventType)) {
            final float depthDiff = Math.abs((float) segment.sleepDepth - (trackAvgDepth / buffer.size()));
            if (depthDiff > 0.25 *  (trackAvgDepth / buffer.size()) || !previousEventType.equals(segment.eventType)) {
                SleepSegment seg = (buffer.isEmpty()) ? segment : TimelineUtils.merge(buffer, threshold);

                if (mergedSegments.size() > 0) {
                    final SleepSegment prevSegment = mergedSegments.get(mergedSegments.size() - 1);
                    if (seg.eventType.equals(Event.Type.SUNRISE.toString())) {
                        // inherit the sleep depth of previous segment for SUNRISE event
                        int depth = prevSegment.sleepDepth;
                        seg = SleepSegment.withSleepDepth(seg, depth);
                    } else if (seg.sleepDepth == prevSegment.sleepDepth && seg.eventType == prevSegment.eventType) {
                        // add this merged segment with the previous one
                        buffer.clear();
                        buffer.add(prevSegment);
                        buffer.add(seg);
                        mergedSegments.remove(prevSegment);
                        seg = TimelineUtils.merge(buffer, threshold);
                    }
                }


                mergedSegments.add(seg);
                buffer.clear();
                trackAvgDepth = 0F;
            }

            buffer.add(segment);
            trackAvgDepth += (float) segment.sleepDepth;
            previousSleepDepth = segment.sleepDepth;
            previousEventType = segment.eventType;
        }

        if(!buffer.isEmpty()) {
            SleepSegment seg = TimelineUtils.merge(buffer, threshold);
            if (seg.eventType.equals(Event.Type.SUNRISE.toString())) {
                seg = SleepSegment.withSleepDepth(seg, 0);
            }
            mergedSegments.add(seg);
        }
        LOGGER.debug("Original size = {}", segments.size());
        LOGGER.debug("Merged size = {}", mergedSegments.size());
        return mergedSegments;
    }


    /**
     * Categorize sleep depth (MOTION, LIGHT, MEDIUM, DEEP)
     * @param sleepSegments
     * @return
     */
    public static List<SleepSegment> categorizeSleepDepth(final List<SleepSegment> sleepSegments) {
        LOGGER.debug("Attempting to categorize {} segments", sleepSegments.size());
        final List<SleepSegment> normalizedSegments = new ArrayList<>();

        for(final SleepSegment segment : sleepSegments) {
            normalizedSegments.add(SleepSegment.withSleepDepth(segment, TimelineUtils.categorizeSleepDepth(segment.sleepDepth)));
        }
        LOGGER.debug("Categorized {} segments", normalizedSegments.size());
        return normalizedSegments;
    }

    public static Integer categorizeSleepDepth(final Integer sleepDepth) {
        // TODO: tune these
        if (sleepDepth > 70 && sleepDepth <= 100) {
            return 100;
        } else if (sleepDepth > 40 && sleepDepth <= 70) {
            return 70;
        } else if (sleepDepth > 10 && sleepDepth <= 40) {
            return 40;
        } else {
            return 10;
        }
    }

    /**
     * Inserts Sunrise and Sunset segments into the timeline
     * Could potentially work for any kind of event
     * @param sunrise
     * @param sunset
     * @param original
     * @return
     */
    public static List<SleepSegment> insertSegments(final SleepSegment sunrise, final SleepSegment sunset, final List<SleepSegment> original) {

        // add higher-priority segments first
        final Set<SleepSegment> sleepSegments = new TreeSet<>();
        sleepSegments.add(sunrise);
        sleepSegments.add(sunset);
        sleepSegments.addAll(original);

        final List<SleepSegment> temp = new ArrayList<>();

        for(SleepSegment segment : sleepSegments) {
            temp.add(segment);
        }

        return temp;
    }

    public static List<SleepSegment> insertSegmentsWithPriority(final List<SleepSegment> extraSegments, final List<SleepSegment> original) {

        final long startTimestamp = original.get(0).timestamp;
        final Multimap<Long, SleepSegment> extraSegmentMap = ArrayListMultimap.create();
        final List<SleepSegment> temp = new ArrayList<>();

        for (SleepSegment segment : extraSegments) {
            if (segment.timestamp < startTimestamp) {
                temp.add(segment);
            } else {
                extraSegmentMap.put(segment.timestamp, segment);
            }
        }

        for (SleepSegment segment : original) {
            if (extraSegmentMap.containsKey(segment.timestamp)) {
                final Map<String, SleepSegment> eventsMap = new HashMap<>();
                eventsMap.put(segment.eventType, segment);
                for (final SleepSegment extraSegment: extraSegmentMap.get(segment.timestamp)) {
                    eventsMap.put(extraSegment.eventType, extraSegment);
                }

                final String winningEvent = Event.getHighPriorityEvents(eventsMap.keySet());
                temp.add(eventsMap.get(winningEvent));
                // TODO: compose multiple messages for same segment if needed
            }
            else {
                temp.add(segment);
            }
        }
        return temp;
    }

    /**
     * Normalize sleep depth based on max value seen.
     * @param value
     * @param maxValue
     * @return
     */
    public static Integer normalizeSleepDepth(final Integer value, final Long maxValue) {
        int sleepDepth = 100;
        if(value == -1) {
            sleepDepth = 100;
        } else if(value > 0) {
            sleepDepth = 100 - (int) (new Double(value) / maxValue * 100);
            LOGGER.trace("Ratio = ({} / {}) = {}", value, maxValue, (new Double(value) / maxValue * 100));
            LOGGER.trace("Sleep Depth = {}", sleepDepth);
        }
        return sleepDepth;
    }


    /**
     * Compute the night's statistics based on the sleep segments
     * @param segments
     * @return
     */
    public static SleepStats computeStats(final List<SleepSegment> segments) {
        Integer soundSleepDuration = 0;
        Integer lightSleepDuration = 0;
        Integer sleepDuration = 0;
        Integer numberOfMotionEvents = 0;

        for(final SleepSegment segment : segments) {
            if (segment.sleepDepth >= 70) {
                soundSleepDuration += segment.durationInSeconds;
            } else if(segment.sleepDepth > 10 && segment.sleepDepth < 70) {
                lightSleepDuration += segment.durationInSeconds;
            } else {
                numberOfMotionEvents += 1;
            }
            LOGGER.trace("duration in seconds = {}", segment.durationInSeconds);
            sleepDuration += segment.durationInSeconds;
        }


        final Integer soundSleepDurationInMinutes = Math.round(new Float(soundSleepDuration)/60);
        final Integer lightSleepDurationInMinutes = Math.round(new Float(lightSleepDuration)/60);
        final Integer sleepDurationInMinutes = Math.round(new Float(sleepDuration) / 60);

        final SleepStats sleepStats = new SleepStats(soundSleepDurationInMinutes,lightSleepDurationInMinutes,sleepDurationInMinutes,numberOfMotionEvents);
        LOGGER.debug("Sleepstats = {}", sleepStats);

        return sleepStats;
    }


    /**
     * Generate string representation of the sleep stats
     * @param sleepStats
     * @return
     */
    public static String generateMessage(final SleepStats sleepStats) {
        final Integer percentageOfSoundSleep = Math.round(new Float(sleepStats.soundSleepDurationInMinutes) /sleepStats.sleepDurationInMinutes * 100);
        return String.format("You slept for a total of **%d minutes**, soundly for %d minutes (%d%%) and moved %d times",
                sleepStats.sleepDurationInMinutes, sleepStats.soundSleepDurationInMinutes, percentageOfSoundSleep, sleepStats.numberOfMotionEvents);
    }

    public static List<Insight> generateRandomInsights(int seed) {
        final Random r = new Random(seed);
        final List<Insight> insights = new ArrayList<>();

        insights.add(new Insight(Sensor.TEMPERATURE, CurrentRoomState.State.Condition.ALERT, "This reminds me of the time I was locked in the freezer at Dairy Queen."));
        insights.add(new Insight(Sensor.SOUND, CurrentRoomState.State.Condition.IDEAL, "The sound levels were perfect for sleep."));
        insights.add(new Insight(Sensor.HUMIDITY, CurrentRoomState.State.Condition.WARNING, "Humidity was a little too high for ideal sleep conditions"));
        insights.add(new Insight(Sensor.PARTICULATES, CurrentRoomState.State.Condition.IDEAL, "The air quality was ideal"));
        insights.add(new Insight(Sensor.LIGHT, CurrentRoomState.State.Condition.WARNING, "It was a little bright for sleep"));

        final Set<Sensor> sensors = new HashSet<>();
        final List<Insight> generatedInsights = new ArrayList<>();

        final int n = r.nextInt(insights.size());
        LOGGER.trace("n = {}", n);
        for(int i =0; i < n; i++) {
            final int pick = r.nextInt(insights.size());
            final Insight temp = insights.get(pick);
            if(!sensors.contains(temp.sensor)) {
                generatedInsights.add(temp);
                sensors.add(temp.sensor);
            }
        }

        return generatedInsights;
    }

    public static Optional<SleepSegment> computeSleepTime(final List<SleepSegment> sleepSegments, int thresholdInMinutes) {

        if(sleepSegments.isEmpty()) {
            return Optional.absent();
        }

        final List<DateTime> dateTimes = new ArrayList<>();
        Map<Long, SleepSegment> map = new HashMap<>();

        for(SleepSegment sleepSegment : sleepSegments) {
            if(sleepSegment.sleepDepth < 70) {
                dateTimes.add(new DateTime(sleepSegment.timestamp + sleepSegment.offsetMillis));
                map.put(sleepSegment.timestamp + sleepSegment.offsetMillis, sleepSegment);
            }
        }

        for(int i =0; i < dateTimes.size() -1; i++) {
            DateTime current = dateTimes.get(i);
            DateTime next = dateTimes.get(i + 1);
            int diffInMinutes = next.getMinuteOfDay() - current.getMinuteOfDay();
            if (diffInMinutes > thresholdInMinutes) {
//                System.out.println("Diff in minutes = " + (next.getSecondOfDay() - current.getSecondOfDay()));
                if(map.containsKey(current.getMillis())) {
                    SleepSegment s = map.get(current.getMillis());
                    return Optional.of(SleepSegment.withEventType(s, Event.Type.SLEEP));

                }
                break;
            }
        }

        return Optional.absent();
    }
}
