package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.FallingAsleepEvent;
import com.hello.suripu.core.models.Events.InBedEvent;
import com.hello.suripu.core.models.Events.OutOfBedEvent;
import com.hello.suripu.core.models.Events.WakeupEvent;
import com.hello.suripu.core.models.SleepPeriod;
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
    private static int INVALID_EVENT_ORDER = -1;
    private final Logger LOGGER;
    private static int INBED_SLEEP_EVENT_LOWER_BOUND_OFFSET = -2;
    private static int INBED_SLEEP_EVENT_UPPER_BOUND_OFFSET = 4;

    private static int WAKE_OUTOFBED_EVENT_LOWER_BOUND_OFFSET = 0;
    private static int WAKE_OUTOFBED_EVENT_UPPER_BOUND_OFFSET = 4;

    public FeedbackUtils(final Optional<UUID> uuid) {
        LOGGER = new LoggerWithSessionId(STATIC_LOGGER,uuid);
    }

    public FeedbackUtils() {
        LOGGER = new LoggerWithSessionId(STATIC_LOGGER);
    }

    public static Optional<DateTime> convertFeedbackToDateTimeByNewTime(final TimelineFeedback feedback, final Integer offsetMillis) {

        return convertFeedbackStringToDateTime(feedback.eventType,feedback.dateOfNight,feedback.sleepPeriod,feedback.newTimeOfEvent,offsetMillis);

    }

    public static Optional<DateTime> convertFeedbackToDateTimeByOldTime(final TimelineFeedback feedback, final Integer offsetMillis) {

        return convertFeedbackStringToDateTime(feedback.eventType,feedback.dateOfNight, feedback.sleepPeriod,feedback.oldTimeOfEvent,offsetMillis);

    }


    private static Optional<DateTime> convertFeedbackStringToDateTime(final Event.Type eventType, final DateTime dateOfNight, final SleepPeriod.Period period,  final String feedbacktime, final Integer offsetMillis) {
        //OLD
        // in bed can not be after after noon AND before 8PM
        // same for fall asleep
        // Wake up has to be after midnight (day +1) and before noon
        // same for out of bed
        final SleepPeriod sleepPeriod = SleepPeriod.createSleepPeriod(period, dateOfNight);
        final String[] parts = feedbacktime.split(":");
        final Integer hour = Integer.valueOf(parts[0]);
        final Integer minute = Integer.valueOf(parts[1]);

        final int inbedSleepLowerBound = sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.START, offsetMillis).getHourOfDay() + INBED_SLEEP_EVENT_LOWER_BOUND_OFFSET;
        final int inbedSleepUpperBound = sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.END_DATA, offsetMillis).getHourOfDay() + INBED_SLEEP_EVENT_UPPER_BOUND_OFFSET;
        final boolean inbedSleepWindowSpansDay = inbedSleepLowerBound > inbedSleepUpperBound;

        final int wakeOutOfBedLowerBound = sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.START, offsetMillis).getHourOfDay() + WAKE_OUTOFBED_EVENT_LOWER_BOUND_OFFSET;
        final int wakeOutOfBedUpperBound = sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.END_DATA, offsetMillis).getHourOfDay() + WAKE_OUTOFBED_EVENT_UPPER_BOUND_OFFSET;
        final boolean wakeOutOfBedWindowSpansDay = wakeOutOfBedLowerBound > wakeOutOfBedUpperBound;

        boolean nextDay = false;
        switch (eventType) {
            case IN_BED:
            case SLEEP:
                if (inbedSleepWindowSpansDay) {
                   if (hour >= 0 && hour < inbedSleepUpperBound) {
                        nextDay = true;
                    } else if( hour >= inbedSleepUpperBound && hour < inbedSleepLowerBound){
                       return Optional.absent();
                   }
                }else if (hour >= inbedSleepUpperBound || hour < inbedSleepLowerBound) {
                    return Optional.absent();
                }

                break;

            case WAKE_UP:
            case OUT_OF_BED:
                if(wakeOutOfBedWindowSpansDay){
                    if(hour >= 0 && hour < wakeOutOfBedUpperBound) {
                        nextDay =  true;
                    } else if ( hour >= wakeOutOfBedUpperBound && hour < wakeOutOfBedLowerBound){
                        return Optional.absent();
                    }
                } else if(hour >= wakeOutOfBedUpperBound || hour < wakeOutOfBedLowerBound){
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
                event = new WakeupEvent(feedback.sleepPeriod,
                        adjustedTime.getMillis(),
                        adjustedTime.plusMinutes(1).getMillis(),
                        offsetMillis
                );
                break;
            case SLEEP:
                event = new FallingAsleepEvent(feedback.sleepPeriod,
                        adjustedTime.getMillis(),
                        adjustedTime.plusMinutes(1).getMillis(),
                        offsetMillis
                );
                break;
            case IN_BED:
                event = new InBedEvent(feedback.sleepPeriod,
                        adjustedTime.getMillis(),
                        adjustedTime.plusMinutes(1).getMillis(),
                        offsetMillis
                );
                break;
            case OUT_OF_BED:
                event = new OutOfBedEvent(feedback.sleepPeriod,
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
    public static Map<Event.Type, Event> getFeedbackAsEventsByType(final ImmutableList<TimelineFeedback> timelineFeedbackList, final Integer offsetMillis) {
        final Map<Event.Type, Event> eventsByType = Maps.newHashMap();

        /* iterate through list*/
        for(final TimelineFeedback timelineFeedback : timelineFeedbackList) {

            /* get datetime of the new time */
            final Optional<DateTime> optionalDateTime = convertFeedbackToDateTimeByNewTime(timelineFeedback, offsetMillis);

            if(optionalDateTime.isPresent()) {
                /* turn into event */
                final Optional<Event> event = fromFeedbackWithAdjustedDateTime(timelineFeedback, optionalDateTime.get(), offsetMillis);

                if (!event.isPresent()) {
                    continue;
                }

                eventsByType.put(event.get().getType(), event.get());
            }
        }

        return eventsByType;
    }

    /* returns map of events by event type */
    public static Map<Event.Type, Event> getFeedbackAsEventsByType(final SleepPeriod.Period period, final ImmutableList<TimelineFeedback> timelineFeedbackList, final TimeZoneOffsetMap timeZoneOffsetMap) {
        final Map<Event.Type, Event> eventsByType = Maps.newHashMap();
        /* iterate through list*/
        for(final TimelineFeedback timelineFeedback : timelineFeedbackList) {
            //if feedback for different period, skip
            if(timelineFeedback.sleepPeriod != period){
                continue;
            }
            /* get datetime of the new time */
            final Optional<DateTime> optionalDateTime = convertFeedbackToDateTimeByNewTime(timelineFeedback, 0);

            if(optionalDateTime.isPresent()) {
                final Long timeUTC = timeZoneOffsetMap.getUTCFromLocalTime(optionalDateTime.get().getMillis());

                final int offset = timeZoneOffsetMap.getOffsetWithDefaultAsZero(timeUTC);

                final DateTime dateTime = optionalDateTime.get().minusMillis(offset);
            /* turn into event */
                final Optional<Event> event = fromFeedbackWithAdjustedDateTime(timelineFeedback, dateTime, offset);

                if (!event.isPresent()) {
                    continue;
                }

                eventsByType.put(event.get().getType(), event.get());

            }
        }

        return eventsByType;
    }



    /* returns list of events by original event type */
    public static Map<Event.Type,Long> getTimesFromEventsMap(final Map<Event.Type, Event> eventsByType) {
        final Map<Event.Type,Long> eventTimesByType = Maps.newHashMap();

        for (final Map.Entry<Event.Type,Event> event : eventsByType.entrySet()) {
            eventTimesByType.put(event.getKey(),event.getValue().getStartTimestamp());
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
        final public ImmutableMap<Event.Type,Event> mainEvents;
        final public ImmutableList<Event> extraEvents;

        public ReprocessedEvents(ImmutableList<Event> mainEvents, ImmutableList<Event> extraEvents) {
            final Map<Event.Type,Event> eventMap = Maps.newHashMap();

            for (final Event event : mainEvents) {
                eventMap.put(event.getType(),event);
            }

            this.mainEvents = ImmutableMap.copyOf(eventMap);
            this.extraEvents = extraEvents;
        }
    }





    public static class EventWithTime {
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
                event.getSleepPeriod(),
                newTime,
                newTime + MINUTE,
                event.getTimezoneOffset(),
                event.getDescription(),
                event.getSoundInfo(),
                event.getSleepDepth());

        return newEvent;
    }

    public ReprocessedEvents reprocessEventsBasedOnFeedback(final SleepPeriod.Period period, final ImmutableList<TimelineFeedback> timelineFeedbackList, final ImmutableCollection<Event> algEvents, final ImmutableList<Event> extraEvents, final TimeZoneOffsetMap timeZoneOffsetMap) {


        /* get feedback events by time  */
        final Map<Event.Type,Event> eventsByType = getFeedbackAsEventsByType(period, timelineFeedbackList, timeZoneOffsetMap);


        for (final Event event : algEvents) {

            //if alg event is already present in feedback, skip
            if (eventsByType.containsKey(event.getType())) {
                continue;
            }


            if (checkEventOrdering(eventsByType,event.getStartTimestamp(),event.getType(), 0)) {
                //alles ist gut jawohl, so insert event
                eventsByType.put(event.getType(), event);
            }
            else {
                //if consistency violation with existing events, then suggest new event time
                //suggest a new time... this should work unless we are processing an event type that should not be there

                final Optional<Long> newEventTimeOptional = suggestNewEventTimeBasedOnIntendedOrdering(getTimesFromEventsMap(eventsByType), event.getStartTimestamp(), event.getType());

                if (!newEventTimeOptional.isPresent()) {
                    continue; //ignore this one
                }

                //create new event
                final Event newEvent = copyEventWithNewTime(event,newEventTimeOptional.get());

                if (!checkEventOrdering(eventsByType,newEvent.getStartTimestamp(),newEvent.getType(), 0)) {
                    //this should not happen evar.
                    LOGGER.error("suggested event time is not consistent.  bad programmer!");

                    //just insert with original timestamp is and continue
                    eventsByType.put(event.getType(), event);
                    continue;
                }

                //we got this far? that means everything is now good, so insert
                eventsByType.put(newEvent.getType(), newEvent);
            }
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
        for (final EventWithTime event : feedbackEventByOriginalTime) {
            if (!feedbackEventsByType.containsKey(event.event.getType())) {
                feedbackEventsByType.put(event.event.getType(),new HashSet<EventWithTime>());
            }

            feedbackEventsByType.get(event.event.getType()).add(event);

        }

        for (final Event event : algEvents) {
            if (!algEventsByType.containsKey(event.getType())) {
                algEventsByType.put(event.getType(),new HashSet<EventWithTime>());
            }

            algEventsByType.get(event.getType()).add(new EventWithTime(event.getStartTimestamp(),event,EventWithTime.Type.MAIN));
        }

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

        return  new ReprocessedEvents(ImmutableList.copyOf(newAlgEvents),ImmutableList.copyOf(newExtraEvents));

    }


    //turns enum in to an integer based on the expected order of the event
    static private Integer eventTypeToOrder(final Event.Type type) {
        int order = INVALID_EVENT_ORDER;
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

    //given a map of type/time, turn the type enum into its expected order, and insert into a map order/time
    static private TreeMap<Integer,Long> orderEventsByIntendedOrder(final Map<Event.Type,Long> algEventsByType) {
        final TreeMap<Integer,Long> eventTimeWithIntendedOrders = Maps.newTreeMap();

        //insert sort, sorted by the order events SHOULD be in
        for (final Map.Entry<Event.Type,Long> entry: algEventsByType.entrySet()) {
            final Long eventTime = entry.getValue();

            int order = eventTypeToOrder(entry.getKey());

            if (order != INVALID_EVENT_ORDER) {
                eventTimeWithIntendedOrders.put(order,eventTime);
            }
        }

        return eventTimeWithIntendedOrders;
    }

    //given a bunch of feedback events, and a proposed new event, make sure that they appear in the right order, or return false

    public boolean checkEventsValidity(final ImmutableList<TimelineFeedback> existingFeedbacks, final int offsetMillis) {
        for (final TimelineFeedback feedback : existingFeedbacks) {

            if (!checkEventValidity(feedback,offsetMillis)) {
                return false;
            }

        }

        return true;
    }

    public boolean checkEventValidity(final TimelineFeedback feedback, final int offsetMillis) {

        final Optional<DateTime> optionalDateTime = convertFeedbackToDateTimeByNewTime(feedback, offsetMillis);

        if (!optionalDateTime.isPresent()) {
            return false;
        }

        final Optional<Event> event = fromFeedbackWithAdjustedDateTime(feedback, optionalDateTime.get(), offsetMillis);

        if (!event.isPresent()) {
            return false;
        }

        return true;
    }

    public boolean checkEventOrdering(final ImmutableList<TimelineFeedback> existingFeedbacks,final TimelineFeedback proposedFeedback, final int tzOffset) {
        if (existingFeedbacks.isEmpty()) {
            return true;
        }

        final Optional<DateTime> proposedFeedbackTimeOptional = convertFeedbackToDateTimeByNewTime(proposedFeedback,tzOffset);

        if (!proposedFeedbackTimeOptional.isPresent()) {
            return false; //invalid time somehow
        }

        final long proposedEventTimeUTC = proposedFeedbackTimeOptional.get().withZone(DateTimeZone.UTC).getMillis();

        //guarantee that there are only the four events (there should not be duplicates, and this will just pick one of the dupes if there happens to be one)
        final Map<Event.Type,Event> eventsByType = getFeedbackAsEventsByType(existingFeedbacks, tzOffset);


        return checkEventOrdering(eventsByType,proposedEventTimeUTC,proposedFeedback.eventType,tzOffset);

    }


    //given a bunch of events (from feedback, or from alg), and a proposed new event, make sure that they appear in the right order, or return false
    private boolean checkEventOrdering(final Map<Event.Type,Event> eventsByType,final long proposedEventTimeUTC, final Event.Type proposedEventType, final int tzOffset) {

        final Map<Event.Type,Long> algTypesByTime = Maps.newHashMap(getTimesFromEventsMap(eventsByType));

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

    //given a bunch of of events (from feedback, or from alg), and a proposed new event, suggest a new time for it that is valid
    //this gets run after "checkEventOrdering" returns false, but you still want to have the event be around
    //event will be put to one minute on the correct side of an event
    //for example, I already have WAKE at 8am, and somehow SLEEP is proposed for 9am, this will put SLEEP at 8:59am
    //another example, I have WAKE at 8am, and OUT_OF_BED is proposed for 7am, OUT_OF_BED will be placed at 8:01am.
    public Optional<Long> suggestNewEventTimeBasedOnIntendedOrdering(final Map<Event.Type, Long> algTimesByType, final long proposedEventTimeUTC, final Event.Type proposedEventType) {

        //get the desired place in the ordering of the proposed event
        //i.e. IN_BED is 1, SLEEP is 2, WAKE is 3, OUT_OF_BED is 4
        final int myOrder = eventTypeToOrder(proposedEventType);

        //check validity of ordering
        if (myOrder == INVALID_EVENT_ORDER) {
            return Optional.absent(); //this should never happen
        }

        //make copy of algTimesByType
        final Map<Event.Type,Long> copy = Maps.newHashMap(algTimesByType);
        copy.put(proposedEventType,proposedEventTimeUTC);

        //put alg times in order
        final TreeMap<Integer,Long> eventsByIntendedOrder = orderEventsByIntendedOrder(copy);

        //get the event after and before the proposed event based on event ordering
        //so if myOrder = 2 (SLEEP), then I would get events 1 and 3
        final Map.Entry<Integer,Long> highEntry = eventsByIntendedOrder.higherEntry(myOrder);
        final Map.Entry<Integer,Long> lowerEntry = eventsByIntendedOrder.lowerEntry(myOrder);

        //did my event times violate the desired sequence
        //for example, if myOrder = 2 (SLEEP), did the sleep time come after the wake time (event order 3)?
        boolean failHigh = false;
        if (highEntry != null) {
            failHigh = proposedEventTimeUTC >= highEntry.getValue();
        }

        //same thing for lower time
        boolean failLow = false;
        if (lowerEntry != null) {
            failLow = proposedEventTimeUTC <= lowerEntry.getValue();
        }

        if (failHigh && failLow) {
            //this can happen if the high event and low event are the same times
            //which means the user is doing something dumb, soo..... just accept it
            return Optional.of(proposedEventTimeUTC); //do nothing
        }

        //if I violated the sequence of the event that comes after, then place the propsoed event one minute before that event
        if (failHigh) {
            final int indexDiff = myOrder - highEntry.getKey();
            return  Optional.of(highEntry.getValue() + indexDiff * MINUTE);
        }

        //as above, but for the event that comes before
        if (failLow) {
            final int indexDiff = myOrder - lowerEntry.getKey();
            return Optional.of(lowerEntry.getValue() + indexDiff*MINUTE);
        }

        //should also never get here either, but whatever
        LOGGER.warn("tried suggesting a new time for something that was not originally inconsistent");
        return Optional.of(proposedEventTimeUTC);
    }

}
