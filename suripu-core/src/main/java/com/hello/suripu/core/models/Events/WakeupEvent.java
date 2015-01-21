package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.message.English;

/**
 * Created by pangwu on 11/24/14.
 */
public class WakeupEvent extends Event {
    public WakeupEvent(long startTimestamp, long endTimestamp, int timezoneOffset) {
        super(Type.WAKE_UP, startTimestamp, endTimestamp, timezoneOffset);
    }

    @Override
    public String getDescription() {
        return English.WAKE_UP_MESSAGE;
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
