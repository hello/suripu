package com.hello.suripu.core.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.util.Bucketing;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.MotionEvent;
import com.hello.suripu.core.models.Events.NullEvent;
import com.hello.suripu.core.models.Events.SleepingEvent;
import com.hello.suripu.core.models.SleepPeriod;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TimelineRefactored {

    private static final Logger LOGGER = LoggerFactory.getLogger(Bucketing.class);

    public static Map<Long, Event> populateTimeline(final List<MotionEvent> motionEventList, final TimeZoneOffsetMap timeZoneOffsetMap) {

        final Map<Long, Event> map = Maps.newHashMap();
        if (motionEventList.isEmpty()) {
            LOGGER.debug("Empty motion event list");
            return map;
        }

        final Long tsForFirstMotionEvent = motionEventList.get(0).getStartTimestamp();
        final Long tsForLastMotionEvent = motionEventList.get(motionEventList.size() -1).getStartTimestamp();
        final SleepPeriod.Period sleepPeriod = motionEventList.get(0).getSleepPeriod();

        final DateTime start = new DateTime(tsForFirstMotionEvent, DateTimeZone.UTC).withSecondOfMinute(0).withMillisOfSecond(0);
        final DateTime end = new DateTime(tsForLastMotionEvent, DateTimeZone.UTC).withSecondOfMinute(0).withMillisOfSecond(0);
        final Minutes numberOfMinutes = Minutes.minutesBetween(start, end);

        for(int i = 0; i < numberOfMinutes.getMinutes(); i++) {
            final DateTime key = start.plusMinutes(i);
            final Integer offset = timeZoneOffsetMap.getOffsetWithDefaultAsZero(key.getMillis());
            LOGGER.trace("Inserting {}", key);
            map.put(key.getMillis(), new SleepingEvent(sleepPeriod, key.getMillis(), key.plusMinutes(1).getMillis(), offset, 100));
        }

        for(final Event motionEvent : motionEventList) {
            map.put(motionEvent.getStartTimestamp(), motionEvent);
        }

        final List<Event> convertedMotionEvents = convertLightMotionToNone(motionEventList, 5);
        for(final Event motionEvent : convertedMotionEvents) {
            map.put(motionEvent.getStartTimestamp(), motionEvent);
        }
        LOGGER.trace("Map size = {}", map.size());

        return map;
    }


    public static List<Event> convertLightMotionToNone(final List<MotionEvent> eventList, final int thresholdSleepDepth){
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

    public static List<Event> mergeEvents(final Map<Long, Event> events) {
        final List<Event> mergedEvents = Lists.newArrayList();
        final List<Long> keyList = Lists.newArrayList(events.keySet());
        Collections.sort(keyList);
        final List<Event> buffer = Lists.newArrayList();
        Long tsOfLastMotionEvent = 0L;

        for(final Long ts : keyList) {
            final Event currentEvent = events.get(ts);
            if(currentEvent.getType() != Event.Type.SLEEPING && currentEvent.getType() != Event.Type.NONE) {


                final Long diffInMillis = currentEvent.getStartTimestamp() - tsOfLastMotionEvent;

                // Because otherwise timeline doesn't look great
                if(currentEvent.getType() == Event.Type.MOTION && diffInMillis < 60 * 15 * 1000L) {
                    buffer.add(currentEvent);
                } else {
                    if(buffer.size() > 8) {
                        final Event merged = merged(buffer);
                        mergedEvents.add(merged);
                    }
                    mergedEvents.add(currentEvent);
                    if(currentEvent.getType() == Event.Type.MOTION) {
                        tsOfLastMotionEvent = currentEvent.getEndTimestamp();
                    }
                    buffer.clear();
                }
                continue;
            }

            if(buffer.size() <= 20) {
                buffer.add(currentEvent);
                continue;
            }

            mergedEvents.add(merged(buffer));
            buffer.clear();
        }

        if(!buffer.isEmpty()) {
            final Event finalMergedEvent = merged(buffer);
            mergedEvents.add(finalMergedEvent);
        }
        return mergedEvents;
    }

    public static Event merged(final List<Event> events) {

        Integer minSleepDepth = events.get(0).getSleepDepth();
        for(final Event event : events) {
            if(event.getSleepDepth() < minSleepDepth && event.getSleepDepth() > 0) {
                minSleepDepth = event.getSleepDepth();
            }
        }

        return new NullEvent(events.get(0).getStartTimestamp(), events.get(events.size()-1).getEndTimestamp(), events.get(0).getTimezoneOffset(), minSleepDepth);
    }
}
