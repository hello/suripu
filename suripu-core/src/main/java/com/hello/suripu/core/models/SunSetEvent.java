package com.hello.suripu.core.models;

import org.joda.time.DateTimeConstants;

/**
 * Created by pangwu on 11/22/14.
 */
public class SunSetEvent extends  Event {
    public SunSetEvent(final long startTimestamp, final int offsetMillis){
        super(Event.Type.SUNSET, startTimestamp, startTimestamp + DateTimeConstants.MILLIS_PER_MINUTE, offsetMillis);
    }

    @Override
    public String getDescription(){
        return "Sun set";
    }
}
