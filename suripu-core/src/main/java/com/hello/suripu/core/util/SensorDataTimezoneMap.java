package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.SleepSegment;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by benjo on 11/2/15.
 */
public class SensorDataTimezoneMap {

    public static SensorDataTimezoneMap create(final List<Sample> sensorData) {
        final TreeMap<Long,Integer> offsetByTimeUTC = Maps.newTreeMap();

        for (final Sample sample : sensorData) {
            offsetByTimeUTC.put(sample.dateTime, sample.offsetMillis);
        }

        return new SensorDataTimezoneMap(offsetByTimeUTC);
    }

    final TreeMap<Long,Integer> offsetByTimeUTC;



    private SensorDataTimezoneMap(final TreeMap<Long, Integer> offsetByTimeUTC) {
        this.offsetByTimeUTC = offsetByTimeUTC;
    }

    public List<Event> remapEventOffsets(final List<Event> events) {

        if (offsetByTimeUTC.isEmpty()) {
            return events;
        }

        final List<Event> newEvents = Lists.newArrayList();

        for (final Event event : events) {
            newEvents.add(Event.createFromType(event.getType(), event.getStartTimestamp(), event.getEndTimestamp(), this.get(event.getStartTimestamp()), event.getDescription(), event.getSoundInfo(), event.getSleepDepth()));
        }

        return newEvents;
    }

    public List<SleepSegment> remapSleepSegmentOffsets(final List<SleepSegment> segments) {

        if (offsetByTimeUTC.isEmpty()) {
            return segments;
        }

        final List<SleepSegment> newSegments = Lists.newArrayList();

        for (final SleepSegment segment : segments) {

            final int newOffset = this.get(segment.getTimestamp());

            newSegments.add(segment.createCopyWithNewOffset(newOffset));
        }

        return newSegments;
    }

    /* Get offset that is nearest in time to the timestamp */
    public Integer get(final long timestampUTC) {

        if (offsetByTimeUTC.isEmpty()) {
            return null;
        }

        //get entry >=
        Map.Entry<Long,Integer> higherEntry = offsetByTimeUTC.ceilingEntry(timestampUTC);

        //get entry <=
        Map.Entry<Long,Integer> lowerEntry = offsetByTimeUTC.floorEntry(timestampUTC);

        if (higherEntry == null) {
            higherEntry = lowerEntry;
        }

        if (lowerEntry == null) {
            lowerEntry = higherEntry;
        }


        //find nearest
        final long diffHigher = Math.abs(higherEntry.getKey() - timestampUTC);
        final long diffLower = Math.abs(lowerEntry.getKey() - timestampUTC);

        if (diffHigher > diffLower) {
            return lowerEntry.getValue();
        }
        else {
            return higherEntry.getValue();
        }

    }
}
