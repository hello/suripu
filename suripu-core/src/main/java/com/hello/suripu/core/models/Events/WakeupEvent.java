package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;

/**
 * Created by pangwu on 11/24/14.
 */
public class WakeupEvent extends Event {
    private final String wakeupMessage;

    public WakeupEvent(SleepPeriod.Period sleepPeriod, long startTimestamp, long endTimestamp, int timezoneOffset) {
        super(Type.WAKE_UP, sleepPeriod, startTimestamp, endTimestamp, timezoneOffset);
        this.wakeupMessage = English.WAKE_UP_MESSAGE;
    }

    public WakeupEvent(SleepPeriod.Period sleepPeriod,long startTimestamp, long endTimestamp, int timezoneOffset, final String wakeupMessage) {
        super(Type.WAKE_UP, sleepPeriod, startTimestamp, endTimestamp, timezoneOffset);
        this.wakeupMessage = wakeupMessage;
    }

    public static WakeupEvent createForNight(long startTimestamp, long endTimestamp, int timezoneOffset) {
        return new WakeupEvent(SleepPeriod.Period.NIGHT, startTimestamp, endTimestamp, timezoneOffset);
    }

    @Override
    public String getDescription() {
        return this.wakeupMessage;
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
