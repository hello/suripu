package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.OnlineHmmData;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.util.FeedbackUtils;
import org.eclipse.jetty.util.MultiMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by benjo on 8/23/15.
 *
 *   Turns feedback events into label sequences pairs 
 *    {t=42,label=1 },
 *    {t=43,label=1},
 *    {t=44,label=1},
 *    {t=45,label=2}, ... etc
 */
public class LabelMaker {
    private final static int GUARANTEED_SLEEP_PERIOD_FROM_ONE_LABEL = 180; //minutes
    private static final long NUMBER_OF_MILLIS_IN_A_MINUTE = 60000L;


    private static final int LABEL_PRE_SLEEP = 0;
    private static final int LABEL_DURING_SLEEP = 1;
    private static final int LABEL_POST_SLEEP = 2;
    private static final int LABEL_PRE_BED = 0;
    private static final int LABEL_DURING_BED = 1;
    private static final int LABEL_POST_BED = 2;


    public LabelMaker(final Optional<UUID> uuid) {

    }

    private int eventTimeToIndex(final long t, final long startTime, final int numMinutesPerPeriod) {
        return (int) ((t - startTime) / NUMBER_OF_MILLIS_IN_A_MINUTE / numMinutesPerPeriod);
    }

    private Map<Integer, Integer> labelEventPair(final long t1, final long t2, final long startTime, final long endTime, final int numMinutesPerPeriod, final int preState, final int duringState, final int postState) {

        final Map<Integer, Integer> labels = Maps.newHashMap();

        final int i1 = eventTimeToIndex(t1, startTime, numMinutesPerPeriod);
        final int i2 = eventTimeToIndex(t2, startTime, numMinutesPerPeriod);
        final int i3 = eventTimeToIndex(endTime, startTime, numMinutesPerPeriod);

        for (int i = 0; i < i1; i++) {
            labels.put(i, preState);
        }

        for (int i = i1; i < i2; i++) {
            labels.put(i, duringState);
        }

        for (int i = i2; i < i3; i++) {
            labels.put(i, postState);
        }

        return labels;

    }

    private Map<Integer, Integer> labelSingleEvent(final long t1, final long startTime, final long endTime, final int numMinutesPerPeriod, final int preState, final int postState, boolean isFirstEventInPair) {
        final Map<Integer, Integer> labels = Maps.newHashMap();

        final int i1 = eventTimeToIndex(t1, startTime, numMinutesPerPeriod);
        final int labelAssumedKnoweldgePeriod = GUARANTEED_SLEEP_PERIOD_FROM_ONE_LABEL / numMinutesPerPeriod;

        if (isFirstEventInPair) {
            for (int i = 0; i < i1; i++) {
                labels.put(i, preState);
            }

            for (int i = i1; i < i1 + labelAssumedKnoweldgePeriod; i++) {
                labels.put(i, postState);
            }
        } else {
            final int i3 = eventTimeToIndex(endTime, startTime, numMinutesPerPeriod);


            for (int i = i1 - labelAssumedKnoweldgePeriod; i < i1; i++) {
                labels.put(i, preState);
            }

            for (int i = i1; i < i3; i++) {
                labels.put(i, postState);
            }

        }

        return labels;
    }

    private static List<Long> eventsToTimestamps(final Collection<Event> events) {
        final List<Long> timestamps = Lists.newArrayList();

        if (events == null) {
            return timestamps;
        }

        for (final Event e : events) {
            timestamps.add(e.getStartTimestamp());
        }

        return timestamps;
    }

    public final Map<String,Map<Integer,Integer>> getLabelsFromEvent(final int tzOffset, final long startTime, final long endTime,
                                                                     final int numMinutesPerPeriod,final ImmutableList<TimelineFeedback> timelineFeedbacks) {

        final Map<Event.Type,Event> eventByType =  FeedbackUtils.getFeedbackAsEventsByType(timelineFeedbacks, tzOffset);

        final Multimap<Event.Type,Event> eventsByType = HashMultimap.create();

        for (final Map.Entry<Event.Type,Event> entry : eventByType.entrySet()) {
            eventsByType.put(entry.getKey(),entry.getValue());
        }

        final List<Long> wakeTimes = eventsToTimestamps(eventsByType.get(Event.Type.WAKE_UP));
        final List<Long> sleepTimes = eventsToTimestamps(eventsByType.get(Event.Type.SLEEP));
        final List<Long> inbedTimes = eventsToTimestamps(eventsByType.get(Event.Type.IN_BED));
        final List<Long> outofbedTimes = eventsToTimestamps(eventsByType.get(Event.Type.OUT_OF_BED));

        return getLabelsFromEvent(startTime,endTime,numMinutesPerPeriod,wakeTimes,sleepTimes);
    }


    public final Map<String,Map<Integer,Integer>> getLabelsFromEvent(final long startTime, final long endTime, final int numMinutesPerPeriod,
                                                                     final List<Long> wakes ,final List<Long> sleeps) {


        final Map<String,Map<Integer,Integer>> labelsByOutputId = Maps.newHashMap();

        //for the moment, assume number of events per type is only one or zero

        if (wakes.size() > 0 && sleeps.size() > 0) {
            final long wake = wakes.get(0);
            final long sleep = sleeps.get(0);

            final Map<Integer,Integer> labels = labelEventPair(sleep,wake,startTime,endTime,numMinutesPerPeriod,LABEL_PRE_SLEEP,LABEL_DURING_SLEEP,LABEL_POST_SLEEP);

            labelsByOutputId.put(OnlineHmmData.OUTPUT_MODEL_SLEEP,labels);
        }
        else if (wakes.size() > 0) {
            final long wake = wakes.get(0);

            final Map<Integer,Integer> labels = labelSingleEvent(wake,startTime,endTime,numMinutesPerPeriod,LABEL_DURING_SLEEP,LABEL_POST_SLEEP,false);

            labelsByOutputId.put(OnlineHmmData.OUTPUT_MODEL_SLEEP,labels);

        }
        else if (sleeps.size() > 0) {
            final long sleep = sleeps.get(0);

            final Map<Integer,Integer> labels = labelSingleEvent(sleep,startTime,endTime,numMinutesPerPeriod,LABEL_PRE_SLEEP,LABEL_DURING_SLEEP,true);

            labelsByOutputId.put(OnlineHmmData.OUTPUT_MODEL_SLEEP,labels);

        }


        return labelsByOutputId;
    }

}
