package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;

public class SleepingEvent extends Event {

    private Integer sleepDepth = 0;

    public SleepingEvent(long startTimestamp, long endTimestamp, int timezoneOffset, final Integer sleepDepth) {
        super(Type.SLEEPING, startTimestamp, endTimestamp, timezoneOffset);
        this.sleepDepth = sleepDepth;
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public SleepSegment.SoundInfo getSoundInfo() {
        return null;
    }

    @Override
    public int getSleepDepth() {
        return sleepDepth;
    }
}
