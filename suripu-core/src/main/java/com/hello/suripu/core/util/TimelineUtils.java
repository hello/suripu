package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
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
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkNotNull;

public class TimelineUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineUtils.class);

    public static final Integer HIGHEST_SLEEP_DEPTH = 100;
    public static final Integer HIGH_SLEEP_DEPTH = 80;
    public static final Integer MEDIUM_SLEEP_DEPTH = 60;
    public static final Integer LOW_SLEEP_DEPTH = 30;
    public static final Integer LOWEST_SLEEP_DEPTH = 10;


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

        final Long timestamp = segments.get(0).getTimestamp();
        final Long id = segments.get(0).id;
        float total = 0f;
        for (final SleepSegment segment : segments) {
            total += (float) segment.sleepDepth;
        }
        final Integer sleepDepth = TimelineUtils.categorizeSleepDepth((int) (total / segments.size()));
        final Integer offsetMillis = segments.get(0).timezoneOffset;
        final Event.Type eventType = segments.get(0).type;
        final String message = segments.get(0).getMessage();
        final List<SensorReading> sensors = segments.get(0).sensors;

        Long durationInMillis = segments.get(segments.size() -1).getTimestamp() - segments.get(0).getTimestamp() +  segments.get(segments.size() -1).getDurationInSeconds() * DateTimeConstants.MILLIS_PER_SECOND;


        if (durationInMillis < threshold * DateTimeConstants.MILLIS_PER_MINUTE) {
            durationInMillis = (long)threshold * DateTimeConstants.MILLIS_PER_MINUTE;
        }
        final SleepSegment mergedSegment = new SleepSegment(id, timestamp, offsetMillis, Math.round(durationInMillis / DateTimeConstants.MILLIS_PER_SECOND), sleepDepth, eventType, sensors, null);
        mergedSegment.setMessage(message);
        return mergedSegment;
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

        for(final TrackerMotion trackerMotion : trackerMotions) {
            if (!trackerMotion.trackerId.equals(trackerId)) {
                LOGGER.warn("User has multiple pills: {} and {}", trackerId, trackerMotion.trackerId);
                break; // if user has multiple pill, only use data from the latest tracker_id
            }

            int sleepDepth = normalizeSleepDepth(trackerMotion.value, maxSVM);

            Event.Type eventType = (sleepDepth <= threshold) ? Event.Type.MOTION : Event.Type.NONE; // TODO: put these in a config file or DB

            // TODO: make this work
            if (trackerMotion.value == maxSVM) {
                eventType = Event.Type.MOTION;

            }

            final List<SensorReading> readings = new ArrayList<>();
            if(deviceData.isPresent()) {
                readings.addAll(SensorReading.fromDeviceData(deviceData.get()));
            }

            final SleepSegment sleepSegment = new SleepSegment(
                    trackerMotion.id,
                    trackerMotion.timestamp,
                    trackerMotion.offsetMillis,
                    DateTimeConstants.SECONDS_PER_MINUTE, // in 1 minute segment
                    sleepDepth,
                    eventType,
                    readings,
                    null);
            sleepSegments.add(sleepSegment);
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
    @Deprecated
    public static List<SleepSegment> mergeConsecutiveSleepSegments(final List<SleepSegment> segments, int threshold) {
        if(segments.isEmpty()) {
            throw new RuntimeException("segments can not be empty");
        }

        final List<SleepSegment> mergedSegments = new ArrayList<>();
        int previousSleepDepth = segments.get(0).sleepDepth;
        Event.Type previousEventType = segments.get(0).type;

        final List<SleepSegment> buffer = new ArrayList<>();
        float trackAvgDepth = 0F;

        for(final SleepSegment segment : segments) {
            // if(segment.sleepDepth != previousSleepDepth || !previousEventType.equals(segment.eventType)) {
            final float depthDiff = Math.abs((float) segment.sleepDepth - (trackAvgDepth / buffer.size()));
            if (depthDiff > 0.25 *  (trackAvgDepth / buffer.size()) || !previousEventType.equals(segment.type)) {
                SleepSegment seg = (buffer.isEmpty()) ? segment : TimelineUtils.merge(buffer, threshold);

                if (mergedSegments.size() > 0) {
                    final SleepSegment prevSegment = mergedSegments.get(mergedSegments.size() - 1);
                    if (seg.type.equals(Event.Type.SUNRISE.toString())) {
                        // inherit the sleep depth of previous segment for SUNRISE event
                        int depth = prevSegment.sleepDepth;
                        seg = SleepSegment.withSleepDepth(seg, depth);
                    } else if (seg.sleepDepth == prevSegment.sleepDepth && seg.type == prevSegment.type) {
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
            previousEventType = segment.type;
        }

        if(!buffer.isEmpty()) {
            SleepSegment seg = TimelineUtils.merge(buffer, threshold);
            if (seg.type.equals(Event.Type.SUNRISE.toString())) {
                seg = SleepSegment.withSleepDepth(seg, 0);
            }
            mergedSegments.add(seg);
        }
        LOGGER.debug("Original size = {}", segments.size());
        LOGGER.debug("Merged size = {}", mergedSegments.size());
        return mergedSegments;
    }


    /**
     * Creates time buckets of $windowSize seconds
     * @param segments
     * @param windowSize
     * @return
     */
    public static List<SleepSegment> mergeByTimeBucket(final List<SleepSegment> segments, int windowSize) {
        final List<SleepSegment> sleepSegments = new ArrayList<>();
        final List<SleepSegment> temp = new ArrayList<>();

        for(int i = 0; i < segments.size(); i++) {
            if(i > 0 && i % windowSize == 0) {
                final SleepSegment merged = merge(temp);
                sleepSegments.add(merged);
                temp.clear();
            } else {
                temp.add(segments.get(i));
            }
        }

        if(!temp.isEmpty()) {
            final SleepSegment remainder = merge(temp);
            sleepSegments.add(remainder);
        }

        LOGGER.debug("Merged by bucket size = {}", sleepSegments.size());
        return sleepSegments;
    }


    /**
     * Merges all events into a single event with the most important event type
     * and the timestamp of the first event.
     * @param sleepSegments
     * @return
     */
    public static SleepSegment merge(final List<SleepSegment> sleepSegments) {
        if(sleepSegments.isEmpty()) {
            throw new RuntimeException("Can not merge empty list");
        }

        int minSleepDepth = sleepSegments.get(0).sleepDepth;
        String message = sleepSegments.get(0).getMessage();
        SleepSegment.SoundInfo soundInfo = sleepSegments.get(0).soundInfo;
        final Set<Event.Type> eventTypes = new HashSet<>();

        for(final SleepSegment sleepSegment : sleepSegments) {
            eventTypes.add(sleepSegment.type);
            if(sleepSegment.sleepDepth < minSleepDepth) {
                minSleepDepth = sleepSegment.sleepDepth;
                message  = sleepSegment.getMessage();
            }

            if(sleepSegment.type.equals(Event.Type.SUNRISE)) {
                soundInfo = sleepSegment.soundInfo;
            }
        }


        final Event.Type finalEventType = Event.getHighPriorityEvents(eventTypes);
        final SleepSegment sleepSegment = new SleepSegment(
                sleepSegments.get(0).id,
                sleepSegments.get(0).getTimestamp(),
                sleepSegments.get(0).timezoneOffset,
                sleepSegments.size() * 60, minSleepDepth,
                finalEventType,
                new ArrayList<SensorReading>(),
                soundInfo
        );

        sleepSegment.setMessage(message);

        return sleepSegment;
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


    /**
     * Harmonizes sleep depth into n buckets
     * @param sleepDepth
     * @return
     */
    public static Integer categorizeSleepDepth(final Integer sleepDepth) {
        // TODO: tune these
        if (sleepDepth > 90 && sleepDepth <= 100) {
            return HIGHEST_SLEEP_DEPTH;
        } else if(sleepDepth > 70 && sleepDepth <= 90) {
            return HIGH_SLEEP_DEPTH;
        } else if (sleepDepth > 40 && sleepDepth <= 70) {
            return MEDIUM_SLEEP_DEPTH;
        } else if (sleepDepth > 10 && sleepDepth <= 40) {
            return LOW_SLEEP_DEPTH;
        }

        return LOWEST_SLEEP_DEPTH;
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


    public static List<SleepSegment> generateAlignedSegmentsByTypeWeight(final List<SleepSegment> segmentList,
                                                                         int slotDurationMS, int mergeSlotCount,
                                                                         boolean collapseNullSegments){
        // Step 1: Get the start and end time of the given segment list
        long startTimestamp = Long.MAX_VALUE;
        long endTimestamp = 0;
        int startOffsetMillis = 0;
        int endOffsetMillis = 0;

        for(final SleepSegment sleepSegment:segmentList){
            if(sleepSegment.startTimestamp < startTimestamp){
                startTimestamp = sleepSegment.startTimestamp;
                startOffsetMillis = sleepSegment.timezoneOffset;
            }

            if(sleepSegment.endTimestamp > endTimestamp){
                endTimestamp = sleepSegment.endTimestamp;
                endOffsetMillis = sleepSegment.timezoneOffset;
            }
        }

        if(startTimestamp == 0 || endTimestamp == 0){
            return Collections.EMPTY_LIST;
        }

        // Step 2: Generate one minute segment slots range from startTimestamp to endTimestamp
        // Time: | min 1 | min 2 | min 3 | min 4 | ... | min N |
        // Type: | none  | none  | none  | none  | ... | none  |
        // These slots are set to type none and will be override later.
        int interval = (int)(endTimestamp - startTimestamp);
        int slotCount = interval / slotDurationMS;
        if(interval % slotDurationMS > 0){
            slotCount++;
        }

        final LinkedHashMap<DateTime, SleepSegment> slots = new LinkedHashMap<>();
        for(int i = 0; i < slotCount; i++){
            final long slotStartTimestamp = startTimestamp + i * slotDurationMS;
            slots.put(new DateTime(slotStartTimestamp, DateTimeZone.UTC), new SleepSegment(slotStartTimestamp,
                    slotStartTimestamp, startOffsetMillis, slotDurationMS / DateTimeConstants.MILLIS_PER_SECOND,
                    100,
                    Event.Type.NONE,
                    null,
                    null
                    ));
        }

        // Step 3: Scan through segmentList, fill slots with highest weight event and their messages.
        // Example:
        // segmentList: | Sleep       |
        //                     |    motion    |
        //                                    |none  |
        //                                           | none |
        //                                                  | Wakeup |

        // empty slots: | none | none | none  | none | none | none   |
        // After scan:  |sleep | sleep| motion| none | none | Wakeup |
        for(final SleepSegment sleepSegment:segmentList){
            int startSlotIndex = (int)(sleepSegment.startTimestamp - startTimestamp) / slotDurationMS;
            long startSlotKey = startTimestamp + startSlotIndex * slotDurationMS;

            int endSlotIndex = (int)(sleepSegment.endTimestamp - startTimestamp) / slotDurationMS;
            if(endSlotIndex > 0){
                endSlotIndex--;
            }


            long endSlotKey = startTimestamp + endSlotIndex * slotDurationMS;

            for(int i = 0; i < endSlotIndex - startSlotIndex + 1; i++){
                final DateTime objectSlotKey = new DateTime(startSlotKey + i * slotDurationMS, DateTimeZone.UTC);
                if(!slots.containsKey(objectSlotKey)){
                    LOGGER.warn("Cannot find key: {}, end {}", objectSlotKey, new DateTime(endSlotKey, DateTimeZone.UTC));
                    continue;
                }

                final SleepSegment currentSlot = slots.get(objectSlotKey);
                if(currentSlot.type.getValue() < sleepSegment.type.getValue()){
                    // Replace the current segment in that slot with higher weight segment
                    final SleepSegment replaceSegment = new SleepSegment(objectSlotKey.getMillis(),
                            objectSlotKey.getMillis(), sleepSegment.timezoneOffset, currentSlot.getDurationInSeconds(),
                            sleepSegment.sleepDepth,
                            sleepSegment.type,
                            sleepSegment.sensors,
                            sleepSegment.soundInfo
                            );
                    slots.put(objectSlotKey, replaceSegment);
                    LOGGER.trace("{} replaced to {}", currentSlot.type, sleepSegment.type);
                }
            }
        }

        // Step 4: merge slots by mergeSlotCount param
        // Example:
        // mergeSlotCount = 3
        // slots:         | low | low | high | low | low | low | high | low | none | none | none |
        // merged slots:  |       high       |       low       |       high        |    none     |
        final LinkedList<SleepSegment> mergeSlots = new LinkedList<>();
        int slotIndex = 0;
        long startSlotKey = -1;
        SleepSegment finalSegment = null;
        SleepSegment currentSegment = null;
        LOGGER.debug("Slots before merge {}", slots.size());
        int minSleepDepth = Integer.MAX_VALUE;
        for(final DateTime slotStartTimestamp:slots.keySet()){  // Iterate though a linkedHashMap will preserve the insert order
            currentSegment = slots.get(slotStartTimestamp);
            //LOGGER.trace(currentSegment.toString());
            if(startSlotKey == -1){
                startSlotKey = slotStartTimestamp.getMillis();
                finalSegment = currentSegment;
            }


            if(finalSegment.type.getValue() < currentSegment.type.getValue()){
                finalSegment = currentSegment;
            }

            if(currentSegment.sleepDepth < minSleepDepth){
                minSleepDepth = currentSegment.sleepDepth;
            }

            slotIndex++;

            if(slotIndex % (mergeSlotCount - 1) == 0){
                final SleepSegment mergedSegment = new SleepSegment(finalSegment.id,
                        startSlotKey,
                        finalSegment.timezoneOffset,
                        (int)(currentSegment.startTimestamp - startSlotKey) / DateTimeConstants.MILLIS_PER_SECOND,
                        minSleepDepth,
                        finalSegment.type,
                        finalSegment.sensors,
                        finalSegment.soundInfo);
                LOGGER.trace(mergedSegment.toString());
                mergedSegment.setMessage(finalSegment.getMessage());
                
                if(collapseNullSegments && mergedSegment.type == Event.Type.NONE){
                    // Do nothing, collapse this event
                    LOGGER.trace("None slot skipped {}", new DateTime(mergedSegment.startTimestamp,
                            DateTimeZone.forOffsetMillis(mergedSegment.timezoneOffset)));
                }else {
                    mergeSlots.add(mergedSegment);
                }

                // reset
                startSlotKey = -1;
                finalSegment = null;
                minSleepDepth = Integer.MAX_VALUE;
            }
        }

        // Handle the dangling case
        if(startSlotKey != -1){
            final SleepSegment mergedSegment = new SleepSegment(finalSegment.id,
                    startSlotKey,
                    finalSegment.timezoneOffset,
                    (int)(currentSegment.getTimestamp() - startSlotKey) / DateTimeConstants.MILLIS_PER_SECOND,
                    finalSegment.sleepDepth,
                    finalSegment.type,
                    finalSegment.sensors,
                    finalSegment.soundInfo);
            LOGGER.trace(mergedSegment.toString());
            if(collapseNullSegments && mergedSegment.type == Event.Type.NONE){
                // Do nothing, collapse this event
            }else {
                mergeSlots.add(mergedSegment);
            }
        }

        LOGGER.trace("Slots after merge {}", mergeSlots.size());

        return ImmutableList.copyOf(mergeSlots);

    }

    public static List<SleepSegment> insertSegmentsWithPriority(final List<SleepSegment> extraSegments, final List<SleepSegment> original) {

        final long startTimestamp = original.get(0).getTimestamp();
        final Multimap<Long, SleepSegment> extraSegmentMap = ArrayListMultimap.create();
        final List<SleepSegment> temp = new ArrayList<>();

        for (final SleepSegment segment : extraSegments) {
            if (segment.getTimestamp() < startTimestamp) {
                temp.add(segment);
            } else {
                extraSegmentMap.put(segment.getTimestamp(), segment);
            }
        }

        for (final SleepSegment segment : original) {
            if (extraSegmentMap.containsKey(segment.getTimestamp())) {
                final Map<Event.Type, SleepSegment> eventsMap = new HashMap<>();
                eventsMap.put(segment.type, segment);
                for (final SleepSegment extraSegment: extraSegmentMap.get(segment.getTimestamp())) {
                    eventsMap.put(extraSegment.type, extraSegment);
                }

                final Event.Type winningEvent = Event.getHighPriorityEvents(eventsMap.keySet());
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
                soundSleepDuration += segment.getDurationInSeconds();
            } else if(segment.sleepDepth > 10 && segment.sleepDepth < 70) {
                lightSleepDuration += segment.getDurationInSeconds();
            } else {
                numberOfMotionEvents += 1;
            }
            LOGGER.trace("duration in seconds = {}", segment.getDurationInSeconds());
            sleepDuration += segment.getDurationInSeconds();
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
        final double sleepDurationInHours = sleepStats.sleepDurationInMinutes / 60.0;
        final double soundDurationInHours = sleepStats.soundSleepDurationInMinutes / 60.0;
        return String.format("You slept for a total of **%.1f hours**, soundly for %.1f hours, (%d%%) and moved %d times",
                sleepDurationInHours, soundDurationInHours, percentageOfSoundSleep, sleepStats.numberOfMotionEvents);
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


    /**
     * Naive implementation of computing sleep time based on motion data only
     * @param sleepSegments
     * @param thresholdInMinutes
     * @return
     */
    public static Optional<SleepSegment> computeSleepTime(final List<SleepSegment> sleepSegments, int thresholdInMinutes) {

        if(sleepSegments.isEmpty()) {
            return Optional.absent();
        }

        final List<DateTime> dateTimes = new ArrayList<>();
        final Map<Long, SleepSegment> map = new HashMap<>();

        for(final SleepSegment sleepSegment : sleepSegments) {
            if(sleepSegment.sleepDepth < 70) {
                dateTimes.add(new DateTime(sleepSegment.getTimestamp() + sleepSegment.timezoneOffset));
                map.put(sleepSegment.getTimestamp() + sleepSegment.timezoneOffset, sleepSegment);
            }
        }

        for(int i =0; i < dateTimes.size() -1; i++) {
            final DateTime current = dateTimes.get(i);
            final DateTime next = dateTimes.get(i + 1);
            final int diffInMinutes = next.getMinuteOfDay() - current.getMinuteOfDay();
            if (diffInMinutes > thresholdInMinutes) {
                if(map.containsKey(current.getMillis())) {
                    final SleepSegment s = map.get(current.getMillis());
                    return Optional.of(SleepSegment.withEventType(s, Event.Type.SLEEP));

                }
                break;
            }
        }

        return Optional.absent();
    }
}
