package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;

/**
 * Created by pangwu on 11/22/14.
 */
public class SunSetEvent extends Event {
    private int sleepDepth = 0;

    public SunSetEvent(final SleepPeriod.Period sleepPeriod, final long startTimestamp, final long endTimestamp, final int offsetMillis, final int sleepDepth){
        super(Event.Type.SUNSET, sleepPeriod, startTimestamp, endTimestamp, offsetMillis);
        this.sleepDepth = sleepDepth;
    }

    @Override
    public String getDescription(){
        return English.SUNSET_MESSAGE;
    }

    @Override
    public SleepSegment.SoundInfo getSoundInfo() {
        return null;
    }

    @Override
    public int getSleepDepth() {
        return this.sleepDepth;
    }
}
