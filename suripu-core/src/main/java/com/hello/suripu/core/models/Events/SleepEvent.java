package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;

/**
 * Created by pangwu on 11/24/14.
 */
public class SleepEvent extends Event {

    private String message;

    public SleepEvent(long startTimestamp, long endTimestamp, int timezoneOffset) {
        super(Type.SLEEP, startTimestamp, endTimestamp, timezoneOffset);
        this.message = "You fell asleep";
    }

    public SleepEvent(long startTimestamp, long endTimestamp, int timezoneOffset, final String message) {
        super(Type.SLEEP, startTimestamp, endTimestamp, timezoneOffset);
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
