package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;

/**
 * Created by pangwu on 11/22/14.
 */
public class MotionEvent extends Event {
    private int sleepDepth = 0;

    public MotionEvent(final SleepPeriod.Period sleepPeriod,
                       final long startTimestamp,
                       final long endTimestamp,
                       final int offsetMillis,
                       final int sleepDepth){
        super(Type.MOTION, sleepPeriod, startTimestamp, endTimestamp, offsetMillis);
        this.sleepDepth = sleepDepth;
    }

    public static MotionEvent createForPeriod(final long startTimestamp,
                       final long endTimestamp,
                       final int offsetMillis,
                       final int sleepDepth,
                       final SleepPeriod.Period period){
       return  new MotionEvent(period, startTimestamp, endTimestamp, offsetMillis, sleepDepth);
    }

    @Override
    public String getDescription(){
        return English.MOTION_MESSAGE;
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
