package com.hello.suripu.core.models;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

/**
 * Created by pangwu on 11/22/14.
 */
public class ParterMotionEvent extends MotionEvent {
    public ParterMotionEvent(long startTimestamp, long endTimestamp, int offsetMillis, double amplitude, double maxAmplitude) {
        super(startTimestamp, endTimestamp, offsetMillis, amplitude, maxAmplitude);
        this.setType(Type.PARTNER_MOTION);
    }

    @Override
    public String getDescription(){
        return String.format("Your partner kicked you at %s",
                new DateTime(this.startTimestamp, DateTimeZone.forOffsetMillis(this.timezoneOffset))
                        .toString(DateTimeFormat.forPattern("HH:mm a")));
    }
}
