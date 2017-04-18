package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.LightSegment;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.sensordata.LightEventsDetector;
import com.hello.suripu.algorithm.sensordata.SoundEventsDetector;
import com.hello.suripu.algorithm.sleep.MotionScoreAlgorithm;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.algorithm.sleep.Vote;
import com.hello.suripu.algorithm.sleep.scores.AmplitudeDataScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.LightOutCumulatedMotionMixScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.LightOutScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.MotionDensityScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.WaveAccumulateMotionScoreFunction;
import com.hello.suripu.algorithm.sleep.scores.ZeroToMaxMotionCountDurationScoreFunction;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.AgitatedSleep;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.DataCompleteness;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.AlarmEvent;
import com.hello.suripu.core.models.Events.FallingAsleepEvent;
import com.hello.suripu.core.models.Events.InBedEvent;
import com.hello.suripu.core.models.Events.LightEvent;
import com.hello.suripu.core.models.Events.LightsOutEvent;
import com.hello.suripu.core.models.Events.MotionEvent;
import com.hello.suripu.core.models.Events.NoiseEvent;
import com.hello.suripu.core.models.Events.NullEvent;
import com.hello.suripu.core.models.Events.OutOfBedEvent;
import com.hello.suripu.core.models.Events.SleepingEvent;
import com.hello.suripu.core.models.Events.WakeupEvent;
import com.hello.suripu.core.models.Insight;
import com.hello.suripu.core.models.MainEventTimes;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.roomstate.Condition;
import com.hello.suripu.core.roomstate.CurrentRoomState;
import com.hello.suripu.core.roomstate.State;
import com.hello.suripu.core.translations.English;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
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
import java.util.UUID;

public class TimelineUtils {

    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(TimelineUtils.class);

    public static final Integer HIGHEST_SLEEP_DEPTH = 100;
    public static final Integer HIGH_SLEEP_DEPTH = 80;
    public static final Integer MEDIUM_SLEEP_DEPTH = 60;
    public static final Integer LOW_SLEEP_DEPTH = 30;
    public static final Integer LOWEST_SLEEP_DEPTH = 10;
    public static final long MINUTE_IN_MILLIS = 60000;

    private static final long PRESLEEP_WINDOW_IN_MILLIS = 900000; // 15 mins
    private static final int LIGHTS_OUT_START_THRESHOLD = 19; // 7pm local time
    private static final int LIGHTS_OUT_END_THRESHOLD = 4; // 4am local time
    private static final long FILTER_NON_SIGNIFICANT_EVENT_IN_MILLIS = 3600000; // 60 mins

    // for sound
    private static final int DEFAULT_QUIET_START_HOUR = 23; // 11pm
    private static final int DEFAULT_QUIET_END_HOUR = 7; // 7am
    private static final int SOUND_WINDOW_SIZE_MINS = 30; // smoothing windows, binning
    private static final int MAX_SOUND_EVENT_SIZE = 5; // max sound event allowed in timeline

    private static final int NUM_PERIODS_IN_DAY= 3;

    private static final Sensor[] SLEEP_TIME_AVERAGE_SENSORS = {
            Sensor.TEMPERATURE,
            Sensor.HUMIDITY,
            Sensor.PARTICULATES,
            Sensor.LIGHT,
    };

    private final Logger LOGGER;

    public List<Event> convertLightMotionToNone(final List<Event> eventList, final int thresholdSleepDepth){
        final LinkedList<Event> convertedEvents = new LinkedList<>();
        for(final Event event:eventList){
            if(event.getType() == Event.Type.NONE || event.getSleepDepth() > thresholdSleepDepth && (event.getType() == Event.Type.MOTION)){
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

    public TimelineUtils(final UUID uuid) {
        this(Optional.of(uuid));
    }

    public TimelineUtils() {
        this(Optional.<UUID>absent());
    }

    public TimelineUtils(final Optional<UUID> uuid) {
        if (uuid.isPresent()) {
            LOGGER = new LoggerWithSessionId(STATIC_LOGGER,uuid.get());
        }
        else {
            LOGGER = new LoggerWithSessionId(STATIC_LOGGER);
        }
    }




    /**
     * Harmonizes sleep depth into n buckets
     * @param sleepDepth
     * @return
     */
    public Integer categorizeSleepDepth(final Integer sleepDepth) {
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

    public List<TrackerMotion> removeNegativeAmplitudes(final List<TrackerMotion> trackerMotions){
        final List<TrackerMotion> positiveMotions = new LinkedList<>();
        for(final TrackerMotion motion:trackerMotions){
            if(motion.value > 0){
                positiveMotions.add(motion);
            }
        }

        return positiveMotions;
    }

    public List<MotionEvent> generateMotionEvents(final List<TrackerMotion> trackerMotions) {
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

    public Integer getSleepDepth(final Integer amplitude, final Map<Integer, Integer> positionMap, final Integer maxSVM){
        if(positionMap.size() < 5){
            return normalizeSleepDepth(amplitude, maxSVM);
        }

        if(positionMap.containsKey(amplitude)){
            return positionMap.get(amplitude);
        }

        return normalizeSleepDepth(amplitude, maxSVM);
    }

    public Integer getMaxSVM(final List<TrackerMotion> amplitudes){
        int maxSVM = 0;
        for(final TrackerMotion trackerMotion : amplitudes) {
            maxSVM = Math.max(maxSVM, trackerMotion.value);
        }

        return maxSVM;
    }

    public Map<Integer, Integer> constructValuePositionMap(final List<TrackerMotion> amplitudes){

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

    public List<SleepSegment> eventsToSegments(final List<Event> events){
        final List<SleepSegment> segments = new ArrayList<>();
        for(final Event event:events){
            segments.add(new SleepSegment((long)segments.size(), event, Collections.EMPTY_LIST));
        }

        return ImmutableList.copyOf(segments);

    }

    public List<Event> removeMotionEventsOutsideBedPeriod(final List<Event> events,
                                                          final Optional<Event> inBedEventOptional,
                                                          final Optional<Event> outOfBedEventOptional){
        final LinkedList<Event> newEventList = new LinkedList<>();
        // State is harmful, shall avoid it like plague
        for(final Event event:events) {

            if (event.getType() != Event.Type.MOTION) {
                newEventList.add(event);
                continue;
            }

            if(inBedEventOptional.isPresent() && event.getEndTimestamp() <= inBedEventOptional.get().getStartTimestamp()) {
                newEventList.add(new NullEvent(event.getStartTimestamp(), event.getEndTimestamp(), event.getTimezoneOffset(), event.getSleepDepth()));
                continue;
            }

            if(outOfBedEventOptional.isPresent() && event.getStartTimestamp() >= outOfBedEventOptional.get().getEndTimestamp()){
                newEventList.add(new NullEvent(event.getStartTimestamp(), event.getEndTimestamp(), event.getTimezoneOffset(), event.getSleepDepth()));
                continue;
            }

            newEventList.add(event);
        }

        return newEventList;
    }

    public List<Event> removeMotionEventsOutsideSleep(final List<Event> events,
                                                      final Optional<Event> sleepEventOptional,
                                                      final Optional<Event> awakeEventOptional){
        final LinkedList<Event> newEventList = new LinkedList<>();
        for(final Event event:events) {

            final Event.Type etype = event.getType();
            if (etype != Event.Type.MOTION && etype != Event.Type.NONE && etype != Event.Type.PARTNER_MOTION) {
                newEventList.add(event);
                continue;
            }

            if(sleepEventOptional.isPresent() && event.getEndTimestamp() <= sleepEventOptional.get().getStartTimestamp()) {
                continue;
            }

            if(awakeEventOptional.isPresent() && event.getStartTimestamp() >= awakeEventOptional.get().getEndTimestamp()){
                continue;
            }

            newEventList.add(event);
        }

        return newEventList;
    }
    public Optional<Event> getFirstSignificantEvent(final List<Event> events){
        for(final Event event:events){
            // only consider in-bed or sleep as significant
            final Event.Type eType = event.getType();
            if (Event.Type.IN_BED.equals(eType) || Event.Type.SLEEP.equals(eType)) {
                return Optional.of(event);
            }
        }

        return Optional.absent();
    }

    public List<Event> removeEventBeforeSignificant(final List<Event> events){
        final Optional<Event> significantEvent = getFirstSignificantEvent(events);
        if (!significantEvent.isPresent()) {
            // nothing to remove
            return events;
        }

        final List<Event> filteredEvents = Lists.newArrayList();
        for(final Event event:events){

            if(event.getStartTimestamp() < significantEvent.get().getStartTimestamp()){
                if (Event.Type.LIGHTS_OUT.equals(event.getType())) {
                    // only keep lights-out if it is within 60 minutes of in-bed/sleep
                    final Long timeDiff = significantEvent.get().getStartTimestamp() - event.getStartTimestamp();
                    if (timeDiff < FILTER_NON_SIGNIFICANT_EVENT_IN_MILLIS)
                        filteredEvents.add(event);
                }
                continue;
            }

            filteredEvents.add(event);
        }

        return filteredEvents;
    }

    public List<Event> greyNullEventsOutsideBedPeriod(final List<Event> events,
                                                      final Optional<Event> inBedEventOptional,
                                                      final Optional<Event> outOfBedEventOptional){
        final LinkedList<Event> newEventList = new LinkedList<>();

        // State is harmful, shall avoid it like plague
        for(final Event event:events){

            final Event.Type eventType = event.getType();
            if (eventType != Event.Type.NONE) {
                if (eventType != Event.Type.MOTION && eventType != Event.Type.PARTNER_MOTION) {
                    newEventList.add(event);
                    continue;
                } else {
                    // for backward compatibility
                    newEventList.add(event);
                    continue;
                }
            }


            // Null event inside bed period, or
            // Null event after in bed but no out of bed event presents, or
            // Null event before out of bed but no in bed event presents, or
            // any Null events when there is no in/out of bed events present.
            // turn it to blue sleep event, let's don't be aggressive.
            newEventList.add(new SleepingEvent(event.getStartTimestamp(), event.getEndTimestamp(), event.getTimezoneOffset(), event.getSleepDepth()));

        }

        return newEventList;
    }


    public List<Event> insertOneMinuteDurationEvents(final List<Event> eventList, final Event sleepEvent){
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

    public List<Event> smoothEvents(final List<Event> eventList){
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

    public List<Event> generateAlignedSegmentsByTypeWeight(final List<Event> eventList,
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
    public Integer normalizeSleepDepth(final int value, int maxValue) {

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
    public static SleepStats computeStats(final List<SleepSegment> segments, final List<TrackerMotion> trackerMotions, final int lightSleepThreshold, final boolean hasMediumSleep, final boolean useUninterruptedDuration) {
        Integer soundSleepDurationInSecs = 0;
        Integer mediumSleepDurationInSecs = 0;
        Integer lightSleepDurationInSecs = 0;

        int sleepDurationInSecs = 0;
        int inBedDurationInSecs = 0;
        Integer numberOfMotionEvents = 0;
        long sleepTimestampMillis = 0L;
        long firstSleepTimestampMillis = 0L;
        long wakeUpTimestampMillis = 0L;
        long outBedTimestampMillis = 0L;
        Integer sleepOnsetTimeMinutes = 0;
        long inBedTimestampMillis = 0L;
        long firstInBedTimestampMillis = 0L;

        boolean sleepStarted = false;
        boolean inBedStarted = false;

        for(final SleepSegment segment : segments) {
            if (segment.getType() == Event.Type.IN_BED) {
                inBedTimestampMillis = segment.getTimestamp();
                inBedStarted = true;

                if (firstInBedTimestampMillis == 0L) {
                    firstInBedTimestampMillis = segment.getTimestamp();
                }
            }

            if(segment.getType() == Event.Type.SLEEP){
                sleepStarted = true;
                sleepTimestampMillis = segment.getTimestamp();

                if (firstSleepTimestampMillis == 0L) {
                    firstSleepTimestampMillis = segment.getTimestamp();
                }
            }

            if(segment.getType() == Event.Type.WAKE_UP && sleepStarted){  //On purpose dangling case, if no wakeup present
                sleepStarted = false;
                wakeUpTimestampMillis = segment.getTimestamp();
                sleepDurationInSecs += (int) (segment.getTimestamp() - sleepTimestampMillis) / DateTimeConstants.MILLIS_PER_SECOND;
            }

            if(segment.getType() == Event.Type.OUT_OF_BED && inBedStarted){
                inBedStarted = false;
                outBedTimestampMillis = segment.getTimestamp();
                inBedDurationInSecs += (int) (segment.getTimestamp() - inBedTimestampMillis) / DateTimeConstants.MILLIS_PER_SECOND;
            }

            if(!sleepStarted){
                continue;
            }

            // compute sleep-state duration
            final int sleepDepth = segment.getSleepDepth();
            final int duration = segment.getDurationInSeconds();

            if (hasMediumSleep) {
                if (sleepDepth >= SleepStats.MEDIUM_SLEEP_DEPTH_THRESHOLD) {
                    soundSleepDurationInSecs += duration;
                } else if (sleepDepth >= SleepStats.LIGHT_SLEEP_DEPTH_THRESHOLD) {
                    mediumSleepDurationInSecs += duration;
                } else if (sleepDepth >= SleepStats.AWAKE_SLEEP_DEPTH_THRESHOLD) {
                    lightSleepDurationInSecs += duration;
                }
            } else {
                if (sleepDepth >= lightSleepThreshold) {
                    soundSleepDurationInSecs += duration;
                } else if (sleepDepth >= 0 && sleepDepth < lightSleepThreshold) {
                    lightSleepDurationInSecs += duration;
                }
            }

            if(segment.getType() == Event.Type.MOTION){
                numberOfMotionEvents++;
            }
            //LOGGER.trace("duration in seconds = {}", segment.getDurationInSeconds());
        }

        if(sleepDurationInSecs == 0 && inBedDurationInSecs == 0 && segments.size() > 0){
            final List<SleepSegment> sortedSegments = Ordering.natural().sortedCopy(segments);

            // I want to keep the final :)
            final long firstEventTimestamp = Math.max(firstSleepTimestampMillis, firstInBedTimestampMillis) == 0 ? sortedSegments.get(0).getTimestamp() : Math.max(firstSleepTimestampMillis, firstInBedTimestampMillis);
            final long lastEventTimestamp = Math.max(wakeUpTimestampMillis, outBedTimestampMillis) == 0 ? sortedSegments.get(sortedSegments.size() - 1).getTimestamp() : Math.max(wakeUpTimestampMillis, outBedTimestampMillis);
            if(lastEventTimestamp - firstEventTimestamp > 4 * DateTimeConstants.MILLIS_PER_HOUR){
                inBedDurationInSecs = (int) ((lastEventTimestamp - firstEventTimestamp) / DateTimeConstants.MILLIS_PER_SECOND);
                if (wakeUpTimestampMillis == 0) {
                    wakeUpTimestampMillis = lastEventTimestamp;
                }
            }
        }

        final Integer soundSleepDurationInMinutes = Math.round((float) soundSleepDurationInSecs / DateTimeConstants.SECONDS_PER_MINUTE);
        final Integer mediumSleepDurationInMinutes = Math.round((float) mediumSleepDurationInSecs / DateTimeConstants.SECONDS_PER_MINUTE);
        final Integer lightSleepDurationInMinutes = Math.round((float) lightSleepDurationInSecs / DateTimeConstants.SECONDS_PER_MINUTE);
        final Integer sleepDurationInMinutes = Math.round((float) sleepDurationInSecs / DateTimeConstants.SECONDS_PER_MINUTE);
        final Integer inBedDurationInMinutes = Math.round((float) inBedDurationInSecs / DateTimeConstants.SECONDS_PER_MINUTE);

        if (firstInBedTimestampMillis > 0 && firstInBedTimestampMillis < firstSleepTimestampMillis) {
            sleepOnsetTimeMinutes = (int) ((firstSleepTimestampMillis - firstInBedTimestampMillis)/MINUTE_IN_MILLIS);
        }
        int uninterruptedSleepDurationInMinutes = soundSleepDurationInMinutes;
        if (useUninterruptedDuration) {
            final AgitatedSleep agitatedSleep = SleepScoreUtils.getAgitatedSleep(trackerMotions, sleepTimestampMillis, wakeUpTimestampMillis);
            uninterruptedSleepDurationInMinutes = Math.max(agitatedSleep.uninterruptedSleepMins - 20, 0);
        }

        final SleepStats sleepStats = new SleepStats(
                soundSleepDurationInMinutes,
                mediumSleepDurationInMinutes,
                lightSleepDurationInMinutes,
                uninterruptedSleepDurationInMinutes,
                sleepDurationInMinutes == 0 ? inBedDurationInMinutes : sleepDurationInMinutes,
                sleepDurationInMinutes == 0,
                numberOfMotionEvents,
                firstSleepTimestampMillis,
                wakeUpTimestampMillis,
                sleepOnsetTimeMinutes
        );
        //LOGGER.debug("Sleepstats = {}", sleepStats);

        return sleepStats;
    }


    /**
     * Generate string representation of the sleep stats
     * @param sleepStats
     * @return
     */
    public String generateMessage(final SleepStats sleepStats, final int numPartnerMotion, final int numSoundEvents) {

        final Integer percentageOfSoundSleep = Math.round((float) sleepStats.soundSleepDurationInMinutes /sleepStats.sleepDurationInMinutes * 100);
        final double sleepDurationInHours = sleepStats.sleepDurationInMinutes / (double)DateTimeConstants.MINUTES_PER_HOUR;
        double soundDurationInHours = sleepStats.uninterruptedSleepDurationInMinutes / (double) DateTimeConstants.MINUTES_PER_HOUR;

        // report in-bed time
        String message = String.format("You were in bed for **%.1f hours**", sleepDurationInHours);
        if(!sleepStats.isInBedDuration){
            message = String.format("You were asleep for **%.1f hours**", sleepDurationInHours);
        }

        if (soundDurationInHours > 0) {
            message += String.format(", and sleeping soundly for %.1f hours", soundDurationInHours);
        }
        message += ".";
        return message;

//        // Count toss & turns
//        if (sleepStats.numberOfMotionEvents > 2) {
//            message += String.format(", and moved %d times", sleepStats.numberOfMotionEvents);
//        }
//        message += ".";
//
//
//        String partnerMessage = "";
//        if (numPartnerMotion > 0) {
//            if (numPartnerMotion == 1) {
//                partnerMessage = "was a single partner disturbance";
//            } else {
//                partnerMessage = String.format("were %d partner disturbances", numPartnerMotion);
//            }
//        }
//
//        String soundMessage = "";
//        if (numSoundEvents > 0) {
//            if (numSoundEvents == 1) {
//                soundMessage = "a single noise disturbance.";
//            } else {
//                soundMessage = String.format("%d noise disturbances", numSoundEvents);
//            }
//        }
//
//        if (!partnerMessage.isEmpty()) {
//            if (soundMessage.isEmpty()) {
//                message += " There " + partnerMessage + ".";
//            } else {
//                message += " There " + partnerMessage;
//            }
//        }
//
//        if (!soundMessage.isEmpty()) {
//            if (!partnerMessage.isEmpty()) {
//            message += ", and " + soundMessage + ".";
//            } else {
//                if (numSoundEvents == 1) {
//                    message += " There was " + soundMessage + ".";
//                } else {
//                    message += " There were " + soundMessage + ".";
//                }
//            }
//        }
//
//        return message;

    }

    public Optional<Float> calculateAverageSensorState(final List<Sample> samples,
                                                       final long startTimestampUTC,
                                                       final long endTimestampUTC) {
        if (samples.isEmpty() || (startTimestampUTC == endTimestampUTC)) {
            return Optional.absent();
        }

        float sum = 0;
        int count = 0;
        for (final Sample sample : samples) {
            final long sampleTimestampUTC = sample.dateTime;
            if (sampleTimestampUTC < startTimestampUTC) {
                continue;
            }

            if (sampleTimestampUTC > endTimestampUTC) {
                break;
            }

            sum += sample.value;
            count++;

        }

        if (count > 0) {
            final float average = sum / count;
            return Optional.of(average);
        } else {
            return Optional.absent();
        }
    }

    public Insight generateInSleepSoundInsight(final int soundEventCount) {
        final int soundScore = SleepScoreUtils.calculateSoundScore(soundEventCount);
        if (soundScore <= 20) {
            return new Insight(Sensor.SOUND, Condition.ALERT,
                    English.ALERT_SOUND_PRE_SLEEP_MESSAGE);
        } else if (soundScore <= 60) {
            return new Insight(Sensor.SOUND, Condition.WARNING,
                    English.WARNING_SOUND_PRE_SLEEP_MESSAGE);
        } else {
            return new Insight(Sensor.SOUND, Condition.IDEAL,
                    English.IDEAL_SOUND_PRE_SLEEP_MESSAGE);
        }
    }

    public List<Insight> generateInSleepInsights(final AllSensorSampleList allSensorSampleList,
                                                 final int soundEventCount,
                                                 final Long sleepTimestampUTC,
                                                 final Long wakeTimestampUTC) {
        if (allSensorSampleList.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Insight> insights = Lists.newArrayList();
        final DateTime sleepDateTime = new DateTime(sleepTimestampUTC, DateTimeZone.UTC);

        for (final Sensor sensor : SLEEP_TIME_AVERAGE_SENSORS) {
            final List<Sample> samples = allSensorSampleList.get(sensor);
            final Optional<Float> sampleAverage = calculateAverageSensorState(samples,
                    sleepTimestampUTC, wakeTimestampUTC);
            if (!sampleAverage.isPresent()) {
                continue;
            }

            final Optional<State> sensorState = CurrentRoomState.getSensorState(sensor,
                    sampleAverage.get(), sleepDateTime, false);
            final Optional<Insight> insight = sensorState.transform(
                    state -> Insight.create(sensor, state.condition(), state.message)
            );

            if (insight.isPresent()) {
                insights.add(insight.get());
            }
        }

        final Insight soundInsight = generateInSleepSoundInsight(soundEventCount);
        insights.add(soundInsight);

        return insights;
    }

    public List<Insight> generatePreSleepInsights(final AllSensorSampleList allSensorSampleList,
                                                  final Long sleepTimestampUTC,
                                                  final Long accountId) {
        final List<Insight> generatedInsights = Lists.newArrayList();

        if (allSensorSampleList.isEmpty()) {
            return generatedInsights;
        }
        try {

            // find index for time period of interest
            int startIndex = 0;
            int endIndex = 0;
            final long startTimestamp = sleepTimestampUTC - PRESLEEP_WINDOW_IN_MILLIS;
            for (final Sample sample : allSensorSampleList.get(Sensor.LIGHT)) {
                if (sample.dateTime < startTimestamp) {
                    startIndex++;
                }

                endIndex++;

                if (sample.dateTime > sleepTimestampUTC) {
                    break;
                }
            }

            // initialize
            final Map<Sensor, Float> counts = new HashMap<>();
            final Map<Sensor, Float> sums = new HashMap<>();
            for (Sensor sensor : allSensorSampleList.getAvailableSensors()) {
                counts.put(sensor, 0.0f);
                sums.put(sensor, 0.0f);
            }

            // add values
            for (int i = startIndex; i < endIndex; i++) {
                for (final Sensor sensor : sums.keySet()) {
                    final float average = sums.get(sensor) + allSensorSampleList.get(sensor).get(i).value;
                    sums.put(sensor, average);

                    final float count = counts.get(sensor) + 1.0f;
                    counts.put(sensor, count);
                }
            }

            final DateTime sleepDateTime = new DateTime(sleepTimestampUTC, DateTimeZone.UTC);

            // compute average for each sensor, generate insights
            for (final Sensor sensor : counts.keySet()) {
                if (counts.get(sensor) == 0.0f) {
                    continue;
                }

                final float average = sums.get(sensor) / counts.get(sensor);
                Optional<State> sensorState;

                switch (sensor) {
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
                    generatedInsights.add(new Insight(sensor, sensorState.get().condition(), sensorState.get().message));
                }
            }

            return generatedInsights;
        } catch (Exception e) {
            LOGGER.error("failed generating pre-sleep insights for account {}, reason: {}", accountId, e.getMessage());
        }

        return generatedInsights;
    }


    /**
     * Naive implementation of computing sleep time based on motion data only
     * @param sleepMotions
     * @param thresholdInMinutes
     * @return
     */
    public Optional<FallingAsleepEvent> getSleepEvent(final List<MotionEvent> sleepMotions, int thresholdInMinutes, int motionThreshold, final Optional<DateTime> sleepTimeThreshold) {

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
                    return Optional.of(new FallingAsleepEvent(motion.getStartTimestamp(), motion.getEndTimestamp(), motion.getTimezoneOffset()));

                }
                break;  // Get the first event
            }
        }

        return Optional.absent();
    }


    public Optional<DateTime> getFirstAwakeWaveTime(final long firstMotionTimestampMillis,
                                                    final long lastMotionTimestampMillis,
                                                    final List<Sample> waveData){
        if(waveData.size() == 0){
            return Optional.absent();
        }

        final long startDetectionTimestampMillis = (lastMotionTimestampMillis - firstMotionTimestampMillis) / 2 + firstMotionTimestampMillis;
        for(final Sample wave:waveData){
            if(wave.value > 0 && wave.dateTime >= startDetectionTimestampMillis && wave.dateTime <= lastMotionTimestampMillis){
                return Optional.of(new DateTime(wave.dateTime, DateTimeZone.forOffsetMillis(wave.offsetMillis)));
            }
        }

        return Optional.absent();

    }

    public List<Event> getLightEvents(Optional<Long> sleepTime, List<Sample> lightData) {

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

        final LinkedList<LightSegment> lightSegments = detector.process(lightAmplitudeData, sleepTime);

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


    public List<Event> getLightEventsWithMultipleLightOut(final List<Sample> lightData) {

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

        final LinkedList<LightSegment> lightSegments = detector.process(lightAmplitudeData, Optional.<Long>absent());

        // convert segments to Events
        final List<Event> events = new ArrayList<>();
        for (final LightSegment segment : lightSegments) {
            final LightSegment.Type segmentType = segment.segmentType;

            final long startTimestamp = segment.startTimestamp + smoothingDegree * MINUTE_IN_MILLIS;
            final long endTimestamp = segment.endTimestamp - smoothingDegree * MINUTE_IN_MILLIS;
            final int offsetMillis = segment.offsetMillis;

            if (segmentType == LightSegment.Type.LIGHTS_OUT) {
                events.add(new LightsOutEvent(endTimestamp, endTimestamp + MINUTE_IN_MILLIS, offsetMillis));
            } else if (segmentType == LightSegment.Type.LIGHT_SPIKE) {
                events.add(new LightEvent(startTimestamp, startTimestamp + MINUTE_IN_MILLIS, offsetMillis, "Light"));
            } else if(segmentType == LightSegment.Type.NONE){
                events.add(new LightEvent(startTimestamp, endTimestamp, offsetMillis, "Light"));
            }
            // TODO: daylight spike event -- unsure what the value might be at this moment
        }
        return events;
    }

    public Optional<DateTime> getLightsOutTime(final List<Event> lightEvents) {
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

    /**
     * Get a list of top peak sound disturbances during queit hours)
     * @param soundData
     * @return
     */
    public List<Event> getSoundEvents(final List<Sample> soundData,
                                      final Map<Long, Integer> sleepDepths,
                                      final Optional<DateTime> optionalLightsOut,
                                      final Optional<DateTime> optionalSleepTime,
                                      final Optional<DateTime> optionalAwakeTime,
                                      final Boolean usePeakEnergyThreshold) {
        if (soundData.size() == 0) {
            return Collections.EMPTY_LIST;
        }

        LOGGER.debug("debug=sound-samples size={}", soundData.size());

        final LinkedList<AmplitudeData> soundAmplitudeData = new LinkedList<>();
        for (final Sample sample : soundData) {
            final float value = (sample.value == 0.0f) ? DataUtils.PEAK_DISTURBANCE_NOISE_FLOOR : sample.value;
            soundAmplitudeData.add(new AmplitudeData(sample.dateTime, (double) value, sample.offsetMillis));
        }

        // adjust boundaries to do noise checking
        int approxQuietTimeStart = DEFAULT_QUIET_START_HOUR; // check from 11pm to 7am
        if (optionalLightsOut.isPresent()) {
            approxQuietTimeStart = optionalLightsOut.get().getHourOfDay();
        }

        if (optionalSleepTime.isPresent()) {
            final DateTime sleepTime = optionalSleepTime.get();
            if (optionalLightsOut.isPresent() && optionalLightsOut.get().isBefore(sleepTime)) {
                approxQuietTimeStart = sleepTime.getHourOfDay();
            }
        }

        int approxQuietTimeEnds = DEFAULT_QUIET_END_HOUR;
        if (optionalAwakeTime.isPresent()) {
            approxQuietTimeEnds = optionalAwakeTime.get().getHourOfDay();
        }

        final int smoothingDegree = SOUND_WINDOW_SIZE_MINS; // smoothing window in minutes

        // get sound events
        final SoundEventsDetector detector = new SoundEventsDetector(approxQuietTimeStart, approxQuietTimeEnds, smoothingDegree);

        // audio_peak_energy is more sensitive to sudden loud noise, use a higher threshold
        final float audioThreshold = (usePeakEnergyThreshold) ? SoundEventsDetector.PEAK_ENERGY_THRESHOLD : SoundEventsDetector.PEAK_DISTURBANCE_THRESHOLD;
        LOGGER.debug("action=select-sound-event-threshold threshold={}", audioThreshold);

        final LinkedList<Segment> soundSegments = detector.process(soundAmplitudeData, audioThreshold);

        final List<Event> events = new ArrayList<>();
        for (final Segment segment : soundSegments) {
            final DateTime segmentDateTime = new DateTime(segment.getStartTimestamp(), DateTimeZone.UTC).plusMillis(segment.getOffsetMillis());
            if (optionalSleepTime.isPresent() && segmentDateTime.isBefore(optionalSleepTime.get())) {
                continue;
            }

            if (optionalAwakeTime.isPresent() && segmentDateTime.isAfter(optionalAwakeTime.get())) {
                continue;
            }

            final long timestamp = segment.getStartTimestamp();
            final int sleepDepth = (sleepDepths.containsKey(timestamp)) ? sleepDepths.get(timestamp) : 0;

            events.add(new NoiseEvent(timestamp, segment.getEndTimestamp(), segment.getOffsetMillis(), sleepDepth));

            if (events.size() >= MAX_SOUND_EVENT_SIZE) {
                break;
            }
        }
        return events;
    }

    public SleepEvents<Optional<Event>> getSleepEvents(final DateTime targetDateLocalUTC,
                                                       final List<TrackerMotion> trackerMotions,
                                                       final List<DateTime> lightOutTimes,
                                                       final Optional<DateTime> firstWaveTimeOptional,
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

        final MotionScoreAlgorithm sleepDetectionAlgorithm = new MotionScoreAlgorithm();
        sleepDetectionAlgorithm.addFeature(aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_AMPLITUDE), new AmplitudeDataScoringFunction());
        sleepDetectionAlgorithm.addFeature(aggregatedFeatures.get(MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE), new MotionDensityScoringFunction(MotionDensityScoringFunction.ScoreType.SLEEP));
        sleepDetectionAlgorithm.addFeature(aggregatedFeatures.get(MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE), new MotionDensityScoringFunction(MotionDensityScoringFunction.ScoreType.WAKE_UP));
        sleepDetectionAlgorithm.addFeature(aggregatedFeatures.get(MotionFeatures.FeatureType.ZERO_TO_MAX_MOTION_COUNT_DURATION), new ZeroToMaxMotionCountDurationScoreFunction());

        if(!lightOutTimes.isEmpty()) {
            final LinkedList<AmplitudeData> lightFeature = new LinkedList<>();
            for (final AmplitudeData amplitudeData : aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_AMPLITUDE)) {
                // Pad the light data
                lightFeature.add(new AmplitudeData(amplitudeData.timestamp, 0, amplitudeData.offsetMillis));

            }
            if(dataWithGapFilled.size() > 0) {
                for(final DateTime lightOutTime:lightOutTimes) {
                    LOGGER.info("Light out time {}", lightOutTime
                            .withZone(DateTimeZone.forOffsetMillis(dataWithGapFilled.get(0).offsetMillis)));
                }
            }
            sleepDetectionAlgorithm.addFeature(lightFeature, new LightOutScoringFunction(lightOutTimes, 3d));

            final LinkedList<AmplitudeData> lightAndCumulatedMotionFeature = new LinkedList<>();
            for (final AmplitudeData amplitudeData : aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_MOTION_PERIOD)) {
                // this is the magical light feature that can keep both magic and fix broken things.
                lightAndCumulatedMotionFeature.add(new AmplitudeData(amplitudeData.timestamp,
                        1d / (amplitudeData.amplitude + 0.3),  // Max can go 3 times as much as the original score
                        amplitudeData.offsetMillis));

            }
            sleepDetectionAlgorithm.addFeature(lightAndCumulatedMotionFeature, new LightOutCumulatedMotionMixScoringFunction(lightOutTimes));
        }

        if(firstWaveTimeOptional.isPresent()) {

            final LinkedList<AmplitudeData> waveAndCumulatedMotionFeature = new LinkedList<>();
            for (final AmplitudeData amplitudeData : aggregatedFeatures.get(MotionFeatures.FeatureType.AWAKE_BACKWARD_DENSITY)) {
                waveAndCumulatedMotionFeature.add(new AmplitudeData(amplitudeData.timestamp,
                        amplitudeData.amplitude,
                        amplitudeData.offsetMillis));

            }
            sleepDetectionAlgorithm.addFeature(waveAndCumulatedMotionFeature, new WaveAccumulateMotionScoreFunction(firstWaveTimeOptional.get()));
        }

        final SleepEvents<Segment> segments = sleepDetectionAlgorithm.getSleepEvents(debugMode);
        final Segment goToBedSegment = segments.goToBed;
        final Segment fallAsleepSegment = segments.fallAsleep;
        final Segment wakeUpSegment = segments.wakeUp;
        final Segment outOfBedSegment = segments.outOfBed;

        //final int smoothWindowSizeInMillis = smoothWindowSizeInMinutes * DateTimeConstants.MILLIS_PER_MINUTE;
        final Event inBedEvent = new InBedEvent(goToBedSegment.getStartTimestamp(),
                goToBedSegment.getStartTimestamp() + 1 * DateTimeConstants.MILLIS_PER_MINUTE,
                goToBedSegment.getOffsetMillis());

        final Event fallAsleepEvent = new FallingAsleepEvent(fallAsleepSegment.getStartTimestamp(),
                fallAsleepSegment.getStartTimestamp() + 1 * DateTimeConstants.MILLIS_PER_MINUTE,
                fallAsleepSegment.getOffsetMillis());

        final Event wakeUpEvent = new WakeupEvent(wakeUpSegment.getStartTimestamp(),
                wakeUpSegment.getStartTimestamp() + 1 * DateTimeConstants.MILLIS_PER_MINUTE,
                wakeUpSegment.getOffsetMillis());

        final Event outOfBedEvent = new OutOfBedEvent(outOfBedSegment.getStartTimestamp(),
                outOfBedSegment.getStartTimestamp() + 1 * DateTimeConstants.MILLIS_PER_MINUTE,
                outOfBedSegment.getOffsetMillis());

        final SleepEvents<Event> events = SleepEvents.create(inBedEvent, fallAsleepEvent, wakeUpEvent, outOfBedEvent);

        return SleepEventSafeGuard.sleepEventsHeuristicFix(events, aggregatedFeatures);

    }



    public Optional<VotingSleepEvents> getSleepEventsFromVoting(final List<TrackerMotion> rawTrackerMotions,
                                                                final List<Sample> sound,
                                                                final List<DateTime> lightOutTimes,
                                                                final Optional<DateTime> firstWaveTimeOptional){
        final List<AmplitudeData> rawAmplitudeData = TrackerMotionUtils.trackerMotionToAmplitudeData(rawTrackerMotions);
        final List<AmplitudeData> rawKickOffCount = TrackerMotionUtils.trackerMotionToKickOffCounts(rawTrackerMotions);
        final List<AmplitudeData> rawSound = SoundUtils.sampleToAmplitudeData(sound);
        final Vote vote = new Vote(rawAmplitudeData, rawKickOffCount, rawSound, lightOutTimes, firstWaveTimeOptional);

        final SleepEvents<Segment> segments = vote.getResult(false);
        final List<Segment> otherAwakes = vote.getAwakes(segments.fallAsleep.getEndTimestamp(), segments.wakeUp.getStartTimestamp(), false);
        return Optional.of(new VotingSleepEvents(segments, otherAwakes));
    }


    /**
     * Returns a list of Alarm Events containing alarms within the window and that have rang.
     * @param ringTimes
     * @param queryStartTime
     * @param queryEndTime
     * @param timeZoneOffsetMap
     * @return
     */
    public List<Event> getAlarmEvents(final List<RingTime> ringTimes, final DateTime queryStartTime, final DateTime queryEndTime, final TimeZoneOffsetMap timeZoneOffsetMap, final DateTime nowInUTC) {
        final List<Event> events = Lists.newArrayList();

        for(final RingTime ringTime : ringTimes) {
            if(ringTime.isEmpty()){
                continue;
            }

            final DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.UTC);
            final int offsetMillis = timeZoneOffsetMap.getOffsetWithDefaultAsZero(ringTime.actualRingTimeUTC);

            final DateTime localNow = nowInUTC.plusMillis(offsetMillis);
            if(actualRingTime.isAfter(nowInUTC)) {
                LOGGER.debug("{} is in the future. It is now {}", ringTime, localNow);
                continue;
            }

            if(ringTime.actualRingTimeUTC >= queryStartTime.getMillis() && ringTime.actualRingTimeUTC <= queryEndTime.getMillis()) {
                LOGGER.debug("{} is valid. Adding to list", ringTime);

                final DateTime actualRingLocalUTC = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.UTC).plusMillis(offsetMillis);
                final String ringTimeString = actualRingLocalUTC.toString(DateTimeFormat.forPattern("HH:mm"));
                String message = String.format(English.ALARM_NORMAL_MESSAGE, ringTimeString);

                if (ringTime.fromSmartAlarm) {
                    final DateTime alarmSetLocalUTC = new DateTime(ringTime.expectedRingTimeUTC, DateTimeZone.UTC).plusMillis(offsetMillis);
                    if (actualRingLocalUTC.equals(alarmSetLocalUTC)) {
                        message = String.format(English.ALARM_NOT_SO_SMART_MESSAGE, ringTimeString);
                    } else {
                        final String setTimeString = alarmSetLocalUTC.toString(DateTimeFormat.forPattern("HH:mm"));
                        message = String.format(English.ALARM_SMART_MESSAGE, ringTimeString, setTimeString);
                    }
                }

                final AlarmEvent event = (AlarmEvent) Event.createFromType(
                        Event.Type.ALARM,
                        new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.UTC).withSecondOfMinute(0).getMillis(),
                        new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.UTC).withSecondOfMinute(0).plusMinutes(1).getMillis(),
                        offsetMillis,
                        Optional.of(message),
                        Optional.<SleepSegment.SoundInfo>absent(),
                        Optional.<Integer>absent());
                events.add(event);
            }
        }

        LOGGER.debug("Adding {} alarms to the timeline", events.size());
        return events;
    }

    public List<Event> eventsFromOptionalEvents(final List<Optional<Event>> optionalEvents) {
        final List<Event> events = Lists.newArrayList();

        for (final Optional<Event> event : optionalEvents) {
            if (!event.isPresent()) {
                continue;
            }

            events.add(event.get());
        }

        return events;
    }

    //basic idea is to drop motions that occur near the pairing times of the pills
    public ImmutableList<TrackerMotion> filterPillPairingMotionsWithTimes(final ImmutableList<TrackerMotion> motions, final List<DateTime> pairTimes) {

        final long timeAfterCreationToFilter = 15 * DateTimeConstants.MILLIS_PER_MINUTE;
        final long timeBeforeCreationToFilter = 5 * DateTimeConstants.MILLIS_PER_MINUTE;
        final List<TrackerMotion> filteredMotions = Lists.newArrayList();

        MOTION_LOOP:
        for (final TrackerMotion m : motions) {

            for (final DateTime t : pairTimes) {
                final long tp = t.withZone(DateTimeZone.UTC).getMillis();

                if (m.timestamp >= tp - timeBeforeCreationToFilter && m.timestamp <= tp + timeAfterCreationToFilter) {
                    continue MOTION_LOOP;
                }
            }


            filteredMotions.add(m);

        }

        LOGGER.info("action=filtering_tracker_motion num_points_dropped={}",motions.size() - filteredMotions.size());

        return ImmutableList.copyOf(filteredMotions);
    }

    public static DataCompleteness getDataCompletenessForAllSleepPeriods(final List<DataCompleteness> dataCompletenessList, final int numSleepPeriods){
        DataCompleteness overallDataCompleteness = DataCompleteness.NO_DATA;
        for (final DataCompleteness dataCompleteness : dataCompletenessList){
            if(dataCompleteness.getValue() > overallDataCompleteness.getValue()){
                overallDataCompleteness = dataCompleteness;
            }
        }
        if (numSleepPeriods < NUM_PERIODS_IN_DAY && overallDataCompleteness == DataCompleteness.ENOUGH_DATA){
            overallDataCompleteness = DataCompleteness.NOT_ENOUGH_DATA;
        }
        return overallDataCompleteness;
    }
    public static MainEventTimes getPrevNightMainEventTimes(final long accountId, final List<MainEventTimes> mainEventTimesList, final DateTime date){
        final DateTime prevNight = date.withZone(DateTimeZone.UTC).withTimeAtStartOfDay().minusDays(1);
        for(final MainEventTimes mainEventTimes: mainEventTimesList){
            if (mainEventTimes.sleepPeriod.period == SleepPeriod.Period.NIGHT && mainEventTimes.sleepPeriod.targetDate.getMillis() == prevNight.getMillis()){
                return mainEventTimes;
            }
        }
        return MainEventTimes.createMainEventTimesEmpty(accountId, SleepPeriod.night(prevNight), DateTime.now(DateTimeZone.UTC).getMillis(), 0, AlgorithmType.NONE, TimelineError.NO_DATA);
    }

    public static DateTime getTargetDate(final boolean isDaySleeper, final DateTime queryDate, final DateTime currentTimeLocal, final Optional<Integer> queryHourOptional, final TimeZoneOffsetMap timeZoneOffsetMap){
        final long endQueryDate = queryDate.getMillis() - timeZoneOffsetMap.getOffsetWithDefaultAsZero(queryDate.getMillis()) + DateTimeConstants.MILLIS_PER_DAY;
        final boolean isStillQueryDate;
        if (queryHourOptional.isPresent()) {
            isStillQueryDate = currentTimeLocal.withTimeAtStartOfDay().isBefore(endQueryDate);
        } else {
            isStillQueryDate = false;
        }
        //target Date
        //if daysleeper - always return most recent sleepperiod
        //otherwise, check to see if actual query day is after midnight (despite query hour - travel)
        final DateTime targetDate;
        if(isStillQueryDate && !isDaySleeper){
            targetDate = queryDate.minusDays(1);
        } else {
            targetDate = queryDate;
        }
        return targetDate;
    }


}
