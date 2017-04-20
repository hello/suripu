package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;

/**
 * Created by pangwu on 11/24/14.
 */
public class FallingAsleepEvent extends Event {

    private String message;

    public FallingAsleepEvent(final SleepPeriod.Period sleepPeriod, long startTimestamp, long endTimestamp, int timezoneOffset) {
        super(Type.SLEEP, sleepPeriod, startTimestamp, endTimestamp, timezoneOffset);
        this.message = English.FALL_ASLEEP_MESSAGE;
    }

    public FallingAsleepEvent(final SleepPeriod.Period sleepPeriod, long startTimestamp, long endTimestamp, int timezoneOffset, final String message) {
        super(Type.SLEEP, sleepPeriod, startTimestamp, endTimestamp, timezoneOffset);
        this.message = message;
    }

    public static FallingAsleepEvent createForPeriod(long startTimestamp, long endTimestamp, int timezoneOffset, final SleepPeriod.Period period){
        return new FallingAsleepEvent(period, startTimestamp,endTimestamp, timezoneOffset);
    }

    @Override
    public String getDescription() {
        return this.message;
    }

    @Override
    public SleepSegment.SoundInfo getSoundInfo() {
        return null;
    }

    @Override
    public int getSleepDepth() {
        return 100;
    }
}
