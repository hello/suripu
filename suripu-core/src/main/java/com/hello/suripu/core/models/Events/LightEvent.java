package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;

public class LightEvent extends Event {
    private String description = English.LIGHT_MESSAGE;

    public LightEvent(final SleepPeriod.Period sleepPeriod, final long startTimestamp, final long endTimestamp, final int offsetMillis, final String description) {
        super(Type.LIGHT, sleepPeriod, startTimestamp, endTimestamp, offsetMillis);
        setDescription(description);
    }

    public static LightEvent createForPeriod(final long startTimestamp, final long endTimestamp, final int offsetMillis, final String description, final SleepPeriod.Period period){
        return new LightEvent(period, startTimestamp, endTimestamp, offsetMillis, description);
    }

    @Override
    public String getDescription(){
        return this.description;
    }
    private void setDescription(final String description) {
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
