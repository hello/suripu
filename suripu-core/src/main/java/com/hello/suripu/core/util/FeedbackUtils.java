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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    public static ImmutableList<Event> reprocessEventsBasedOnFeedback(final ImmutableList<TimelineFeedback> timelineFeedbackList, final ImmutableList<Event> algEvents,final Integer offsetMillis) {
        List<Event> matchedEvents = new ArrayList<>();

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
        final Iterator<Long> it =  feedbackEventMapByOriginalTime.keySet().iterator();

        while (it.hasNext()) {
            final Long key = it.next();
            final Event value = feedbackEventMapByOriginalTime.get(key);

            typeAndTimeMatcher.add(value.getType(),key);
        }



        /* procedure: if extra event has match in map (type matches, and time matches), we replace it with the feedback
          *           otherwise, place extra event in results */

        for (final Event algEvent : algEvents) {

            //find the closest timestamp to the alg event that is of the same type
            Long closestTimestamp = typeAndTimeMatcher.getClosest(algEvent.getType(),algEvent.getStartTimestamp());

            if (closestTimestamp == null) {
                //no match, just add the event
                matchedEvents.add(algEvent);
                continue;
            }

            final Event feedbackEvent = feedbackEventMapByOriginalTime.get(closestTimestamp);

            if (feedbackEvent == null) {
                //no match, just add the event
                matchedEvents.add(algEvent);
                continue;
            }

            //match, replace the event

            //clone feedback, but changing the message from the default message to the alg-generated event's message
            matchedEvents.add(Event.createFromType(
                    feedbackEvent.getType(),
                    feedbackEvent.getStartTimestamp(),
                    feedbackEvent.getEndTimestamp(),
                    feedbackEvent.getTimezoneOffset(),
                    algEvent.getDescription(),
                    feedbackEvent.getSoundInfo(),
                    feedbackEvent.getSleepDepth()));

        }



        return ImmutableList.copyOf(matchedEvents);

    }
}
