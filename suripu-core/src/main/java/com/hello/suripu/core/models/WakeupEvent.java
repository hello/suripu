package com.hello.suripu.core.models;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by pangwu on 11/24/14.
 */
public class WakeupEvent extends Event {
    public WakeupEvent(long startTimestamp, long endTimestamp, int timezoneOffset) {
        super(Type.WAKE_UP, startTimestamp, endTimestamp, timezoneOffset);
    }

    @Override
    public void setDescription(final String message) {
        throw new NotImplementedException();
    }

    @Override
    public String getDescription() {
        return "Woke up";
    }

    @Override
    public void setSoundInfo(final SleepSegment.SoundInfo soundInfo) {
        throw new NotImplementedException();
    }

    @Override
    public SleepSegment.SoundInfo getSoundInfo() {
        return null;
    }

    @Override
    public void setSleepDepth(final int sleepDepth) {
        throw new NotImplementedException();
    }

    @Override
    public int getSleepDepth() {
        return 0;
    }
}
