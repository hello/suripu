package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.algorithmintegration.TimelineAlgorithmResult;
import com.hello.suripu.core.translations.English;
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
    public final long createdAt;

    private MainEventTimes (final Long accountId, final SleepPeriod sleepPeriod, final long createdAt,  Map<Event.Type, EventTime> eventTimeMap){
        this.accountId = accountId;
        this.sleepPeriod = sleepPeriod;
        this.createdAt = createdAt;
        this.eventTimeMap = ImmutableMap.copyOf(eventTimeMap);
    }

    public static class EventTime {
        public final Long TIME;
        public final Integer OFFSET;

        public EventTime(final Long time, final Integer offset){
            this.TIME = time;
            this.OFFSET= offset;
        }
    }


    public static MainEventTimes createMainEventTimes (final long accountId, final long inBedTime, final int inBedOffset,
                                                       final long sleepTime, final int sleepOffset, final long wakeUpTime,
                                                       final int wakeUpOffset, final long outOfBedTime, final int outOfBedOffset,
                                                       final long createdAt){
        final DateTime inBedTimeLocalUTC = new DateTime(inBedTime + inBedOffset, DateTimeZone.UTC);
        final SleepPeriod sleepPeriod = SleepPeriod.createSleepPeriod(inBedTimeLocalUTC);
        final EventTime inBedEventTime = new EventTime(inBedTime, inBedOffset);
        final EventTime sleepEventTime = new EventTime(sleepTime, sleepOffset);
        final EventTime wakeUpEventTime = new EventTime(wakeUpTime, wakeUpOffset);
        final EventTime outOfBedEventTime = new EventTime(outOfBedTime, outOfBedOffset);
        final ImmutableMap<Event.Type, EventTime> eventTimeMap = ImmutableMap.<Event.Type, EventTime>builder()
                .put(Event.Type.IN_BED, inBedEventTime)
                .put(Event.Type.SLEEP, sleepEventTime)
                .put(Event.Type.WAKE_UP, wakeUpEventTime)
                .put(Event.Type.OUT_OF_BED, outOfBedEventTime)
                .build();
        return new MainEventTimes(accountId, sleepPeriod,createdAt, eventTimeMap);
    }

    public static MainEventTimes createMainEventTimes (final long accountId, final SleepPeriod sleepPeriod, final long createdAt,
                                                       final TimelineAlgorithmResult timelineAlgorithmResult){
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

            return new MainEventTimes(accountId, sleepPeriod, createdAt,eventTimeMap);
        }
        return new MainEventTimes(accountId, sleepPeriod, createdAt, new HashMap<>());
    }

    public static MainEventTimes createMainEventTimes (final long accountId, final SleepPeriod sleepPeriod, final long createdAt, final Map<Event.Type, EventTime> mainEventTimeMap){
        return new MainEventTimes(accountId, sleepPeriod, createdAt, mainEventTimeMap);
    }


    public static MainEventTimes createMainEventTimesEmpty (final long accountId, final SleepPeriod sleepPeriod, final long createdAt){
        return new MainEventTimes(accountId, sleepPeriod, createdAt,  new HashMap<>());
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
                eventTimeMap.get(Event.Type.IN_BED).TIME,
                eventTimeMap.get(Event.Type.IN_BED).TIME +DateTimeConstants.MILLIS_PER_MINUTE,
                eventTimeMap.get(Event.Type.IN_BED).OFFSET,
                Optional.of(English.IN_BED_MESSAGE),
                Optional.<SleepSegment.SoundInfo>absent(),
                Optional.<Integer>absent()));

        mainEvents.add(Event.createFromType(Event.Type.SLEEP,
                eventTimeMap.get(Event.Type.SLEEP).TIME,
                eventTimeMap.get(Event.Type.SLEEP).TIME +DateTimeConstants.MILLIS_PER_MINUTE,
                eventTimeMap.get(Event.Type.SLEEP).OFFSET,
                Optional.of(English.FALL_ASLEEP_MESSAGE),
                Optional.<SleepSegment.SoundInfo>absent(),
                Optional.<Integer>absent()));

        mainEvents.add(Event.createFromType(Event.Type.WAKE_UP,
                eventTimeMap.get(Event.Type.WAKE_UP).TIME,
                eventTimeMap.get(Event.Type.WAKE_UP).TIME +DateTimeConstants.MILLIS_PER_MINUTE,
                eventTimeMap.get(Event.Type.WAKE_UP).OFFSET,
                Optional.of(English.WAKE_UP_MESSAGE),
                Optional.<SleepSegment.SoundInfo>absent(),
                Optional.<Integer>absent()));

        mainEvents.add(Event.createFromType(Event.Type.OUT_OF_BED,
                eventTimeMap.get(Event.Type.OUT_OF_BED).TIME,
                eventTimeMap.get(Event.Type.OUT_OF_BED).TIME +DateTimeConstants.MILLIS_PER_MINUTE,
                eventTimeMap.get(Event.Type.OUT_OF_BED).OFFSET,
                Optional.of(English.OUT_OF_BED_MESSAGE),
                Optional.<SleepSegment.SoundInfo>absent(),
                Optional.<Integer>absent()));

        return mainEvents;
    }

}
