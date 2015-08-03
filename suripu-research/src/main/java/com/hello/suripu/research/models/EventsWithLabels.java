package com.hello.suripu.research.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.Event;

import java.util.List;

/**
 * Created by benjo on 6/18/15.
 */
public class EventsWithLabels {

    public EventsWithLabels(List<Event> algEvents, List<Event> feedbackEvents) {
        this.algEvents = algEvents;
        this.feedbackEvents = feedbackEvents;
    }

    @JsonProperty("alg_events")
    public final List<Event> algEvents;

    @JsonProperty("feedback_events")
    public final List<Event> feedbackEvents;

}
