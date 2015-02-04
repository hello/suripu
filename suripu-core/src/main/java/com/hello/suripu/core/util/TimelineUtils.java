package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.LightSegment;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.sensordata.LightEventsDetector;
import com.hello.suripu.algorithm.sleep.MotionScoreAlgorithm;
import com.hello.suripu.algorithm.sleep.scores.AmplitudeDataScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.LightOutCumulatedMotionMixScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.LightOutScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.MotionDensityScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.SleepDataScoringFunction;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.CurrentRoomState;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.InBedEvent;
import com.hello.suripu.core.models.Events.LightEvent;
import com.hello.suripu.core.models.Events.LightsOutEvent;
import com.hello.suripu.core.models.Events.MotionEvent;
import com.hello.suripu.core.models.Events.NullEvent;
import com.hello.suripu.core.models.Events.OutOfBedEvent;
import com.hello.suripu.core.models.Events.SleepEvent;
import com.hello.suripu.core.models.Events.WakeupEvent;
import com.hello.suripu.core.models.Insight;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TimelineUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineUtils.class);

    public static final Integer HIGHEST_SLEEP_DEPTH = 100;
    public static final Integer HIGH_SLEEP_DEPTH = 80;
    public static final Integer MEDIUM_SLEEP_DEPTH = 60;
    public static final Integer LOW_SLEEP_DEPTH = 30;
    public static final Integer LOWEST_SLEEP_DEPTH = 10;
    public static final long MINUTE_IN_MILLIS = 60000;

    private static final long PRESLEEP_WINDOW_IN_MILLIS = 900000; // 15 mins
    private static final int LIGHTS_OUT_START_THRESHOLD = 19; // 7pm local time
    private static final int LIGHTS_OUT_END_THRESHOLD = 4; // 4am local time



    public static List<Event> convertLightMotionToNone(final List<Event> eventList, final int thresholdSleepDepth){
        final LinkedList<Event> convertedEvents = new LinkedList<>();
        for(final Event event:eventList){
            if(event.getType() == Event.Type.MOTION && event.getSleepDepth() > thresholdSleepDepth){
                final NullEvent nullEvent = new NullEvent(event.getStartTimestamp(),
                        event.getEndTimestamp(),
                        event.getTimezoneOffset(),
                        event.getSleepDepth());
                convertedEvents.add(nullEvent);
            }else{
                convertedEvents.add(event);
            }
        }

        return ImmutableList.copyOf(convertedEvents);
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

    public static List<TrackerMotion> removeNegativeAmplitudes(final List<TrackerMotion> trackerMotions){
        final List<TrackerMotion> positiveMotions = new LinkedList<>();
        for(final TrackerMotion motion:trackerMotions){
            if(motion.value > 0){
                positiveMotions.add(motion);
            }
        }

        return positiveMotions;
    }

    public static List<MotionEvent> generateMotionEvents(final List<TrackerMotion> trackerMotions) {
        final List<MotionEvent> motionEvents = new ArrayList<>();

        final List<TrackerMotion> positiveMotions = removeNegativeAmplitudes(trackerMotions);
        if(positiveMotions.isEmpty()) {
            return motionEvents;
        }

        int maxSVM = getMaxSVM(positiveMotions);
        final Map<Integer, Integer> positionMap = constructValuePositionMap(positiveMotions);

        LOGGER.debug("Max SVM = {}", maxSVM);

        final Long trackerId = positiveMotions.get(0).trackerId;
        for(final TrackerMotion trackerMotion : positiveMotions) {
            if (!trackerMotion.trackerId.equals(trackerId)) {
                LOGGER.warn("User has multiple pills: {} and {}", trackerId, trackerMotion.trackerId);
                break; // if user has multiple pill, only use data from the latest tracker_id
            }

            final MotionEvent motionEvent = new MotionEvent(
                    trackerMotion.timestamp,
                    trackerMotion.timestamp + DateTimeConstants.MILLIS_PER_MINUTE,
                    trackerMotion.offsetMillis,
                    getSleepDepth(trackerMotion.value, positionMap, maxSVM));
            motionEvents.add(motionEvent);
        }
        LOGGER.debug("Generated {} segments from {} tracker motion samples", motionEvents.size(), trackerMotions.size());

        return motionEvents;
    }

    public static Integer getSleepDepth(final Integer amplitude, final Map<Integer, Integer> positionMap, final Integer maxSVM){
        if(positionMap.size() < 5){
            return normalizeSleepDepth(amplitude, maxSVM);
        }

        if(positionMap.containsKey(amplitude)){
            return positionMap.get(amplitude);
        }

        return normalizeSleepDepth(amplitude, maxSVM);
    }

    public static Integer getMaxSVM(final List<TrackerMotion> amplitudes){
        int maxSVM = 0;
        for(final TrackerMotion trackerMotion : amplitudes) {
            maxSVM = Math.max(maxSVM, trackerMotion.value);
        }

        return maxSVM;
    }

    public static Map<Integer, Integer> constructValuePositionMap(final List<TrackerMotion> amplitudes){

        final Integer[] sortedSVMs = new Integer[amplitudes.size()];

        int index = 0;
        for(final TrackerMotion trackerMotion:amplitudes){
            sortedSVMs[index] = trackerMotion.value;
            ++index;
        }
        Arrays.sort(sortedSVMs);

        final Map<Integer, Integer> positionIndex = new HashMap<>();
        int position = 1;

        for(final Integer value : sortedSVMs) {
            positionIndex.put(value, 100 - position * 100 / amplitudes.size());
            position++;
        }

        return positionIndex;
    }

    public static List<SleepSegment> eventsToSegments(final List<Event> events){
        final List<SleepSegment> segments = new ArrayList<>();
        for(final Event event:events){
            segments.add(new SleepSegment((long)segments.size(), event, Collections.EMPTY_LIST));
        }

        return ImmutableList.copyOf(segments);

    }

    public static List<Event> removeMotionEventsOutsideBedPeriod(final List<Event> events){
        boolean isInBed = false;
        final LinkedList<Event> newEventList = new LinkedList<>();
        for(final Event event:events){
            if(isInBed == false && event.getType() == Event.Type.IN_BED){
                isInBed = true;
            }

            if(isInBed && event.getType() == Event.Type.OUT_OF_BED){
                isInBed = false;
            }

            if(isInBed == false && event.getType() == Event.Type.MOTION){
                newEventList.add(new NullEvent(event.getStartTimestamp(), event.getEndTimestamp(), event.getTimezoneOffset(), event.getSleepDepth()));
            }else{
                newEventList.add(event);
            }
        }

        return newEventList;
    }


    public static List<Event> insertOneMinuteDurationEvents(final List<Event> eventList, final Event sleepEvent){
        final ArrayList<Event> result =  new ArrayList<>();
        boolean inserted = false;
        for(final Event event:eventList){
            if(!(event.getStartTimestamp() <= sleepEvent.getStartTimestamp() &&
                    event.getEndTimestamp() >= sleepEvent.getEndTimestamp())) {
                result.add(event);
                continue;
            }
            final long startToHere = sleepEvent.getStartTimestamp() - event.getStartTimestamp();
            final long hereToEnd = event.getEndTimestamp() - sleepEvent.getEndTimestamp();

            if(startToHere == 0 && hereToEnd == 0){
                if(sleepEvent.getType().getValue() > event.getType().getValue()) {
                    LOGGER.debug("Replace {} event by {} event", event.getType(), sleepEvent.getType());
                    result.add(sleepEvent);
                    inserted = true;
                    continue;
                }
                result.add(event);
                continue;
            }

            // s > 0, e > 0
            // s = 0, e > 0
            // s > 0, e = 0
            if(startToHere > 0 && hereToEnd > 0){
                result.add(Event.extend(event, event.getStartTimestamp() + startToHere, sleepEvent.getStartTimestamp(), event.getSleepDepth()));
                result.add(sleepEvent);
                inserted = true;
                result.add(Event.extend(event, sleepEvent.getEndTimestamp(), event.getEndTimestamp(), event.getSleepDepth()));
                continue;
            }

            // s = 0, e > 0
            // s > 0, e = 0
            if(startToHere == 0 && hereToEnd > 0){
                result.add(sleepEvent);
                inserted = true;
                result.add(Event.extend(event, sleepEvent.getEndTimestamp(), event.getEndTimestamp(), event.getSleepDepth()));
                continue;
            }

            // s > 0, e = 0
            if(startToHere > 0 && hereToEnd == 0){
                result.add(Event.extend(event, event.getStartTimestamp() + startToHere, sleepEvent.getStartTimestamp(), event.getSleepDepth()));
                result.add(sleepEvent);
                inserted = true;
                continue;
            }

        }

        if(!inserted){
            if(sleepEvent.getEndTimestamp() <= eventList.get(0).getStartTimestamp()){
                result.add(0, sleepEvent);
            }

            if(sleepEvent.getStartTimestamp() >= eventList.get(eventList.size() - 1).getEndTimestamp()){
                result.add(sleepEvent);
            }
        }
        return result;
    }

    public static List<Event> smoothEvents(final List<Event> eventList){
        if(eventList.size() == 0){
            return eventList;
        }

        final ArrayList<Event> result = new ArrayList<>();
        Event firstEventOfThatType = null;
        Event lastEventOfThatType = null;
        int minSleepDepth = Integer.MAX_VALUE;

        for(final Event event:eventList){
            if(event.getType() == Event.Type.MOTION){
                if(firstEventOfThatType == null){
                    firstEventOfThatType = event;
                    lastEventOfThatType = event;
                    minSleepDepth = event.getSleepDepth();
                    continue;
                }

                lastEventOfThatType = event;
                if(event.getSleepDepth() < minSleepDepth){
                    minSleepDepth = event.getSleepDepth();
                }
                continue;
            }

            if(lastEventOfThatType != null){
                final Event smoothedEvent = Event.extend(firstEventOfThatType, firstEventOfThatType.getStartTimestamp(), lastEventOfThatType.getEndTimestamp(), minSleepDepth);
                result.add(smoothedEvent);
            }
            result.add(event);
            firstEventOfThatType = null;
            lastEventOfThatType = null;
            minSleepDepth = Integer.MAX_VALUE;
        }

        if(lastEventOfThatType != null){
            final Event smoothedEvent = Event.extend(firstEventOfThatType, firstEventOfThatType.getStartTimestamp(), lastEventOfThatType.getEndTimestamp(), minSleepDepth);
            result.add(smoothedEvent);
        }

        return result;
    }

    public static List<Event> generateAlignedSegmentsByTypeWeight(final List<Event> eventList,
                                                                         int slotDurationMS, int mergeSlotCount,
                                                                         boolean collapseNullSegments){
        // Step 1: Get the start and end time of the given segment list
        long startTimestamp = Long.MAX_VALUE;
        long endTimestamp = 0;
        int startOffsetMillis = 0;
        int endOffsetMillis = 0;

        for(final Event event:eventList){
            if(event.getStartTimestamp() < startTimestamp){
                startTimestamp = event.getStartTimestamp();
                startOffsetMillis = event.getTimezoneOffset();
            }

            if(event.getEndTimestamp() > endTimestamp){
                endTimestamp = event.getEndTimestamp();
                endOffsetMillis = event.getTimezoneOffset();
            }
        }

        if(startTimestamp == 0 || endTimestamp == 0){
            return Collections.EMPTY_LIST;
        }

        // Step 2: Generate one minute segment slots range from startTimestamp to endTimestamp
        // Time:   min 1   min 2   min 3   min 4   ...   min N
        // Slots: | none  | none  | none  | none  | ... | none  |
        // These slots are set to type none and will be override later.
        int interval = (int)(endTimestamp - startTimestamp);
        int slotCount = interval / slotDurationMS;
        if(interval % slotDurationMS > 0){
            slotCount++;
        }

        final LinkedHashMap<DateTime, Event> slots = new LinkedHashMap<>();
        for(int i = 0; i < slotCount; i++){
            final long slotStartTimestamp = startTimestamp + i * slotDurationMS;
            slots.put(new DateTime(slotStartTimestamp, DateTimeZone.UTC),
                    new NullEvent(slotStartTimestamp,
                        slotStartTimestamp + slotDurationMS,
                        startOffsetMillis,
                        100
                    ));
        }

        // Step 3: Scan through segmentList, fill slots with highest weight event and their messages.
        // Example:
        // Time:         | min 1 | min2 | min3 | min4 | min5 |  min6  |
        // segmentList:  |     Sleep    |
        //                      |    motion    |
        //                                     |none  |
        //                                            | none |
        //                                                   | Wakeup |
        // slots before: | none | none | none  | none | none | none   |
        // slots after:  |sleep | sleep| motion| none | none | Wakeup |
        for(final Event event:eventList){
            int startSlotIndex = (int)(event.getStartTimestamp() - startTimestamp) / slotDurationMS;
            long startSlotKey = startTimestamp + startSlotIndex * slotDurationMS;

            int endSlotIndex = (int)(event.getEndTimestamp() - startTimestamp) / slotDurationMS;
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

                final Event currentSlot = slots.get(objectSlotKey);
                if(currentSlot.getType().getValue() < event.getType().getValue()){
                    // Replace the current segment in that slot with higher weight segment
                    final Event replaceEvent = Event.extend(event, objectSlotKey.getMillis(), objectSlotKey.getMillis() + slotDurationMS);
                    slots.put(objectSlotKey, replaceEvent);
                    //LOGGER.debug("{} replaced to {}", currentSlot.getType(), event.getType());
                }
            }
        }

        // Step 4: merge slots by mergeSlotCount param
        // Example:
        // mergeSlotCount = 3
        // slots:         | low | low | high | low | low | low | high | low | none | none | none |
        // merged slots:  |       high       |       low       |       high        |    none     |
        final LinkedList<Event> mergeSlots = new LinkedList<>();
        int count = 0;
        long startSlotKey = 0;
        Event finalEvent = null;
        Event currentEvent = null;
        LOGGER.debug("Slots before merge {}", slots.size());
        int minSleepDepth = 0;
        for(final DateTime slotStartTimestamp:slots.keySet()){  // Iterate though a linkedHashMap will preserve the insert order
            currentEvent = slots.get(slotStartTimestamp);
            //LOGGER.trace(currentSegment.toString());
            if(count == 0){
                startSlotKey = slotStartTimestamp.getMillis();
                finalEvent = currentEvent;
                minSleepDepth = currentEvent.getSleepDepth();
            }


            if(finalEvent.getType().getValue() < currentEvent.getType().getValue()){
                finalEvent = currentEvent;
            }

            if(minSleepDepth > currentEvent.getSleepDepth()){
                minSleepDepth = currentEvent.getSleepDepth();
            }

            count++;

            if(count == mergeSlotCount){
                final Event mergedEvent = Event.extend(finalEvent, startSlotKey, currentEvent.getEndTimestamp(), minSleepDepth);
                if(collapseNullSegments && mergedEvent.getType() == Event.Type.NONE){
                    // Do nothing, collapse this event
                    LOGGER.trace("None slot skipped {}", new DateTime(mergedEvent.getStartTimestamp(),
                            DateTimeZone.forOffsetMillis(mergedEvent.getTimezoneOffset())));
                }else {
                    mergeSlots.add(mergedEvent);
                }

                // reset
                count = 0;
                minSleepDepth = 0;
            }
        }

        // Handle the dangling case
        if(count > 0){
            final Event mergedEvent = Event.extend(finalEvent,
                    startSlotKey,
                    currentEvent.getEndTimestamp(),
                    minSleepDepth);
            LOGGER.trace(mergedEvent.toString());
            if(collapseNullSegments && mergedEvent.getType() == Event.Type.NONE){
                // Do nothing, collapse this event
            }else {
                mergeSlots.add(mergedEvent);
            }
        }

        LOGGER.trace("Slots size after merge {}", mergeSlots.size());

        return ImmutableList.copyOf(mergeSlots);

    }


    /**
     * Normalize sleep depth based on max value seen.
     * @param value
     * @param maxValue
     * @return
     */
    public static Integer normalizeSleepDepth(final int value, int maxValue) {

        int sleepDepth = 100;
        if (value < 0) {
            sleepDepth = 100;
        } else if (value > 0) {
            int percentage = value * 100 / maxValue;
            sleepDepth = 100 - percentage;
            LOGGER.trace("Absolute Ratio = ({} / {}) = {}", value, maxValue, percentage);
            LOGGER.trace("Absolute Sleep Depth = {}", sleepDepth);
        }
        return sleepDepth;


    }


    /**
     * Compute the night's statistics based on the sleep segments
     * @param segments
     * @return
     */
    public static SleepStats computeStats(final List<SleepSegment> segments, final int lightSleepThreshold) {
        Integer soundSleepDuration = 0;
        Integer lightSleepDuration = 0;
        int sleepDurationInSecs = 0;
        int inBedDurationInSecs = 0;
        Integer numberOfMotionEvents = 0;
        long sleepTime = 0L;
        Long wakeTime = 0L;
        Integer sleepOnsetTimeMinutes = 0;
        long inBedTime = 0L;

        boolean sleepStarted = false;
        boolean inBedStarted = false;

        for(final SleepSegment segment : segments) {
            if (segment.getType() == Event.Type.IN_BED) {
                inBedTime = segment.getTimestamp();
                inBedStarted = true;
            }

            if(segment.getType() == Event.Type.SLEEP){
                sleepStarted = true;
                sleepTime = segment.getTimestamp();
            }

            if(segment.getType() == Event.Type.WAKE_UP && sleepStarted){  //On purpose dangling case, if no wakeup present
                sleepStarted = false;
                sleepDurationInSecs = (int) (segment.getTimestamp() - sleepTime) / DateTimeConstants.MILLIS_PER_SECOND;
            }

            if(segment.getType() == Event.Type.OUT_OF_BED && inBedStarted){
                inBedStarted = false;
                inBedDurationInSecs = (int) (segment.getTimestamp() - inBedTime) / DateTimeConstants.MILLIS_PER_SECOND;
            }

            if(!sleepStarted){
                continue;
            }
            if (segment.getSleepDepth() >= lightSleepThreshold) {
                soundSleepDuration += segment.getDurationInSeconds();
            } else if(segment.getSleepDepth() >= 0 && segment.getSleepDepth() < lightSleepThreshold) {
                lightSleepDuration += segment.getDurationInSeconds();
            }

            if(segment.getType() == Event.Type.MOTION){
                numberOfMotionEvents++;
            }
            LOGGER.trace("duration in seconds = {}", segment.getDurationInSeconds());
        }


        final Integer soundSleepDurationInMinutes = Math.round(new Float(soundSleepDuration) / DateTimeConstants.SECONDS_PER_MINUTE);
        final Integer lightSleepDurationInMinutes = Math.round(new Float(lightSleepDuration) / DateTimeConstants.SECONDS_PER_MINUTE);
        final Integer sleepDurationInMinutes = Math.round(new Float(sleepDurationInSecs) / DateTimeConstants.SECONDS_PER_MINUTE);
        final Integer inBedDurationInMinutes = Math.round(new Float(inBedDurationInSecs) / DateTimeConstants.SECONDS_PER_MINUTE);

        if (inBedTime > 0 && inBedTime < sleepTime) {
            sleepOnsetTimeMinutes = (int) ((sleepTime - inBedTime)/MINUTE_IN_MILLIS);
        }

        final SleepStats sleepStats = new SleepStats(soundSleepDurationInMinutes,
                lightSleepDurationInMinutes,
                sleepDurationInMinutes == 0 ? inBedDurationInMinutes : sleepDurationInMinutes,
                numberOfMotionEvents,
                sleepTime,
                wakeTime,
                sleepOnsetTimeMinutes
        );
        LOGGER.debug("Sleepstats = {}", sleepStats);

        return sleepStats;
    }


    /**
     * Generate string representation of the sleep stats
     * @param sleepStats
     * @return
     */
    public static String generateMessage(final SleepStats sleepStats, final Boolean reportSleepDuration) {
        final Integer percentageOfSoundSleep = Math.round(new Float(sleepStats.soundSleepDurationInMinutes) /sleepStats.sleepDurationInMinutes * 100);
        final double sleepDurationInHours = sleepStats.sleepDurationInMinutes / (double)DateTimeConstants.MINUTES_PER_HOUR;
        final double soundDurationInHours = sleepStats.soundSleepDurationInMinutes / (double)DateTimeConstants.MINUTES_PER_HOUR;

        if (reportSleepDuration) {
            // report sleep duration
            return String.format("You were asleep for **%.1f hours**, and sleeping soundly for %.1f hours.",
                    sleepDurationInHours, soundDurationInHours);
        }

        // report in-bed time
        return String.format("You were in bed for **%.1f hours**", sleepDurationInHours);

    }

    public static List<Insight> generatePreSleepInsights(Optional<AllSensorSampleList> optionalSensorData, final long sleepTimestampUTC) {

        if (!optionalSensorData.isPresent()) {
            return Collections.EMPTY_LIST;
        }

        final List<Insight> generatedInsights = new ArrayList<>();

        final AllSensorSampleList sensorData = optionalSensorData.get();

        // find index for time period of interest
        int startIndex = 0;
        int endIndex = 0;
        final long startTimestamp = sleepTimestampUTC - PRESLEEP_WINDOW_IN_MILLIS;
        for (Sample sample : sensorData.getData(Sensor.LIGHT)) {
            if (sample.dateTime < startTimestamp) {
                startIndex++;
            }

            endIndex++;

            if (sample.dateTime > sleepTimestampUTC) {
                break;
            }
        }

        // initialize
        Map<Sensor, Float> counts = new HashMap<>();
        Map<Sensor, Float> sums = new HashMap<>();
        for (Sensor sensor : sensorData.getAvailableSensors()) {
            counts.put(sensor, 0.0f);
            sums.put(sensor, 0.0f);
        }

        // add values
        for (int i = startIndex; i < endIndex; i++) {
            for (Sensor sensor : sums.keySet()) {
                final float average = sums.get(sensor) + sensorData.getData(sensor).get(i).value;
                sums.put(sensor, average);

                final float count = counts.get(sensor) + 1.0f;
                counts.put(sensor, count);
            }
        }

        final DateTime sleepDateTime = new DateTime(sleepTimestampUTC, DateTimeZone.UTC);

        // compute average for each sensor, generate insights
        for (Sensor sensor : counts.keySet()) {
            if (counts.get(sensor) == 0.0f) {
                continue;
            }

            final float average = sums.get(sensor) / counts.get(sensor);
            Optional<CurrentRoomState.State> sensorState;

            switch(sensor) {
                case LIGHT:
                    sensorState = Optional.of(CurrentRoomState.getLightState(average, sleepDateTime, true));
                    break;
                case SOUND:
                    sensorState = Optional.of(CurrentRoomState.getSoundState(average, sleepDateTime, true));
                    break;
                case HUMIDITY:
                    sensorState = Optional.of(CurrentRoomState.getHumidityState(average, sleepDateTime, true));
                    break;
                case TEMPERATURE:
                    sensorState = Optional.of(CurrentRoomState.getTemperatureState(average, sleepDateTime, CurrentRoomState.DEFAULT_TEMP_UNIT, true));
                    break;
                case PARTICULATES:
                    sensorState = Optional.of(CurrentRoomState.getParticulatesState(average, sleepDateTime, true));
                    break;
                default:
                    sensorState = Optional.absent();
                    break;
            }

            if (sensorState.isPresent()) {
                generatedInsights.add(new Insight(sensor, sensorState.get().condition, sensorState.get().message));
            }
        }

        return generatedInsights;
    }


    /**
     * Naive implementation of computing sleep time based on motion data only
     * @param sleepMotions
     * @param thresholdInMinutes
     * @return
     */
    public static Optional<SleepEvent> getSleepEvent(final List<MotionEvent> sleepMotions, int thresholdInMinutes, int motionThreshold, final Optional<DateTime> sleepTimeThreshold) {

        if(sleepMotions.isEmpty()) {
            return Optional.absent();
        }

        final List<DateTime> dateTimes = new ArrayList<>();
        final Map<Long, MotionEvent> map = new HashMap<>();

        // convert local_UTC to UTC
        DateTime sleepTimeThresholdUTC = new DateTime(sleepMotions.get(0).getStartTimestamp(), DateTimeZone.UTC).minusSeconds(1);
        if (sleepTimeThreshold.isPresent()) {
            sleepTimeThresholdUTC = sleepTimeThreshold.get().minusMillis(sleepMotions.get(0).getTimezoneOffset());
        }

        for(final MotionEvent sleepMotion : sleepMotions) {
            if(sleepMotion.getSleepDepth() < motionThreshold) {
                final DateTime dateTime = new DateTime(sleepMotion.getStartTimestamp(), DateTimeZone.UTC);
                if (dateTime.isAfter(sleepTimeThresholdUTC)) {
                    dateTimes.add(dateTime);
                    map.put(sleepMotion.getStartTimestamp(), sleepMotion);
                }
            }
        }

        if(dateTimes.size() < 2){
            return Optional.absent();
        }


        for(int i = 0; i < dateTimes.size() - 1; i++) {
            final DateTime current = dateTimes.get(i);
            final DateTime next = dateTimes.get(i + 1);
            final int diffInMinutes = (int)(next.getMillis() - current.getMillis()) / DateTimeConstants.MILLIS_PER_MINUTE;
            if (diffInMinutes > thresholdInMinutes) {
                if(map.containsKey(current.getMillis())) {
                    final MotionEvent motion = map.get(current.getMillis());
                    return Optional.of(new SleepEvent(motion.getStartTimestamp(), motion.getEndTimestamp(), motion.getTimezoneOffset()));

                }
                break;  // Get the first event
            }
        }

        return Optional.absent();
    }

    public static List<Event> getLightEvents(List<Sample> lightData) {

        if (lightData.size() == 0) {
            return Collections.EMPTY_LIST;
        }

        LOGGER.debug("Light samples size: {}", lightData.size());

        final LinkedList<AmplitudeData> lightAmplitudeData = new LinkedList<>();
        for (final Sample sample : lightData) {
            lightAmplitudeData.add(new AmplitudeData(sample.dateTime, (double) sample.value, sample.offsetMillis));
        }

        // TODO: could make this configurable.
        final double darknessThreshold = 1.1; // DVT unit ALS is very sensitive
        final int approxSunsetHour = 17;
        final int approxSunriseHour = 6;
        final int smoothingDegree = 5; // think of it as minutes

        final LightEventsDetector detector = new LightEventsDetector(approxSunriseHour, approxSunsetHour, darknessThreshold, smoothingDegree);

        final LinkedList<LightSegment> lightSegments = detector.process(lightAmplitudeData);

        // convert segments to Events
        final List<Event> events = new ArrayList<>();
        for (final LightSegment segment : lightSegments) {
            final LightSegment.Type segmentType = segment.segmentType;

            if (segmentType == LightSegment.Type.NONE) {
                continue;
            }

            final long startTimestamp = segment.startTimestamp + smoothingDegree * MINUTE_IN_MILLIS;
            final int offsetMillis = segment.offsetMillis;

            if (segmentType == LightSegment.Type.LIGHTS_OUT) {
                // create light on and lights out event
                // remove light on for now.
//                final LightEvent event = new LightEvent(startTimestamp, startTimestamp + MINUTE_IN_MILLIS, offsetMillis, "Lights on");
//                events.add(event);

                final long endTimestamp = segment.endTimestamp - smoothingDegree * MINUTE_IN_MILLIS;
                events.add(new LightsOutEvent(endTimestamp, endTimestamp + MINUTE_IN_MILLIS, offsetMillis));

            } else if (segmentType == LightSegment.Type.LIGHT_SPIKE) {
                events.add(new LightEvent(startTimestamp, startTimestamp + MINUTE_IN_MILLIS, offsetMillis, "Light"));
            }
            // TODO: daylight spike event -- unsure what the value might be at this moment
        }
        return events;
    }

    public static Optional<DateTime> getLightsOutTime(final List<Event> lightEvents) {
        for (final Event event : lightEvents) {
            if (event.getType() == Event.Type.LIGHTS_OUT) {
                final DateTime lightsOutTime = new DateTime(event.getEndTimestamp(), DateTimeZone.forOffsetMillis(event.getTimezoneOffset()));
                final int lightsOutHour = lightsOutTime.getHourOfDay();
                if (lightsOutHour > LIGHTS_OUT_START_THRESHOLD  || lightsOutHour < LIGHTS_OUT_END_THRESHOLD) { // 7pm to 4am
                    // minus 10 mins to allow for some people falling asleep
                    // before turning off the lights! (e.g. bryan, Q)
                    return Optional.of(lightsOutTime);
                }
                break;
            }
        }

        return Optional.absent();
    }

    public static List<Optional<Event>> getSleepEvents(final DateTime targetDateLocalUTC,
                                         final List<TrackerMotion> trackerMotions,
                                         final Optional<DateTime> lightOutTimeOptional,
                                         final int smoothWindowSizeInMinutes,
                                         final int sleepFeatureAggregateWindowInMinutes,
                                         final int wakeUpFeatureAggregateWindowInMinutes,
                                         final boolean debugMode){
        final TrackerMotionDataSource dataSource = new TrackerMotionDataSource(TrackerMotion.Utils.removeDuplicates(trackerMotions));
        final List<AmplitudeData> dataWithGapFilled = dataSource.getDataForDate(targetDateLocalUTC.withTimeAtStartOfDay());

        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> motionFeatures = MotionFeatures.generateTimestampAlignedFeatures(dataWithGapFilled,
                sleepFeatureAggregateWindowInMinutes,
                wakeUpFeatureAggregateWindowInMinutes,
                debugMode);
        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedFeatures = MotionFeatures.aggregateData(motionFeatures, smoothWindowSizeInMinutes);
        LOGGER.info("smoothed data size {}", aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_AMPLITUDE).size());

        final ArrayList<SleepDataScoringFunction> scoringFunctions = new ArrayList<>();

        int featureDimension = 1;

        final Map<Long, List<AmplitudeData>> matrix = MotionScoreAlgorithm.createFeatureMatrix(aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_AMPLITUDE));
        scoringFunctions.add(new AmplitudeDataScoringFunction());

        featureDimension = MotionScoreAlgorithm.addToFeatureMatrix(matrix, aggregatedFeatures.get(MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE));
        scoringFunctions.add(new MotionDensityScoringFunction(MotionDensityScoringFunction.ScoreType.SLEEP));

        featureDimension = MotionScoreAlgorithm.addToFeatureMatrix(matrix, aggregatedFeatures.get(MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE));
        scoringFunctions.add(new MotionDensityScoringFunction(MotionDensityScoringFunction.ScoreType.WAKE_UP));

        if(lightOutTimeOptional.isPresent()) {
            final LinkedList<AmplitudeData> lightFeature = new LinkedList<>();
            for (final AmplitudeData amplitudeData : aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_AMPLITUDE)) {
                // Pad the light data
                lightFeature.add(new AmplitudeData(amplitudeData.timestamp, 0, amplitudeData.offsetMillis));

            }
            featureDimension = MotionScoreAlgorithm.addToFeatureMatrix(matrix, lightFeature);

            if(dataWithGapFilled.size() > 0) {
                LOGGER.info("Light out time {}", lightOutTimeOptional.get()
                        .withZone(DateTimeZone.forOffsetMillis(dataWithGapFilled.get(0).offsetMillis)));
            }
            scoringFunctions.add(new LightOutScoringFunction(lightOutTimeOptional.get(), 3d));

            final LinkedList<AmplitudeData> lightAndCumulatedMotionFeature = new LinkedList<>();
            for (final AmplitudeData amplitudeData : aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_MOTION_PERIOD)) {
                // this is the magical light feature that can keep both magic and fix broken things.
                lightAndCumulatedMotionFeature.add(new AmplitudeData(amplitudeData.timestamp,
                        1d / (amplitudeData.amplitude + 0.3),  // Max can go 3 times as much as the original score
                        amplitudeData.offsetMillis));

            }
            featureDimension = MotionScoreAlgorithm.addToFeatureMatrix(matrix, lightAndCumulatedMotionFeature);
            scoringFunctions.add(new LightOutCumulatedMotionMixScoringFunction(lightOutTimeOptional.get()));
        }

        final MotionScoreAlgorithm sleepDetectionAlgorithm = new MotionScoreAlgorithm(matrix,
                featureDimension,  // modality
                aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_AMPLITUDE).size(),  // num of data.
                scoringFunctions);

        final List<Segment> segments = sleepDetectionAlgorithm.getSleepEvents(debugMode);
        final ArrayList<Event> events = new ArrayList<>();
        final Segment goToBedSegment = segments.get(0);
        final Segment fallAsleepSegment = segments.get(1);
        final Segment wakeUpSegment = segments.get(2);
        final Segment outOfBedSegment = segments.get(3);

        //final int smoothWindowSizeInMillis = smoothWindowSizeInMinutes * DateTimeConstants.MILLIS_PER_MINUTE;
        events.add(new InBedEvent(goToBedSegment.getStartTimestamp(),
                goToBedSegment.getStartTimestamp() + 1 * DateTimeConstants.MILLIS_PER_MINUTE,
                goToBedSegment.getOffsetMillis()));

        events.add(new SleepEvent(fallAsleepSegment.getStartTimestamp(),
                fallAsleepSegment.getStartTimestamp() + 1 * DateTimeConstants.MILLIS_PER_MINUTE,
                fallAsleepSegment.getOffsetMillis()));

        events.add(new WakeupEvent(wakeUpSegment.getStartTimestamp(),
                wakeUpSegment.getStartTimestamp() + 1 * DateTimeConstants.MILLIS_PER_MINUTE,
                wakeUpSegment.getOffsetMillis()));

        events.add(new OutOfBedEvent(outOfBedSegment.getStartTimestamp(),
                outOfBedSegment.getStartTimestamp() + 1 * DateTimeConstants.MILLIS_PER_MINUTE,
                outOfBedSegment.getOffsetMillis()));

        return sleepEventsHeuristicFix(events, aggregatedFeatures);

    }


    public static boolean hasLongQuietPeriod(final long startTimestamp, final long endTimestamp, final List<AmplitudeData> cumulatedQietCounts, final int thresholdCount){
        double maxCount = 0;
        for(final AmplitudeData cumulatedQuietCount:cumulatedQietCounts){
            if(cumulatedQuietCount.timestamp >= startTimestamp && cumulatedQuietCount.timestamp <= endTimestamp){
                if(cumulatedQuietCount.amplitude > maxCount){
                    maxCount = cumulatedQuietCount.amplitude;
                }
            }
        }

        return maxCount >= thresholdCount;
    }

    public static List<Optional<Event>> sleepEventsHeuristicFix(final List<Event> sleepEvents, final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features){
        final Event goToBed = sleepEvents.get(0);
        final Event sleep = sleepEvents.get(1);
        final Event wakeUp = sleepEvents.get(2);
        final Event outOfBed = sleepEvents.get(3);

        final ArrayList<Optional<Event>> fixedSleepEvents = new ArrayList<>();
        fixedSleepEvents.add(Optional.of(goToBed));
        fixedSleepEvents.add(Optional.of(sleep));
        fixedSleepEvents.add(Optional.of(wakeUp));
        fixedSleepEvents.add(Optional.of(outOfBed));

        if(sleep.getStartTimestamp() == goToBed.getStartTimestamp()){
            fixedSleepEvents.set(1, Optional.of((Event)new SleepEvent(sleep.getStartTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                    sleep.getEndTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                    sleep.getTimezoneOffset())));
            LOGGER.warn("Sleep {} has the same time with in bed, set to in bed +1 minute.",
                    new DateTime(sleep.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleep.getTimezoneOffset())));
        }

        if(wakeUp.getStartTimestamp() == outOfBed.getStartTimestamp()){
            fixedSleepEvents.set(3, Optional.of((Event) new OutOfBedEvent(outOfBed.getStartTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                    outOfBed.getEndTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                    outOfBed.getTimezoneOffset())));
            LOGGER.warn("Out of bed {} has the same time with wake up, set to wake up +1 minute.",
                    new DateTime(outOfBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.getTimezoneOffset())));
        }

        // Heuristic fix
        if(sleep.getStartTimestamp() < goToBed.getStartTimestamp()) {

            if(goToBed.getStartTimestamp() - sleep.getEndTimestamp() < 40 * DateTimeConstants.MILLIS_PER_MINUTE) {
                LOGGER.warn("Go to bed {} later then fall asleep {}, go to bed set to sleep.",
                        new DateTime(goToBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(goToBed.getTimezoneOffset())),
                        new DateTime(sleep.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleep.getTimezoneOffset())));

                fixedSleepEvents.set(0, Optional.of((Event) new InBedEvent(sleep.getStartTimestamp() - DateTimeConstants.MILLIS_PER_MINUTE,
                        sleep.getEndTimestamp() - DateTimeConstants.MILLIS_PER_MINUTE,
                        sleep.getTimezoneOffset())));
            }else{
                fixedSleepEvents.set(0, Optional.<Event>absent());
            }

        }

        // Heuristic fix: wake up time is later than out of bed, use out of bed because it looks
        // for the most significant motion
        if(wakeUp.getStartTimestamp() > outOfBed.getStartTimestamp()){
                // Huge spike before motion+spikes, has motion in between
                // already wake up?
            if(wakeUp.getStartTimestamp() - outOfBed.getEndTimestamp() < 40 * DateTimeConstants.MILLIS_PER_MINUTE) {
                LOGGER.warn("Wake up later than out of bed, wake up {}, out of bed {}, out of bed set to wake up + 1.",
                        new DateTime(wakeUp.getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.getTimezoneOffset())),
                        new DateTime(outOfBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.getTimezoneOffset())));


                fixedSleepEvents.set(3, Optional.of((Event) new OutOfBedEvent(wakeUp.getEndTimestamp(),
                        wakeUp.getEndTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                        wakeUp.getTimezoneOffset())));
            }else{
                LOGGER.warn("Wake up later than out of bed too much, wake up {}, out of bed {}, set out of bed as wake up and remove out of bed.",
                        new DateTime(wakeUp.getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.getTimezoneOffset())),
                        new DateTime(outOfBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.getTimezoneOffset())));
                fixedSleepEvents.set(2, Optional.of((Event) new WakeupEvent(outOfBed.getStartTimestamp(),
                        outOfBed.getEndTimestamp(),
                        outOfBed.getTimezoneOffset())));
                fixedSleepEvents.set(3, Optional.<Event>absent());

            }

        }


        if(Math.abs(sleep.getStartTimestamp() - goToBed.getStartTimestamp()) > 120 * DateTimeConstants.MILLIS_PER_MINUTE){
            LOGGER.warn("Go to bed and sleep off too much, out of bed {}, sleep {}, eliminate sleep.",
                    new DateTime(goToBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(goToBed.getTimezoneOffset())),
                    new DateTime(sleep.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleep.getTimezoneOffset())));
            fixedSleepEvents.set(1, Optional.<Event>absent());
        }

        if(Math.abs(wakeUp.getStartTimestamp() - outOfBed.getStartTimestamp()) > 120 * DateTimeConstants.MILLIS_PER_MINUTE){


            if(features.get(MotionFeatures.FeatureType.MAX_AMPLITUDE).size() > 0){
                final List<AmplitudeData> motion = features.get(MotionFeatures.FeatureType.MAX_AMPLITUDE);
                final long lastMotionTimestamp = motion.get(motion.size() - 1).timestamp;
                if(Math.abs(outOfBed.getStartTimestamp() - lastMotionTimestamp) > 2 * DateTimeConstants.MILLIS_PER_HOUR){
                    LOGGER.warn("Wake up and out of bed off too much, out of bed {}, wake up {}, eliminate both.",
                            new DateTime(outOfBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.getTimezoneOffset())),
                            new DateTime(wakeUp.getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.getTimezoneOffset())));
                    fixedSleepEvents.set(2, Optional.<Event>absent());
                    fixedSleepEvents.set(3, Optional.<Event>absent());

                }else{
                    LOGGER.warn("Wake up and out of bed off too much, out of bed {}, wake up {}, eliminate wake up.",
                            new DateTime(outOfBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.getTimezoneOffset())),
                            new DateTime(wakeUp.getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.getTimezoneOffset())));

                    // The one more close to last motion is more likely to be correct
                    fixedSleepEvents.set(2, Optional.<Event>absent());
                }
            }

        }

        if(fixedSleepEvents.get(0).isPresent() && fixedSleepEvents.get(3).isPresent() &&
                fixedSleepEvents.get(3).get().getStartTimestamp() - fixedSleepEvents.get(0).get().getEndTimestamp() < 4 * DateTimeConstants.MILLIS_PER_HOUR){
            fixedSleepEvents.set(0, Optional.<Event>absent());
            fixedSleepEvents.set(3, Optional.<Event>absent());
            LOGGER.warn("In bed {} - {} less than 4 hours, eliminate both.",
                    new DateTime(goToBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(goToBed.getTimezoneOffset())),
                    new DateTime(outOfBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.getTimezoneOffset())));
        }

        if(fixedSleepEvents.get(1).isPresent() && fixedSleepEvents.get(2).isPresent() &&
                fixedSleepEvents.get(2).get().getStartTimestamp() - fixedSleepEvents.get(1).get().getEndTimestamp() < 4 * DateTimeConstants.MILLIS_PER_HOUR){

            fixedSleepEvents.set(1, Optional.<Event>absent());
            fixedSleepEvents.set(2, Optional.<Event>absent());
            LOGGER.warn("sleep {} - {} less than 4 hours, eliminate both.",
                    new DateTime(sleep.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleep.getTimezoneOffset())),
                    new DateTime(wakeUp.getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.getTimezoneOffset())));
        }



        return fixedSleepEvents;
    }

}
