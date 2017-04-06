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
    /*
    Invalid / not present main event times are represented by a timestamp of 0L
     */

    public Long accountId;
    public final SleepPeriod sleepPeriod;
    public final ImmutableMap<Event.Type, EventTime> eventTimeMap;
    public final EventTime createdAt;

    private MainEventTimes (final Long accountId, final SleepPeriod sleepPeriod, final EventTime createdAt, Map<Event.Type, EventTime> eventTimeMap){
        this.accountId = accountId;
        this.sleepPeriod = sleepPeriod;
        this.createdAt = createdAt;
        this.eventTimeMap = ImmutableMap.copyOf(eventTimeMap);
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
                                                       final long createdAtTime, final int createdAtOffset){
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
        if (new MainEventTimes(accountId, sleepPeriod, createdAt, eventTimeMap).hasValidEventTimes()) {
            return new MainEventTimes(accountId, sleepPeriod, createdAt, eventTimeMap);
        }
        return createMainEventTimesEmpty(accountId, sleepPeriod, createdAtTime, createdAtOffset);
    }

    public static MainEventTimes createMainEventTimes (final long accountId, final SleepPeriod sleepPeriod, final long createdAtTime, final int createdAtOffset,
                                                       final TimelineAlgorithmResult timelineAlgorithmResult){
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

            if (new MainEventTimes(accountId, sleepPeriod, createdAt,eventTimeMap).hasValidEventTimes()) {
                return new MainEventTimes(accountId, sleepPeriod, createdAt, eventTimeMap);
            }
        }
        return createMainEventTimesEmpty(accountId, sleepPeriod, createdAtTime, createdAtOffset);
    }

    public static MainEventTimes createMainEventTimes (final long accountId, final SleepPeriod sleepPeriod, final long createdAtTime, final int createdAtOffset, final Map<Event.Type, EventTime> mainEventTimeMap){
        final EventTime createdAt = new EventTime(createdAtTime, createdAtOffset);
        if (new MainEventTimes(accountId, sleepPeriod, createdAt, mainEventTimeMap).hasValidEventTimes()) {
            return new MainEventTimes(accountId, sleepPeriod, createdAt, mainEventTimeMap);
        }
        else return createMainEventTimesEmpty(accountId, sleepPeriod, createdAtTime, createdAtOffset);
    }

    public static MainEventTimes createMainEventTimes (final long accountId, final SleepPeriod sleepPeriod, final Long createdAtTime, final int createdAtOffset, final List<SleepSegment> timelineEvents){
        long inBedTime = 0L;
        int inBedOffset = 0;
        long sleepTime= 0L;
        int sleepOffset= 0;
        long wakeUpTime = 0L;
        int wakeUpOffset = 0;
        long outOfBedTime = 0L;
        int outOfBedOffset = 0;
        for(final SleepSegment sleepSegment: timelineEvents){
            switch(sleepSegment.getType()){
                case IN_BED:
                    inBedTime = sleepSegment.getTimestamp();
                    inBedOffset = sleepSegment.getOffsetMillis();
                case SLEEP:
                    sleepTime = sleepSegment.getTimestamp();
                    sleepOffset = sleepSegment.getOffsetMillis();
                case WAKE_UP:
                    wakeUpTime = sleepSegment.getTimestamp();
                    wakeUpOffset = sleepSegment.getOffsetMillis();
                case OUT_OF_BED:
                    outOfBedTime = sleepSegment.getTimestamp();
                    outOfBedOffset = sleepSegment.getOffsetMillis();
                default: continue;
            }
        }
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
        if (new MainEventTimes(accountId, sleepPeriod,createdAt, eventTimeMap).hasValidEventTimes()) {
            return new MainEventTimes(accountId, sleepPeriod, createdAt, eventTimeMap);
        }
        return createMainEventTimesEmpty(accountId, sleepPeriod, createdAtTime, createdAtOffset);
    }

    public static MainEventTimes createMainEventTimesEmpty (final long accountId, final SleepPeriod sleepPeriod, final long createdAtTime, final int createdAtOffset){
        final EventTime createdAt = new EventTime(createdAtTime, createdAtOffset);
        final ImmutableMap<Event.Type, EventTime> eventTimeMap = ImmutableMap.<Event.Type, EventTime>builder()
                .put(Event.Type.IN_BED, new EventTime(0L, 0))
                .put(Event.Type.SLEEP, new EventTime(0L, 0))
                .put(Event.Type.WAKE_UP, new EventTime(0L, 0))
                .put(Event.Type.OUT_OF_BED, new EventTime(0L, 0))
                .build();
        return new MainEventTimes(accountId, sleepPeriod, createdAt,  eventTimeMap);
    }

    public boolean hasValidEventTimes(){
        final List<Event.Type> MAIN_EVENT_TYPES = Arrays.asList(Event.Type.IN_BED, Event.Type.SLEEP,Event.Type.WAKE_UP,Event.Type.OUT_OF_BED);

        for (final Event.Type eventType : MAIN_EVENT_TYPES){
            if (!this.eventTimeMap.containsKey(eventType)){
                return false;
            }
            if (this.eventTimeMap.get(eventType).time == 0){
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
                eventTimeMap.get(Event.Type.SLEEP).time +DateTimeConstants.MILLIS_PER_MINUTE,
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

    public static Map<SleepPeriod.Period, MainEventTimes> getSleepPeriodsMainEventTimesMapForDate(final List<MainEventTimes> mainEventTimesList, final DateTime date){
        final Map<SleepPeriod.Period, MainEventTimes> sleepEventsMap = new HashMap<>();
        for ( final MainEventTimes mainEventTimes : mainEventTimesList) {
            if (mainEventTimes.sleepPeriod.targetDate.withTimeAtStartOfDay().getMillis() == date.withTimeAtStartOfDay().getMillis()){
                sleepEventsMap.put(mainEventTimes.sleepPeriod.period, mainEventTimes);
            }
        }
        return sleepEventsMap;
    }

}
