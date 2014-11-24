package com.hello.suripu.core.util;

import com.hello.suripu.core.models.Event;
import org.joda.time.DateTimeConstants;

/**
 * Created by pangwu on 11/23/14.
 */
public class EventUtil {
    public static int getEventDurationInSecond(final Event event){
        if(event.endTimestamp < event.startTimestamp){
            // Concern: Usually this is too deep in the call stack and make it hard to debug.
            throw new IllegalArgumentException(String.format("Event %s, end time %d larger than start time %d",
                    event.getType(), event.startTimestamp, event.endTimestamp));
        }
        return (int)(event.endTimestamp - event.startTimestamp) / DateTimeConstants.MILLIS_PER_SECOND;
    }
}
