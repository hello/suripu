package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.algorithmintegration.TimelineAlgorithmResult;
import com.hello.suripu.core.translations.English;
import com.hello.suripu.core.util.AlgorithmType;
import com.hello.suripu.core.util.TimelineError;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jarredheinrich on 2/7/17.
 */
public class MainEventTimes {
    //private final List<Event.Type> REQUIRED_EVENT_TYPES =  Arrays.asList(Event.Type.IN_BED, Event.Type.SLEEP,Event.Type.WAKE_UP);
    public Long accountId;
    public final SleepPeriod sleepPeriod;
    public final ImmutableMap<Event.Type, EventTime> eventTimeMap;
    public final EventTime createdAt;
    public final AlgorithmType algorithmType;
    public final TimelineError timelineError;

    private MainEventTimes (final Long accountId, final SleepPeriod sleepPeriod, final EventTime createdAt, Map<Event.Type, EventTime> eventTimeMap, AlgorithmType algorithmType, TimelineError timelineError){
        this.accountId = accountId;
        this.sleepPeriod = sleepPeriod;
        this.createdAt = createdAt;
        this.eventTimeMap = ImmutableMap.copyOf(eventTimeMap);
        this.algorithmType = algorithmType;
        this.timelineError = timelineError;
    }

    public static class EventTime {
        public final Long time;
        public final Integer offset;

        public EventTime(final Long time, final Integer offset){
            this.time = time;
            this.offset= offset;
        }
    }


    public static MainEventTimes createMainEventTimes (final long accountId, final long inBedTime, final int inBedOffset,
                                                       final long sleepTime, final int sleepOffset, final long wakeUpTime,
                                                       final int wakeUpOffset, final long outOfBedTime, final int outOfBedOffset,
                                                       final long createdAtTime, final int createdAtOffset, final AlgorithmType algorithmType, final TimelineError timelineError){
        final DateTime inBedTimeLocalUTC = new DateTime(inBedTime + inBedOffset, DateTimeZone.UTC);
        final SleepPeriod sleepPeriod = SleepPeriod.createSleepPeriod(inBedTimeLocalUTC);
        final EventTime inBedEventTime = new EventTime(inBedTime, inBedOffset);
        final EventTime sleepEventTime = new EventTime(sleepTime, sleepOffset);
        final EventTime wakeUpEventTime = new EventTime(wakeUpTime, wakeUpOffset);
        final EventTime outOfBedEventTime = new EventTime(outOfBedTime, outOfBedOffset);
        final EventTime createdAt = new EventTime(createdAtTime, createdAtOffset);
        final ImmutableMap<Event.Type, EventTime> eventTimeMap = ImmutableMap.<Event.Type, EventTime>builder()
                .put(Event.Type.IN_BED, inBedEventTime)
                .put(Event.Type.SLEEP, sleepEventTime)
                .put(Event.Type.WAKE_UP, wakeUpEventTime)
                .put(Event.Type.OUT_OF_BED, outOfBedEventTime)
                .build();
        return new MainEventTimes(accountId, sleepPeriod,createdAt, eventTimeMap, algorithmType, timelineError);
    }

    public static MainEventTimes createMainEventTimes (final long accountId, final SleepPeriod sleepPeriod, final long createdAtTime, final int createdAtOffset,
                                                       final TimelineAlgorithmResult timelineAlgorithmResult, final TimelineError timelineError){
        final EventTime createdAt = new EventTime(createdAtTime, createdAtOffset);

        if(timelineAlgorithmResult.mainEvents.containsKey(Event.Type.IN_BED) && timelineAlgorithmResult.mainEvents.containsKey(Event.Type.SLEEP)  && timelineAlgorithmResult.mainEvents.containsKey(Event.Type.WAKE_UP)  && timelineAlgorithmResult.mainEvents.containsKey(Event.Type.OUT_OF_BED) ) {
            final EventTime inBedEventTime = new EventTime(timelineAlgorithmResult.mainEvents.get(Event.Type.IN_BED).getStartTimestamp(), timelineAlgorithmResult.mainEvents.get(Event.Type.IN_BED).getTimezoneOffset());
            final EventTime sleepEventTime = new EventTime(timelineAlgorithmResult.mainEvents.get(Event.Type.SLEEP).getStartTimestamp(), timelineAlgorithmResult.mainEvents.get(Event.Type.SLEEP).getTimezoneOffset());
            final EventTime wakeUpEventTime = new EventTime(timelineAlgorithmResult.mainEvents.get(Event.Type.WAKE_UP).getStartTimestamp(), timelineAlgorithmResult.mainEvents.get(Event.Type.WAKE_UP).getTimezoneOffset());
            final EventTime outOfBedEventTime = new EventTime(timelineAlgorithmResult.mainEvents.get(Event.Type.OUT_OF_BED).getStartTimestamp(), timelineAlgorithmResult.mainEvents.get(Event.Type.OUT_OF_BED).getTimezoneOffset());
            final ImmutableMap<Event.Type, EventTime> eventTimeMap = ImmutableMap.<Event.Type, EventTime>builder()
                    .put(Event.Type.IN_BED, inBedEventTime)
                    .put(Event.Type.SLEEP, sleepEventTime)
                    .put(Event.Type.WAKE_UP, wakeUpEventTime)
                    .put(Event.Type.OUT_OF_BED, outOfBedEventTime)
                    .build();

            return new MainEventTimes(accountId, sleepPeriod, createdAt,eventTimeMap, timelineAlgorithmResult.algorithmType, timelineError);
        }
        return new MainEventTimes(accountId, sleepPeriod, createdAt, new HashMap<>(), timelineAlgorithmResult.algorithmType, timelineError);
    }

    public static MainEventTimes createMainEventTimes (final long accountId, final SleepPeriod sleepPeriod, final long createdAtTime, final int createdAtOffset, final Map<Event.Type, EventTime> mainEventTimeMap, final AlgorithmType algorithmType, final TimelineError timelineError){
        final EventTime createdAt = new EventTime(createdAtTime, createdAtOffset);
        return new MainEventTimes(accountId, sleepPeriod, createdAt, mainEventTimeMap, algorithmType, timelineError);
    }


    public static MainEventTimes createMainEventTimesEmpty (final long accountId, final SleepPeriod sleepPeriod, final long createdAtTime, final int createdAtOffset, final AlgorithmType algorithmType, final TimelineError timelineError){
        final EventTime createdAt = new EventTime(createdAtTime, createdAtOffset);
        return new MainEventTimes(accountId, sleepPeriod, createdAt,  new HashMap<>(),  algorithmType, timelineError);
    }

    public boolean hasValidEventTimes(){
        final List<Event.Type> MAIN_EVENT_TYPES = Arrays.asList(Event.Type.IN_BED, Event.Type.SLEEP,Event.Type.WAKE_UP,Event.Type.OUT_OF_BED);

        for (final Event.Type eventType : MAIN_EVENT_TYPES){
            if (!this.eventTimeMap.containsKey(eventType)){
                return false;
            }
        }

        return true;
    }

    public List<Event> getMainEvents(){
        final List<Event> mainEvents = new ArrayList<>();
        if(!this.hasValidEventTimes()){
            return mainEvents;
        }
        mainEvents.add(Event.createFromType(Event.Type.IN_BED,
                eventTimeMap.get(Event.Type.IN_BED).time,
                eventTimeMap.get(Event.Type.IN_BED).time+DateTimeConstants.MILLIS_PER_MINUTE,
                eventTimeMap.get(Event.Type.IN_BED).offset,
                Optional.of(English.IN_BED_MESSAGE),
                Optional.<SleepSegment.SoundInfo>absent(),
                Optional.<Integer>absent()));

        mainEvents.add(Event.createFromType(Event.Type.SLEEP,
                eventTimeMap.get(Event.Type.SLEEP).time,
                eventTimeMap.get(Event.Type.SLEEP).time+DateTimeConstants.MILLIS_PER_MINUTE,
                eventTimeMap.get(Event.Type.SLEEP).offset,
                Optional.of(English.FALL_ASLEEP_MESSAGE),
                Optional.<SleepSegment.SoundInfo>absent(),
                Optional.<Integer>absent()));

        mainEvents.add(Event.createFromType(Event.Type.WAKE_UP,
                eventTimeMap.get(Event.Type.WAKE_UP).time,
                eventTimeMap.get(Event.Type.WAKE_UP).time+DateTimeConstants.MILLIS_PER_MINUTE,
                eventTimeMap.get(Event.Type.WAKE_UP).offset,
                Optional.of(English.WAKE_UP_MESSAGE),
                Optional.<SleepSegment.SoundInfo>absent(),
                Optional.<Integer>absent()));

        mainEvents.add(Event.createFromType(Event.Type.OUT_OF_BED,
                eventTimeMap.get(Event.Type.OUT_OF_BED).time,
                eventTimeMap.get(Event.Type.OUT_OF_BED).time+DateTimeConstants.MILLIS_PER_MINUTE,
                eventTimeMap.get(Event.Type.OUT_OF_BED).offset,
                Optional.of(English.OUT_OF_BED_MESSAGE),
                Optional.<SleepSegment.SoundInfo>absent(),
                Optional.<Integer>absent()));

        return mainEvents;
    }

}
