package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;

public class SleepingEvent extends Event {

    private Integer sleepDepth = 0;

    public SleepingEvent(SleepPeriod.Period sleepPeriod, long startTimestamp, long endTimestamp, int timezoneOffset, final Integer sleepDepth) {
        super(Type.SLEEPING, sleepPeriod, startTimestamp, endTimestamp, timezoneOffset);
        this.sleepDepth = sleepDepth;
    }

    public static SleepingEvent createForPeriod(long startTimestamp, long endTimestamp, int timezoneOffset, final Integer sleepDepth, final SleepPeriod.Period period){
        return new SleepingEvent(period, startTimestamp, endTimestamp, timezoneOffset, sleepDepth);
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public SleepSegment.SoundInfo getSoundInfo() {
        return null;
    }

    @Override
    public int getSleepDepth() {
        return sleepDepth;
    }
}
