package com.hello.suripu.core.models;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by pangwu on 11/22/14.
 */
public class SunSetEvent extends  Event {
    private int sleepDepth = 0;

    public SunSetEvent(final long startTimestamp, final long endTimestamp, final int offsetMillis, final int sleepDepth){
        super(Event.Type.SUNSET, startTimestamp, endTimestamp, offsetMillis);
        this.setSleepDepth(sleepDepth);
    }

    @Override
    public String getDescription(){
        return "Sun set";
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
    public void setSleepDepth(int sleepDepth) {
        this.sleepDepth = sleepDepth;
    }

    @Override
    public int getSleepDepth() {
        return this.sleepDepth;
    }
}
