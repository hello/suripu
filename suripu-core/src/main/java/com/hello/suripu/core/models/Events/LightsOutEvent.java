package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.message.English;

public class LightsOutEvent extends Event {
    private String description = English.LIGHTS_OUT_MESSAGE;

    public LightsOutEvent(final long startTimestamp, final long endTimestamp, final int offsetMillis) {
        super(Type.LIGHTS_OUT, startTimestamp, endTimestamp, offsetMillis);
    }

    @Override
    public String getDescription(){
        return this.description;
    }

    public void setDescription(final String description) {
        this.description = description;
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
