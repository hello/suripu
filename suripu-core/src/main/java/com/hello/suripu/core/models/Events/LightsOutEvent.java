package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;

public class LightsOutEvent extends Event {
    private String description = English.LIGHTS_OUT_MESSAGE;

    public LightsOutEvent(final SleepPeriod.Period sleepPeriod, final long startTimestamp, final long endTimestamp, final int offsetMillis) {
        super(Type.LIGHTS_OUT, sleepPeriod, startTimestamp, endTimestamp, offsetMillis);
    }

    public static LightsOutEvent createForPeriod(final long startTimestamp, final long endTimestamp, final int offsetMillis, final SleepPeriod.Period period){
        return new LightsOutEvent(period, startTimestamp, endTimestamp, offsetMillis);
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
