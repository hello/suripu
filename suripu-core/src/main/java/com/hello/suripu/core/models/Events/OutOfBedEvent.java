package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;

/**
 * Created by pangwu on 11/24/14.
 */
public class OutOfBedEvent extends Event {
    public OutOfBedEvent(long startTimestamp, long endTimestamp, int timezoneOffset) {
        super(Type.OUT_OF_BED, startTimestamp, endTimestamp, timezoneOffset);
    }

    @Override
    public String getDescription() {
        return English.OUT_OF_BED_MESSAGE;
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
