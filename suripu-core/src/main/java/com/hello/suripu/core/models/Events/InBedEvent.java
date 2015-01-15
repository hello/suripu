package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;

/**
 * Created by pangwu on 11/24/14.
 */
public class InBedEvent extends Event {

    private String message;

    public InBedEvent(long startTimestamp, long endTimestamp, int timezoneOffset) {
        super(Type.SLEEP, startTimestamp, endTimestamp, timezoneOffset);
        this.message = "You went to bed";
    }

    public InBedEvent(long startTimestamp, long endTimestamp, int timezoneOffset, final String message) {
        super(Type.IN_BED, startTimestamp, endTimestamp, timezoneOffset);
        this.message = message;
    }

    @Override
    public String getDescription() {
        return this.message;
    }

    @Override
    public SleepSegment.SoundInfo getSoundInfo() {
        return null;
    }

    @Override
    public int getSleepDepth() {
        return 100;
    }
}
