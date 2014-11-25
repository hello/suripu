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
        this.sleepDepth = sleepDepth;
        this.soundInfo = soundInfo;
    }

    @Override
    public String getDescription(){
        return "Sun rise";
    }

    /*
    * Let's don't put the setters in the base class, because not all events has those properties.
     */
    public void setSoundInfo(final SleepSegment.SoundInfo sunRiseSound) {
        this.soundInfo = soundInfo;
    }

    @Override
    public SleepSegment.SoundInfo getSoundInfo() {
        return this.soundInfo;
    }

    @Override
    public int getSleepDepth() {
        return this.sleepDepth;
    }
}
