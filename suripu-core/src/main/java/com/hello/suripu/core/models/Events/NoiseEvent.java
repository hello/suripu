package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

public class NoiseEvent extends Event {
    private String description = English.NOISE_MESSAGE;

    public NoiseEvent(final long startTimestamp, final long endTimestamp, final int offsetMillis) {
        super(Type.NOISE, startTimestamp, endTimestamp, offsetMillis);
        final DateTime noiseDateTime = new DateTime(startTimestamp, DateTimeZone.UTC).plusMillis(offsetMillis);
        this.description += " " + noiseDateTime.toString(DateTimeFormat.forPattern("HH:mm"));
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
        return 0;
    }
}
