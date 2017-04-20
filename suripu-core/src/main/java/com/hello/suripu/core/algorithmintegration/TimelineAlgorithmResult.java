package com.hello.suripu.core.algorithmintegration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.util.AlgorithmType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 1/20/16.
 */
public class TimelineAlgorithmResult {
    public final ImmutableMap<Event.Type,Event> mainEvents;
    public final ImmutableList<Event> extraEvents;
    public final AlgorithmType algorithmType;
    public final boolean timelineLockedDown;

    public TimelineAlgorithmResult(final AlgorithmType algorithmType, final List<Event> events, final Boolean timelineLockedDown) {
        this(algorithmType,events,Collections.<Event>emptyList(), timelineLockedDown);
    }

    public TimelineAlgorithmResult(final AlgorithmType algorithmType, final List<Event> mainEvents, final List<Event> extraEvents, final boolean timelineLockedDown) {
        final Map<Event.Type,Event> mainEventMap = Maps.newHashMap();

        for (final Event event : mainEvents) {
            mainEventMap.put(event.getType(),event);
        }

        this.extraEvents = ImmutableList.copyOf(extraEvents);
        this.mainEvents = ImmutableMap.copyOf(mainEventMap);
        this.algorithmType = algorithmType;
        this.timelineLockedDown = timelineLockedDown;
    }
}
