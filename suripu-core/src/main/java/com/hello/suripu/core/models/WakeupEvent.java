package com.hello.suripu.core.models;

/**
 * Created by pangwu on 11/24/14.
 */
public class WakeupEvent extends Event {
    public WakeupEvent(long startTimestamp, long endTimestamp, int timezoneOffset) {
        super(Type.WAKE_UP, startTimestamp, endTimestamp, timezoneOffset);
    }

    @Override
    public String getDescription() {
        return "Woke up";
    }

    @Override
    public SleepSegment.SoundInfo getSoundInfo() {
        return null;
    }

    @Override
    public int getSleepDepth() {
        return 0;
    }
}
