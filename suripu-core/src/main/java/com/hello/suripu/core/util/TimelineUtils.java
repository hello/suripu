package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SensorReading;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.models.TrackerMotion;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class TimelineUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineUtils.class);

    /**
     * Merge a List<Segment> to a single segment
     * The minimum duration is 60 seconds
     * @param segments
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
        final Integer sleepDepth = segments.get(0).sleepDepth;
        final Integer offsetMillis = segments.get(0).offsetMillis;
        final String eventType = segments.get(0).eventType;
        final String message = segments.get(0).message;
        final List<SensorReading> sensors = segments.get(0).sensors;

        Long durationInMillis = segments.get(segments.size() -1).timestamp - segments.get(0).timestamp;


        if(segments.size() == 1 || durationInMillis < threshold * 60000) {
            durationInMillis = threshold * 60000L;
        }
        return new SleepSegment(id, timestamp, offsetMillis, Math.round(durationInMillis / 1000), sleepDepth, eventType, message, sensors);
    }


    /**
     * Generate Sleep Segments from the TrackerMotion data
     * @param trackerMotions
     * @param threshold
     * @param groupBy
     * @return
     */
    @Timed
    public static List<SleepSegment> generateSleepSegments(final List<TrackerMotion> trackerMotions, final int threshold, final int groupBy) {

        return generateSleepSegments(trackerMotions, threshold, groupBy, Optional.<DeviceData>absent());
    }

    public static List<SleepSegment> generateSleepSegments(final List<TrackerMotion> trackerMotions, final int threshold, final int groupBy, final Optional<DeviceData> deviceData) {
        final List<SleepSegment> sleepSegments = new ArrayList<>();


        if(trackerMotions.isEmpty()) {
            return sleepSegments;
        }

        Long maxSVM = 0L;
        for(final TrackerMotion trackerMotion : trackerMotions) {
            maxSVM = Math.max(maxSVM, trackerMotion.value);
        }

        LOGGER.debug("Max SVM = {}", maxSVM);

        int i = 0;
        final Long trackerId = trackerMotions.get(0).trackerId;

        for(final TrackerMotion trackerMotion : trackerMotions) {
            if (!trackerMotion.trackerId.equals(trackerId)) {
                LOGGER.warn("User has multiple pills: {} and {}", trackerId, trackerMotion.trackerId);
                break; // if user has multiple pill, only use data from the latest tracker_id
            }

            int sleepDepth = normalizeSleepDepth(trackerMotion.value, maxSVM);

            String eventType = (sleepDepth <= threshold) ? Event.Type.MOTION.toString() : null; // TODO: put these in a config file or DB

            if(i == 0) {
                eventType = "SLEEP";
            } else if (i == trackerMotions.size() -1) {
                eventType = "WAKE_UP";
            }

            String eventMessage = "";

            if(eventType != null && eventType.equals(Event.Type.MOTION.toString())) {
                eventMessage = String.format("We detected something at %s", new DateTime(trackerMotion.timestamp).toString());
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
                    60 * groupBy, // in seconds
                    sleepDepth,
                    eventType,
                    eventMessage,
                    readings);
            sleepSegments.add(sleepSegment);
            i++;
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

        final List<SleepSegment> buffer = new ArrayList<>();

        for(final SleepSegment segment : segments) {
            if(segment.sleepDepth != previousSleepDepth) {
                final SleepSegment seg = (buffer.isEmpty()) ? segment : TimelineUtils.merge(buffer, threshold);
                mergedSegments.add(seg);
                buffer.clear();
            }

            buffer.add(segment);
            previousSleepDepth = segment.sleepDepth;
        }

        if(!buffer.isEmpty()) {
            mergedSegments.add(TimelineUtils.merge(buffer, threshold));
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

        for(SleepSegment segment : sleepSegments) {
            Integer sleepDepth = segment.sleepDepth;

            if( segment.sleepDepth <=10) {
                sleepDepth = 10;
            } else if(segment.sleepDepth > 10 && segment.sleepDepth <= 40) {
                sleepDepth = 40;
            } else if(segment.sleepDepth > 40 && segment.sleepDepth <= 70) {
                sleepDepth = 70;
            } else if (segment.sleepDepth > 70 && segment.sleepDepth <= 100) {
                sleepDepth = 100;
            }
            normalizedSegments.add(SleepSegment.withSleepDepth(segment, sleepDepth));
        }
        LOGGER.debug("Categorized {} segments", normalizedSegments.size());
        return normalizedSegments;
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


        Integer soundSleepDurationInMinutes = Math.round(new Float(soundSleepDuration)/60);
        Integer lightSleepDurationInMinutes = Math.round(new Float(lightSleepDuration)/60);
        Integer sleepDurationInMinutes = Math.round(new Float(sleepDuration) / 60);

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
        return String.format("You slept for a total of **%d mins**, soundly for %d minutes (%d %%) & moved %d times",
                sleepStats.sleepDurationInMinutes, sleepStats.soundSleepDurationInMinutes, percentageOfSoundSleep, sleepStats.numberOfMotionEvents);
    }
}
