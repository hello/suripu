package com.hello.suripu.core.models;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
        setSleepDepth(sleepDepth);
    }

    @Override
    public String getDescription(){
        return "Motion detected";
    }

    @Override
    public void setSoundInfo(SleepSegment.SoundInfo soundInfo) {
        throw new NotImplementedException();
    }

    @Override
    public SleepSegment.SoundInfo getSoundInfo() {
        return null;
    }

    @Override
    public void setSleepDepth(final int sleepDepth) {
        this.sleepDepth = sleepDepth;
    }

    @Override
    public int getSleepDepth() {
        return this.sleepDepth;
    }

}
