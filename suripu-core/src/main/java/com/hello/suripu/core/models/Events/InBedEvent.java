package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;

/**
 * Created by pangwu on 11/24/14.
 */
public class InBedEvent extends Event {

    private String message;

    public InBedEvent(SleepPeriod.Period sleepPeriod, long startTimestamp, long endTimestamp, int timezoneOffset) {
        super(Type.IN_BED, sleepPeriod, startTimestamp, endTimestamp, timezoneOffset);
        this.message = English.IN_BED_MESSAGE;
    }

    public InBedEvent(final SleepPeriod.Period sleepPeriod, long startTimestamp, long endTimestamp, int timezoneOffset, final String message) {
        super(Type.IN_BED, sleepPeriod, startTimestamp, endTimestamp, timezoneOffset);
        this.message = message;
    }

    public static InBedEvent createForPeriod(long startTimestamp, long endTimestamp, int timezoneOffset, SleepPeriod.Period period){
        return new InBedEvent(period, startTimestamp, endTimestamp, timezoneOffset);
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
        return 0;
    }
}
