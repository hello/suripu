package com.hello.suripu.core.models;

import org.joda.time.DateTimeConstants;

/**
 * Created by pangwu on 11/22/14.
 */
public class SunRiseEvent extends Event {
    public SunRiseEvent(final long startTimestamp, final int offsetMillis){
        super(Type.SUNRISE, startTimestamp, startTimestamp + DateTimeConstants.MILLIS_PER_MINUTE, offsetMillis);
    }

    @Override
    public String getDescription(){
        return "Sun rise";
    }
}
