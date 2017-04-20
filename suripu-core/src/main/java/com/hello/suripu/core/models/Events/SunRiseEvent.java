package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;

/**
 * Created by pangwu on 11/22/14.
 */
public class SunRiseEvent extends Event {
    private int sleepDepth = 0;
    private SleepSegment.SoundInfo soundInfo;

    public SunRiseEvent(final SleepPeriod.Period sleepPeriod, final long startTimestamp, final long endTimestamp, final int offsetMillis, final int sleepDepth, final SleepSegment.SoundInfo soundInfo){
        super(Type.SUNRISE, sleepPeriod, startTimestamp, endTimestamp, offsetMillis);
        this.sleepDepth = sleepDepth;
        this.soundInfo = soundInfo;
    }

    @Override
    public String getDescription(){ return English.SUNRISE_MESSAGE; }

    /*
    * Let's don't put the setters in the base class, because not all events has those properties.
     */
    public void setSoundInfo(final SleepSegment.SoundInfo sunRiseSound) {
        this.soundInfo = sunRiseSound;
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
