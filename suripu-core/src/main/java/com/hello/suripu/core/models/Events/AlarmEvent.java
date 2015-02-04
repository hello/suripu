package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

/**
 * Created by pangwu on 11/22/14.
 */
public class AlarmEvent extends Event {

    public AlarmEvent(final long startTimestamp, final long endTimestamp, final int offsetMillis){
        super(Type.ALARM, startTimestamp, endTimestamp, offsetMillis);
    }

    @Override
    public String getDescription(){
        final String time = new DateTime(getStartTimestamp(), DateTimeZone.UTC).plusMillis(getTimezoneOffset()).toString(DateTimeFormat.forPattern("HH:mm"));
        return String.format(English.ALARM_NORMAL_MESSAGE, time);
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
