package com.hello.suripu.core.models.timeline.v2;

import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Event;

import java.util.List;

public enum ValidAction {
    ADJUST_TIME,
    VERIFY,
    INCORRECT,
    REMOVE;


    /**
     * Naive implementation of valid feedback actions
     * @param type
     * @return
     */
    public static List<ValidAction> from(Event.Type type) {
        switch (type) {
            case SLEEP:
            case IN_BED:
            case OUT_OF_BED:
            case WAKE_UP:
            case SLEEP_DISTURBANCE:
                return Lists.newArrayList(ADJUST_TIME, VERIFY, INCORRECT);
            case LIGHTS_OUT:
            case MOTION:
            case PARTNER_MOTION:
            case NOISE:
            case SNORING:
            case SLEEP_TALK:
                return Lists.newArrayList(VERIFY, INCORRECT);
            default:
                return Lists.newArrayList();
        }
    }
}
