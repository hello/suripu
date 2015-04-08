package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.FallingAsleepEvent;
import com.hello.suripu.core.models.Events.InBedEvent;
import com.hello.suripu.core.models.Events.OutOfBedEvent;
import com.hello.suripu.core.models.Events.WakeupEvent;
import org.joda.time.DateTimeConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pangwu on 4/6/15.
 */
public class VotingSleepEvents {
    public final SleepEvents<Optional<Event>> sleepEvents;
    public final List<Event> extraEvents = new ArrayList<>();
    public VotingSleepEvents(final SleepEvents<Segment> segments, final List<Segment> awakes){
        final Segment goToBedSegment = segments.goToBed;
        final Segment fallAsleepSegment = segments.fallAsleep;
        final Segment wakeUpSegment = segments.wakeUp;
        final Segment outOfBedSegment = segments.outOfBed;

        //final int smoothWindowSizeInMillis = smoothWindowSizeInMinutes * DateTimeConstants.MILLIS_PER_MINUTE;
        final Optional<Event> inBedEvent = Optional.of((Event)new InBedEvent(goToBedSegment.getStartTimestamp(),
                goToBedSegment.getEndTimestamp(),
                goToBedSegment.getOffsetMillis()));

        final Optional<Event> fallAsleepEvent = Optional.of((Event)new FallingAsleepEvent(fallAsleepSegment.getStartTimestamp(),
                fallAsleepSegment.getEndTimestamp(),
                fallAsleepSegment.getOffsetMillis()));

        final Optional<Event> wakeUpEvent = Optional.of((Event)new WakeupEvent(wakeUpSegment.getStartTimestamp(),
                wakeUpSegment.getEndTimestamp(),
                wakeUpSegment.getOffsetMillis()));

        final Optional<Event> outOfBedEvent = Optional.of((Event)new OutOfBedEvent(outOfBedSegment.getStartTimestamp(),
                outOfBedSegment.getEndTimestamp(),
                outOfBedSegment.getOffsetMillis()));

        final SleepEvents<Optional<Event>> events = SleepEvents.create(inBedEvent, fallAsleepEvent, wakeUpEvent, outOfBedEvent);
        this.sleepEvents = events;


        for(final Segment segment:awakes){
            if(segment.getStartTimestamp() < fallAsleepSegment.getEndTimestamp()){
                continue;
            }

            if(segment.getEndTimestamp() > wakeUpSegment.getStartTimestamp()){
                continue;
            }

            if(segment.getDuration() <= DateTimeConstants.MILLIS_PER_MINUTE){
                continue;
            }
            
            this.extraEvents.add(new WakeupEvent(segment.getStartTimestamp(),
                    segment.getStartTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                    segment.getOffsetMillis()));
            this.extraEvents.add(new FallingAsleepEvent(segment.getEndTimestamp(),
                    segment.getEndTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                    segment.getOffsetMillis()));
        }
    }
}
