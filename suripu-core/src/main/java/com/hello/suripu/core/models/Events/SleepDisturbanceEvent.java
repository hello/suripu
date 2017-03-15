package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;

/**
 * Created by jarredheinrich on 1/19/17.
 */
public class SleepDisturbanceEvent extends Event {
    private String description = English.SLEEP_DISTURBANCE_MESSAGE;
    private int sleepDepth = 0;
    public SleepDisturbanceEvent (final long startTimestamp, final long endTimestamp, final int offsetMillis, final int sleepDepth) {
        super(Type.SLEEP_DISTURBANCE, startTimestamp, endTimestamp, offsetMillis);
        this.sleepDepth = sleepDepth;
    }

    @Override
    public String getDescription(){
        return this.description;
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
