package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;

/**
 * Created by pangwu on 11/24/14.
 */
public class SleepMotionEvent extends Event {
    private int sleepDepth = 0;

    public SleepMotionEvent(final SleepPeriod.Period sleepPeriod,
                            final long startTimestamp,
                            final long endTimestamp,
                            final int offsetMillis,
                            final int sleepDepth){
        super(Event.Type.SLEEP_MOTION, sleepPeriod, startTimestamp, endTimestamp, offsetMillis);
        this.sleepDepth = sleepDepth;
    }

    @Override
    public String getDescription() {
        return English.SLEEP_MOTION_MESSAGE;
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
