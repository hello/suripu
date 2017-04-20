package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by pangwu on 11/24/14.
 */
public class WakeupEvent extends Event {
    private final String wakeupMessage;

    public WakeupEvent(SleepPeriod.Period sleepPeriod, long startTimestamp, long endTimestamp, int timezoneOffset) {
        super(Type.WAKE_UP, sleepPeriod, startTimestamp, endTimestamp, timezoneOffset);
        this.wakeupMessage = getWakeUpMessage(startTimestamp, timezoneOffset);
    }

    public WakeupEvent(SleepPeriod.Period sleepPeriod,long startTimestamp, long endTimestamp, int timezoneOffset, final String wakeupMessage) {
        super(Type.WAKE_UP, sleepPeriod, startTimestamp, endTimestamp, timezoneOffset);
        this.wakeupMessage = wakeupMessage;
    }

    public static WakeupEvent createForPeriod(long startTimestamp, long endTimestamp, int timezoneOffset, SleepPeriod.Period period) {
        return new WakeupEvent(period, startTimestamp, endTimestamp, timezoneOffset);
    }

    @Override
    public String getDescription() {
        return this.wakeupMessage;
    }

    @Override
    public SleepSegment.SoundInfo getSoundInfo() {
        return null;
    }

    @Override
    public int getSleepDepth() {
        return 0;
    }

    private String getWakeUpMessage(final Long startTimestamp, final int timezoneOffset){
        final int wakeUpHour =  new DateTime(startTimestamp + timezoneOffset, DateTimeZone.UTC).getHourOfDay();
        if (wakeUpHour < 12) {
            return English.MORNING_WAKE_UP_MESSAGE;
        }
        if (wakeUpHour < 18) {
            return English.AFTERNOON_WAKE_UP_MESSAGE;
        }
        return English.NIGHT_WAKE_UP_MESSAGE;
    }
}
