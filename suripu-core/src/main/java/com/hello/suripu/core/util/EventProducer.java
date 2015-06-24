package com.hello.suripu.core.util;

import com.hello.suripu.core.models.Event;

import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 6/24/15.
 */
public interface EventProducer {
    public List<Event> getEventsFromProbabilitySequence(Map<String,List<List<Double>>> probsByOutpidId, double [][] sensorData, Long t0, Integer numMinutesPerInterval, Integer timezoneOffset);
}
