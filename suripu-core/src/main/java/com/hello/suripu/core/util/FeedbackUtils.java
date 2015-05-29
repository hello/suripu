package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.FallingAsleepEvent;
import com.hello.suripu.core.models.Events.InBedEvent;
import com.hello.suripu.core.models.Events.OutOfBedEvent;
import com.hello.suripu.core.models.Events.WakeupEvent;
import com.hello.suripu.core.models.TimelineFeedback;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FeedbackUtils {

    public static Optional<DateTime> convertFeedbackToDateTimeByNewTime(final TimelineFeedback feedback, final Integer offsetMillis) {

        return convertFeedbackStringToDateTime(feedback.eventType,feedback.dateOfNight,feedback.newTimeOfEvent,offsetMillis);

    }

    public static Optional<DateTime> convertFeedbackToDateTimeByOldTime(final TimelineFeedback feedback, final Integer offsetMillis) {

        return convertFeedbackStringToDateTime(feedback.eventType,feedback.dateOfNight,feedback.oldTimeOfEvent,offsetMillis);

    }


    private static Optional<DateTime> convertFeedbackStringToDateTime(final Event.Type eventType,final DateTime dateOfNight, final String feedbacktime, final Integer offsetMillis) {
        // in bed can not be after after noon AND before 8PM
        // same for fall asleep
        // Wake up has to be after midnight (day +1) and before noon
        // same for out of bed
        final String[] parts = feedbacktime.split(":");
        final Integer hour = Integer.valueOf(parts[0]);
        final Integer minute = Integer.valueOf(parts[1]);

        boolean nextDay = false;
        switch (eventType) {
            case IN_BED:
            case SLEEP:
                if(hour >= 0 && hour < 16) {
                    nextDay =  true;
                } else if(hour >= 16 && hour < 18) {
                    return Optional.absent();
                }

                break;
            case WAKE_UP:
            case OUT_OF_BED:
                if(hour >= 0 && hour < 16) {
                    nextDay =  true;
                } else {
                    return Optional.absent();
                }
                break;
        }

        DateTime dateTimeOfEvent = dateOfNight;
        if(nextDay) {
            dateTimeOfEvent = dateTimeOfEvent.plusDays(1);
        }

        final DateTime converted = new DateTime(
                dateTimeOfEvent.getYear(),
                dateTimeOfEvent.getMonthOfYear(),
                dateTimeOfEvent.getDayOfMonth(),
                hour,
                minute,
                0,
                DateTimeZone.UTC).minusMillis(offsetMillis);
        return Optional.of(converted);
    }


    public static Optional<Event> fromFeedbackWithAdjustedDateTime(final TimelineFeedback feedback, final DateTime adjustedTime, final Integer offsetMillis) {
        Event event;
        switch (feedback.eventType) {
            case WAKE_UP:
                event = new WakeupEvent(
                        adjustedTime.getMillis(),
                        adjustedTime.plusMinutes(1).getMillis(),
                        offsetMillis
                );
                break;
            case SLEEP:
                event = new FallingAsleepEvent(
                        adjustedTime.getMillis(),
                        adjustedTime.plusMinutes(1).getMillis(),
                        offsetMillis
                );
                break;
            case IN_BED:
                event = new InBedEvent(
                        adjustedTime.getMillis(),
                        adjustedTime.plusMinutes(1).getMillis(),
                        offsetMillis
                );
                break;
            case OUT_OF_BED:
                event = new OutOfBedEvent(
                        adjustedTime.getMillis(),
                        adjustedTime.plusMinutes(1).getMillis(),
                        offsetMillis
                );
                break;
            default:
                return Optional.absent();
        }

        return Optional.of(event);
    }

    /* returns map of events by event type */
    public static Map<Event.Type, Event> getFeedbackAsEventMap(final List<TimelineFeedback> timelineFeedbackList, final Integer offsetMillis) {
        final Map<Event.Type, Event> events = Maps.newHashMap();

        /* iterate through list*/
        for(final TimelineFeedback timelineFeedback : timelineFeedbackList) {

            /* get datetime of the new time */
            final Optional<DateTime> optionalDateTime = convertFeedbackToDateTimeByNewTime(timelineFeedback, offsetMillis);

            if(optionalDateTime.isPresent()) {

                /* turn into event */
                final Optional<Event> event = fromFeedbackWithAdjustedDateTime(timelineFeedback, optionalDateTime.get(), offsetMillis);
                events.put(event.get().getType(), event.get());
            }
        }
        return events;
    }

    /* returns map of events by original event type */
    public static Map<Long,Event> getFeedbackEventsInOriginalTimeMap(final List<TimelineFeedback> timelineFeedbackList, final Integer offsetMillis) {

        /* this map will return items that are within PROXIMITY_FOR_FEEDBACK_MATCH_MILLISECONDS of the key */
        final Map<Long,Event> eventMap =  Maps.newHashMap();


        /* iterate through list*/
        for(final TimelineFeedback timelineFeedback : timelineFeedbackList) {

            /* get datetime of the new time */
            final Optional<DateTime> oldTime = convertFeedbackToDateTimeByOldTime(timelineFeedback, offsetMillis);
            final Optional<DateTime> newTime = convertFeedbackToDateTimeByNewTime(timelineFeedback, offsetMillis);

            if (!oldTime.isPresent() || !newTime.isPresent()) {
                continue;
            }

            /* turn into event */
            final Optional<Event> event = fromFeedbackWithAdjustedDateTime(timelineFeedback, newTime.get(), offsetMillis);

            if (!event.isPresent()) {
                continue;
            }


            eventMap.put(oldTime.get().getMillis(),event.get());

        }

        return eventMap;
    }

    public static class ReprocessedEvents {
        final public ImmutableList<Event> mainEvents;
        final public ImmutableList<Event> extraEvents;

        public ReprocessedEvents(ImmutableList<Event> mainEvents, ImmutableList<Event> extraEvents) {
            this.mainEvents = mainEvents;
            this.extraEvents = extraEvents;
        }
    }

    //we're going to hash this guy
    private static class TypeAndTime {
        final public Event.Type event;
        final public Long time;

        public TypeAndTime(Event.Type event, Long time) {
            this.event = event;
            this.time = time;
        }

        @Override
        public int hashCode() {
            return  (int) (time / 1000L) + event.getValue();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TypeAndTime)) {
                return false;
            }

            final TypeAndTime obj2 = (TypeAndTime) obj;

            return (obj2.event.equals(event) && obj2.time.equals(time) );
        }
    }

    private static ImmutableList<Event> remap (final Map<TypeAndTime,Long> mapping,final ImmutableList<Event> originalEvents) {
        final List<Event> mappedEvents = new ArrayList<>();

        for (final Event event : originalEvents) {

            final TypeAndTime typeAndTime = new TypeAndTime(event.getType(),event.getStartTimestamp());

            final Long mappedTime = mapping.get(typeAndTime);

            if (mappedTime == null) {
                mappedEvents.add(event);
                continue;
            }

            //remap

            //clone feedback, but changing the message from the default message to the alg-generated event's message
            mappedEvents.add(Event.createFromType(
                    event.getType(),
                    mappedTime,
                    mappedTime + 60000L,
                    event.getTimezoneOffset(),
                    event.getDescription(),
                    event.getSoundInfo(),
                    event.getSleepDepth()));

        }

        return ImmutableList.copyOf(mappedEvents);
    }

    public static ReprocessedEvents reprocessEventsBasedOnFeedback(final ImmutableList<TimelineFeedback> timelineFeedbackList, final ImmutableList<Event> algEvents,final ImmutableList<Event> extraEvents, final Integer offsetMillis) {


        /* get events by time  */
        final  Map<Long,Event> feedbackEventMapByOriginalTime = getFeedbackEventsInOriginalTimeMap(timelineFeedbackList,offsetMillis);

        Scorer longScorer = new Scorer<Long>() {
            @Override
            public Long getScore(Long o1, Long o2) {
                return Math.abs(o1-o2);
            }
        };

        Comparator longComparator = new Comparator<Long>() {

            @Override
            public int compare(Long o1, Long o2) {
                if (o1 < o2) {
                    return -1;
                }

                if (o1 > o2) {
                    return 1;
                }

                return 0;
            }
        };

        //track events by event type, and then the times.
        final BestMatchByTypeMap<Event.Type,Long> typeAndTimeMatcher = new BestMatchByTypeMap<>(longScorer,longComparator);

        for (final Event event : algEvents) {
            typeAndTimeMatcher.add(event.getType(),event.getStartTimestamp());
        }

        for (final Event event : extraEvents) {
            typeAndTimeMatcher.add(event.getType(),event.getStartTimestamp());
        }

        Map<TypeAndTime,Long> timeEventMapper = new HashMap<>();

        final Iterator<Long> it =  feedbackEventMapByOriginalTime.keySet().iterator();

        //go through feedback times, find matches by type and closest time
        //when you find the match, remove the item from our overly-complicated tracker map
        while (it.hasNext()) {
            final Long key = it.next(); //feedback original time
            final Event value = feedbackEventMapByOriginalTime.get(key);
            final Long newFeedbackTime = value.getStartTimestamp();

            //this matchedEventTime is the original alg-produced event time, the "key" is the feedback time
            //we are finding the closest original event time with this "key"
            //SO NOTE WE ARE MATCHING BY FEEDBACK ORIGINAL TIME
            Long matchedEventTime = typeAndTimeMatcher.getClosest(value.getType(),key);

            if (matchedEventTime == null) {
                continue;
            }

            //remove matched event
            typeAndTimeMatcher.remove(value.getType(),matchedEventTime);

            //store event old time, type, and new time
            //we will go back and lookup the original alg-produced event time and type to go find the newFeedbackTime
            timeEventMapper.put(new TypeAndTime(value.getType(),matchedEventTime), newFeedbackTime);
        }


        //operating under the assumption that all events type with time are mutually exclusive
        final ImmutableList<Event> newAlgEvents = remap(timeEventMapper,algEvents);

        final ImmutableList<Event> newExtraEvents = remap(timeEventMapper,extraEvents);

        return  new ReprocessedEvents(newAlgEvents,newExtraEvents);



    }

}
