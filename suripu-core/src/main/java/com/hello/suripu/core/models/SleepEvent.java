package com.hello.suripu.core.models;

/**
 * Created by pangwu on 11/24/14.
 */
public class SleepEvent extends Event {
    public SleepEvent(long startTimestamp, long endTimestamp, int timezoneOffset) {
        super(Type.SLEEP, startTimestamp, endTimestamp, timezoneOffset);
    }

    @Override
    public String getDescription() {
        return "You fell asleep";
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
