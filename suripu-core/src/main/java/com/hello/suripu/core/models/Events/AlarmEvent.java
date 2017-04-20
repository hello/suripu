package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;

/**
 * Created by pangwu on 11/22/14.
 */
public class AlarmEvent extends Event {

    private String message = English.ALARM_DEFAULT_MESSAGE;

    public AlarmEvent(final SleepPeriod.Period sleepPeriod, final long startTimestamp, final long endTimestamp, final int offsetMillis){
        super(Type.ALARM, sleepPeriod, startTimestamp, endTimestamp, offsetMillis);
    }

    public AlarmEvent(final SleepPeriod.Period sleepPeriod, final long startTimestamp, final long endTimestamp, final int offsetMillis, final String message){
        super(Type.ALARM, sleepPeriod, startTimestamp, endTimestamp, offsetMillis);
        this.message = message;
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
