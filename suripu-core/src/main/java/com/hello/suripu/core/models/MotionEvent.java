package com.hello.suripu.core.models;

import javax.ws.rs.HEAD;

/**
 * Created by pangwu on 11/22/14.
 */
public class MotionEvent extends Event {
    private int sleepDepth = 0;

    public MotionEvent(final long startTimestamp,
                       final long endTimestamp,
                       final int offsetMillis,
                       final int sleepDepth){
        super(Type.MOTION, startTimestamp, endTimestamp, offsetMillis);
        this.sleepDepth = sleepDepth;
    }

    @Override
    public String getDescription(){
        return "Motion detected";
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
