package com.hello.suripu.core.models;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by pangwu on 11/24/14.
 */
public class SleepEvent extends Event {
    public SleepEvent(long startTimestamp, long endTimestamp, int timezoneOffset) {
        super(Type.SLEEP, startTimestamp, endTimestamp, timezoneOffset);
    }

    @Override
    public void setDescription(String message) {
        throw new NotImplementedException();
    }

    @Override
    public String getDescription() {
        return "Fell asleep";
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
        throw new NotImplementedException();
    }

    @Override
    public int getSleepDepth() {
        return 100;
    }
}
