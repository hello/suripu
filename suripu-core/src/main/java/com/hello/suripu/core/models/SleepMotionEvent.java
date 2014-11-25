package com.hello.suripu.core.models;

import javax.ws.rs.HEAD;

/**
 * Created by pangwu on 11/24/14.
 */
public class SleepMotionEvent extends Event {
    private int sleepDepth = 0;

    public SleepMotionEvent(final long startTimestamp,
                       final long endTimestamp,
                       final int offsetMillis,
                       final int sleepDepth){
        super(Event.Type.SLEEP_MOTION, startTimestamp, endTimestamp, offsetMillis);
        this.sleepDepth = sleepDepth;
    }

    @Override
    public String getDescription() {
        return "Toss and turns";
    }

    @Override
    public SleepSegment.SoundInfo getSoundInfo() {
        return null;
    }

    @Override
    public int getSleepDepth() {
        return this.sleepDepth;
    }
}
