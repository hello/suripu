package com.hello.suripu.core.models;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by pangwu on 11/22/14.
 */
public class SunRiseEvent extends Event {
    private int sleepDepth = 0;
    private SleepSegment.SoundInfo soundInfo;

    public SunRiseEvent(final long startTimestamp, final long endTimestamp, final int offsetMillis, final int sleepDepth, final SleepSegment.SoundInfo soundInfo){
        super(Type.SUNRISE, startTimestamp, endTimestamp, offsetMillis);
        this.setSleepDepth(sleepDepth);
    }

    @Override
    public void setDescription(String message) {
        throw new NotImplementedException();
    }

    @Override
    public String getDescription(){
        return "Sun rise";
    }

    @Override
    public void setSoundInfo(final SleepSegment.SoundInfo soundInfo) {
        this.soundInfo = soundInfo;
    }

    @Override
    public SleepSegment.SoundInfo getSoundInfo() {
        return this.soundInfo;
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
