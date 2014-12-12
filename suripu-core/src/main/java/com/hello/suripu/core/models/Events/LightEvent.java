package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;

public class LightEvent extends Event {
    private String description = "Light";

    public LightEvent(final long startTimestamp, final long endTimestamp, final int offsetMillis, final String description) {
        super(Type.LIGHT, startTimestamp, endTimestamp, offsetMillis);
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
