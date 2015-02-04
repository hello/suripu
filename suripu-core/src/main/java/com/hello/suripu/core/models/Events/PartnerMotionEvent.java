package com.hello.suripu.core.models.Events;

import com.hello.suripu.core.translations.English;

/**
 * Created by pangwu on 11/22/14.
 */
public class PartnerMotionEvent extends SleepMotionEvent {
    public PartnerMotionEvent(long startTimestamp, long endTimestamp, int offsetMillis, int sleepDepth) {
        super(startTimestamp, endTimestamp, offsetMillis, sleepDepth);
        this.setType(Type.PARTNER_MOTION);
    }

    @Override
    public String getDescription(){
        return English.PARTNER_MOTION_MESSAGE;
    }
}
