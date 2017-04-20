package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;

/**
 * Created by pangwu on 11/24/14.
 */
public class NullEvent extends Event {

    private int sleepDepth = 0;

    public NullEvent(final long startTimestamp, final long endTimestamp, final int timezoneOffset, final int sleepDepth) {
        super(Type.NONE, SleepPeriod.Period.NONE, startTimestamp, endTimestamp, timezoneOffset);
        this.sleepDepth = sleepDepth;
    }

    @Override
    public String getDescription() {
        return English.NULL_MESSAGE;
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
