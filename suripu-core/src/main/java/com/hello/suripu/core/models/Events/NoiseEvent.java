package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;

public class NoiseEvent extends Event {
    private String description = English.NOISE_MESSAGE;
    private int sleepDepth = 0;
    public NoiseEvent(final long startTimestamp, final long endTimestamp, final int offsetMillis, final int sleepDepth) {
        super(Type.NOISE, startTimestamp, endTimestamp, offsetMillis);
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
