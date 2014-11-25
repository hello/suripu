package com.hello.suripu.core.models;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by pangwu on 11/24/14.
 */
public class NullEvent extends Event {

    private int sleepDepth = 0;

    public NullEvent(final long startTimestamp, final long endTimestamp, final int timezoneOffset, final int sleepDepth) {
        super(Type.NONE, startTimestamp, endTimestamp, timezoneOffset);
        this.sleepDepth = sleepDepth;
    }

    @Override
    public void setDescription(final String message) {
        throw new NotImplementedException();
    }

    @Override
    public String getDescription() {
        return "";
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
