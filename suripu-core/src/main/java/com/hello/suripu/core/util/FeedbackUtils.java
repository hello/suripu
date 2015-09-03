package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.FallingAsleepEvent;
import com.hello.suripu.core.models.Events.InBedEvent;
import com.hello.suripu.core.models.Events.OutOfBedEvent;
import com.hello.suripu.core.models.Events.WakeupEvent;
import com.hello.suripu.core.models.TimelineFeedback;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;


public class FeedbackUtils {
    private static long MINUTE = 60000L;
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(FeedbackUtils.class);
    private final Logger LOGGER;

    public FeedbackUtils(final UUID uuid) {
        LOGGER = new LoggerWithSessionId(STATIC_LOGGER,uuid);
    }

    public FeedbackUtils() {
        LOGGER = new LoggerWithSessionId(STATIC_LOGGER);
    }

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
    public static Map<Event.Type, Long> getFeedbackAsNewTimesByType(final ImmutableList<TimelineFeedback> timelineFeedbackList, final Integer offsetMillis) {
        final Map<Event.Type, Long> eventTimesByType = Maps.newHashMap();

        /* iterate through list*/
        for(final TimelineFeedback timelineFeedback : timelineFeedbackList) {

            /* get datetime of the new time */
            final Optional<DateTime> optionalDateTime = convertFeedbackToDateTimeByNewTime(timelineFeedback, offsetMillis);

            if(optionalDateTime.isPresent()) {

                /* turn into event */
                final Optional<Event> event = fromFeedbackWithAdjustedDateTime(timelineFeedback, optionalDateTime.get(), offsetMillis);
                final Long timestampUTC = optionalDateTime.get().withZone(DateTimeZone.UTC).getMillis();

                eventTimesByType.put(event.get().getType(), timestampUTC);
            }
        }

        return eventTimesByType;
    }

    /* returns list of events by original event type */
    public static List<EventWithTime> getFeedbackEventsWithOriginalTime(final List<TimelineFeedback> timelineFeedbackList, final Integer offsetMillis) {

        /* this map will return items that are within PROXIMITY_FOR_FEEDBACK_MATCH_MILLISECONDS of the key */
        final List<EventWithTime> eventList =  Lists.newArrayList();


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


            eventList.add(new EventWithTime(oldTime.get().getMillis(), event.get(),EventWithTime.Type.FEEDBACK));

        }

        return eventList;
    }

    public static class ReprocessedEvents {
        final public ImmutableList<Event> mainEvents;
        final public ImmutableList<Event> extraEvents;

        public ReprocessedEvents(ImmutableList<Event> mainEvents, ImmutableList<Event> extraEvents) {
            this.mainEvents = mainEvents;
            this.extraEvents = extraEvents;
        }
    }





    private static class EventWithTime {
        enum Type {
            FEEDBACK,
            MAIN,
            EXTRA;
        }

        public final Long time;
        public final Event event;
        public final Type type;

        public EventWithTime(Long time, Event event,Type type) {
            this.time = time;
            this.event = event;
            this.type = type;
        }



    }

    private static class ScoredEventWithTimePair {
        public final EventWithTime e1;
        public final EventWithTime e2;
        public final double score;


        public ScoredEventWithTimePair(EventWithTime e1, EventWithTime e2) {
            this.e1 = e1;
            this.e2 = e2;
            this.score = Math.abs( (e1.time - e2.time) / 60000L);
        }
    }

    private static final Comparator<ScoredEventWithTimePair> lowestScoreComparator = new Comparator<ScoredEventWithTimePair>() {
        @Override
        public int compare(ScoredEventWithTimePair o1, ScoredEventWithTimePair o2) {
            if (o1.score > o2.score) {
                return 1;
            }

            if (o1.score < o2.score) {
                return -1;
            }

            return 0;
        }
    };

    private static EventWithTime mergeEvents(final ScoredEventWithTimePair pair) {


            //clone feedback, but changing the message from the default message to the alg-generated event's message
            final Event event = Event.createFromType(
                    pair.e1.event.getType(),
                    pair.e2.event.getStartTimestamp(),
                    pair.e2.event.getEndTimestamp(),
                    pair.e1.event.getTimezoneOffset(),
                    pair.e1.event.getDescription(),
                    pair.e1.event.getSoundInfo(),
                    pair.e1.event.getSleepDepth());


            return new EventWithTime(event.getStartTimestamp(),event,pair.e1.type);

    }

    static final Event copyEventWithNewTime(final Event event, final long newTime) {
        //create new event
        final Event newEvent = Event.createFromType(
                event.getType(),
                newTime,
                newTime + MINUTE,
                event.getTimezoneOffset(),
                event.getDescription(),
                event.getSoundInfo(),
                event.getSleepDepth());

        return newEvent;
    }

    public ReprocessedEvents reprocessEventsBasedOnFeedback(final ImmutableList<TimelineFeedback> timelineFeedbackList, final ImmutableList<Event> algEvents,final ImmutableList<Event> extraEvents, final Integer offsetMillis) {
        //so there will only ever by one feedback of a given type on a day
        final Map<Event.Type,Long> algTimesByEvent = getFeedbackAsNewTimesByType(timelineFeedbackList, offsetMillis);

        for (final Event event : algEvents) {
            if (algTimesByEvent.containsKey(event.getType())) {
                continue;
            }


            //lovely n^2 algorithm here
            if (!checkEventOrdering(algTimesByEvent,event.getStartTimestamp(),event.getType(),offsetMillis)) {
                //ruh-roh, consistency violation

                //suggest a new time... this should work unless we are processing an event type that should not be there
                final Optional<Long> newEventTimeOptional = suggestNewEventTypeBasedOnIntendedOrdering(algTimesByEvent,event.getStartTimestamp(),event.getType());

                if (!newEventTimeOptional.isPresent()) {
                    continue; //ignore this one
                }

                //create new event
                final Event newEvent = copyEventWithNewTime(event,newEventTimeOptional.get());

                if (!checkEventOrdering(algTimesByEvent,newEvent.getStartTimestamp(),newEvent.getType(),offsetMillis)) {
                    //this should not happen evar.
                    LOGGER.error("suggested event time is not consistent.  bad programmer!");

                    //just insert with original timestamp is and continue
                    algTimesByEvent.put(event.getType(), event.getStartTimestamp());
                    continue;
                }

                //we got this far? that means everything is good, so insert
                algTimesByEvent.put(newEvent.getType(), newEvent.getStartTimestamp());
            }
        }


        //now take the alg events, and insert by the mapped times
        //the mapped times will be the feedback times, or other adjustments that were made
        //to enforce consistency
        final Map<Event.Type,Event> eventsByType = Maps.newHashMap();

        for (final Event event : algEvents) {
            final Long newTime = algTimesByEvent.get(event.getType());

            if (newTime == null) {
                LOGGER.error("somehow got a mapped time that was null");
                continue; //this should never happen
            }

            eventsByType.put(event.getType(),copyEventWithNewTime(event,newTime));
        }

        //turn events map in to events list
        final List<Event> mainEvents = Lists.newArrayList();

        for (final Map.Entry<Event.Type,Event> entry : eventsByType.entrySet()) {
            mainEvents.add(entry.getValue());
        }

        return new ReprocessedEvents(ImmutableList.copyOf(mainEvents),ImmutableList.copyOf(Collections.EMPTY_LIST));
    }


    public ReprocessedEvents reprocessEventsBasedOnFeedbackTheOldWay(final ImmutableList<TimelineFeedback> timelineFeedbackList, final ImmutableList<Event> algEvents,final ImmutableList<Event> extraEvents, final Integer offsetMillis) {

        // get events by time
        final  List<EventWithTime> feedbackEventByOriginalTime = getFeedbackEventsWithOriginalTime(timelineFeedbackList, offsetMillis);

        Map<Event.Type,Set<EventWithTime>> algEventsByType = Maps.newHashMap();
        Map<Event.Type,Set<EventWithTime>> feedbackEventsByType = Maps.newHashMap();


        //populate maps

        //populate feedback multimap with feedback events, organized by type of event
        for (final EventWithTime event : feedbackEventByOriginalTime) {
            if (!feedbackEventsByType.containsKey(event.event.getType())) {
                feedbackEventsByType.put(event.event.getType(),new HashSet<EventWithTime>());
            }

            feedbackEventsByType.get(event.event.getType()).add(event);

        }

        //populate alg multimap with "main" alg events, organized by type
        for (final Event event : algEvents) {
            if (!algEventsByType.containsKey(event.getType())) {
                algEventsByType.put(event.getType(),new HashSet<EventWithTime>());
            }

            algEventsByType.get(event.getType()).add(new EventWithTime(event.getStartTimestamp(),event,EventWithTime.Type.MAIN));
        }

        //populate alg multimap with "extra" alg events, organized by type
        for (final Event event : extraEvents) {
            if (!algEventsByType.containsKey(event.getType())) {
                algEventsByType.put(event.getType(),new HashSet<EventWithTime>());
            }

            algEventsByType.get(event.getType()).add(new EventWithTime(event.getStartTimestamp(),event,EventWithTime.Type.EXTRA));
        }

        final List<Event> newAlgEvents = Lists.newArrayList();
        final List<Event> newExtraEvents = Lists.newArrayList();

        //match up feedback with alg events
        for (final Event.Type key : algEventsByType.keySet()) {
            final Set<EventWithTime> feedbacksOfOneType = feedbackEventsByType.get(key);

            if (feedbacksOfOneType == null) {
                continue;
            }


            final Set<EventWithTime> algeventsOfOneType = algEventsByType.get(key);

            while (!algeventsOfOneType.isEmpty() && !feedbacksOfOneType.isEmpty()) {
                final List<ScoredEventWithTimePair> scoredPairs = Lists.newArrayList();

                //SCORE AND REMOVE
                for (final EventWithTime aevent : algeventsOfOneType) {
                    for (final EventWithTime feedback : feedbacksOfOneType) {
                        scoredPairs.add(new ScoredEventWithTimePair(aevent, feedback));
                    }
                }

                Collections.sort(scoredPairs, lowestScoreComparator);

                //lowest score!
                final ScoredEventWithTimePair best = scoredPairs.get(0);

                //remove matches
                algeventsOfOneType.remove(best.e1);
                feedbacksOfOneType.remove(best.e2);


                //add event
                final EventWithTime mergedEvent = mergeEvents(best);

                final String type = best.e1.event.getType().toString();
                final DateTime oldTime = new DateTime(best.e1.event.getStartTimestamp(),DateTimeZone.forOffsetMillis(best.e1.event.getTimezoneOffset()));
                final DateTime feedbackOldTime = new DateTime(best.e2.time,DateTimeZone.forOffsetMillis(best.e1.event.getTimezoneOffset()));
                final DateTime feedbackNewTime = new DateTime(best.e2.event.getStartTimestamp(),DateTimeZone.forOffsetMillis(best.e1.event.getTimezoneOffset()));

                final Long deltaOld = (feedbackOldTime.getMillis() - oldTime.getMillis()) / MINUTE;
                final Long deltaNew = (feedbackNewTime.getMillis() - oldTime.getMillis()) / MINUTE;

                LOGGER.info("matched {} min apart, and moved it {} minutes",deltaOld,deltaNew);
                LOGGER.info("matched {} at time {} to feedback at time {} moving to {}", type, oldTime, feedbackOldTime, feedbackNewTime);

                switch (mergedEvent.type) {

                    case FEEDBACK:
                        break;
                    case MAIN:
                        newAlgEvents.add(mergedEvent.event);
                        break;
                    case EXTRA:
                        newExtraEvents.add(mergedEvent.event);
                        break;
                }


            }

        }

        //add the remainder of alg events
        for (final Set<EventWithTime> eventWithTimes : algEventsByType.values()) {
            for (final EventWithTime event : eventWithTimes) {
                switch (event.type) {

                    case FEEDBACK:
                        break;
                    case MAIN:
                        newAlgEvents.add(event.event);
                        break;
                    case EXTRA:
                        newExtraEvents.add(event.event);
                        break;
                }

            }
        }

        return new ReprocessedEvents(ImmutableList.copyOf(newAlgEvents),ImmutableList.copyOf(newExtraEvents));

    }


    static private Integer eventTypeToOrder(final Event.Type type) {
        int order = -1;
        switch (type) {
            case IN_BED:
                order = 0;
                break;
            case SLEEP:
                order = 1;
                break;
            case WAKE_UP:
                order = 2;
                break;
            case OUT_OF_BED:
                order = 3;
                break;
        }
        return order;
    }

    static private TreeMap<Integer,Long> orderEventsByIntendedOrder(final Map<Event.Type,Long> algEventsByType) {
        final TreeMap<Integer,Long> eventTimeWithIntendedOrders = Maps.newTreeMap();

        //insert sort, sorted by the order events SHOULD be in
        for (final Map.Entry<Event.Type,Long> entry: algEventsByType.entrySet()) {
            final Long eventTime = entry.getValue();

            int order = eventTypeToOrder(entry.getKey());

            if (order != -1) {
                eventTimeWithIntendedOrders.put(order,eventTime);
            }
        }

        return eventTimeWithIntendedOrders;
    }

    public boolean checkEventOrdering(final ImmutableList<TimelineFeedback> existingFeedbacks,final long proposedEventTimeUTC, final Event.Type proposedEventType, final int tzOffset) {
        if (existingFeedbacks.isEmpty()) {
            return true;
        }

        //guarantee that there are only the four events (there should not be duplicates, and this will just pick one of the dupes if there happens to be one)
        final Map<Event.Type,Long> algTypesByTime = getFeedbackAsNewTimesByType(existingFeedbacks, tzOffset);

        return checkEventOrdering(algTypesByTime,proposedEventTimeUTC,proposedEventType,tzOffset);

    }

    private boolean checkEventOrdering(final Map<Event.Type,Long> algTypesByTimeInput,final long proposedEventTimeUTC, final Event.Type proposedEventType, final int tzOffset) {

        final Map<Event.Type,Long> algTypesByTime = Maps.newHashMap(algTypesByTimeInput);

        algTypesByTime.put(proposedEventType, proposedEventTimeUTC);

        final TreeMap<Integer,Long> eventsByIntendedOrder = orderEventsByIntendedOrder(algTypesByTime);

        //assert monotonically increasing
        long prevTime = 0;
        for (final Map.Entry<Integer,Long> eventTimeWithIntendedOrder : eventsByIntendedOrder.entrySet()) {
            if (eventTimeWithIntendedOrder.getValue() <= prevTime) {
                return false;
            }

            prevTime = eventTimeWithIntendedOrder.getValue();
        }

        return true;
    }

    public Optional<Long> suggestNewEventTypeBasedOnIntendedOrdering(final Map<Event.Type,Long> algTypesByTime,final long proposedEventTimeUTC,final Event.Type proposedEventType) {

        final int myOrder = eventTypeToOrder(proposedEventType);

        if (myOrder == -1) {
            return Optional.absent();
        }

        final Map<Event.Type,Long> copy = Maps.newHashMap(algTypesByTime);
        copy.put(proposedEventType,proposedEventTimeUTC);

        final TreeMap<Integer,Long> eventsByIntendedOrder = orderEventsByIntendedOrder(copy);
        final Map.Entry<Integer,Long> highEntry = eventsByIntendedOrder.higherEntry(myOrder);
        final Map.Entry<Integer,Long> lowerEntry = eventsByIntendedOrder.lowerEntry(myOrder);


        boolean failHigh = false;
        if (highEntry != null) {
            failHigh = proposedEventTimeUTC > highEntry.getValue();
        }

        boolean failLow = false;
        if (lowerEntry != null) {
            failLow = proposedEventTimeUTC < lowerEntry.getValue();
        }

        if (failHigh && failLow) {
            LOGGER.error("somehow event order is really really really screwed up");
            return Optional.of(proposedEventTimeUTC); //do nothing, just accept it.
        }

        if (failHigh) {
            return  Optional.of(highEntry.getValue() - MINUTE);
        }

        if (failLow) {
            return Optional.of(lowerEntry.getValue() + MINUTE);
        }

        //should also never get here either, but whatever
        LOGGER.warn("tried suggesting a new time for something that was not originally incosistent");
        return Optional.of(proposedEventTimeUTC);
    }

}
