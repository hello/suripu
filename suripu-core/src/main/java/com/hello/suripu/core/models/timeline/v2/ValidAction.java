package com.hello.suripu.core.models.timeline.v2;

import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Event;

import java.util.List;

public enum ValidAction {
    ADJUST_TIME,
    VERIFY,
    REMOVE;


    /**
     * Naive implementation of valid feedback actions
     * @param type
     * @return
     */
    public static List<ValidAction> from(Event.Type type) {
        switch (type) {
            case SLEEPING:
            case IN_BED:
            case OUT_OF_BED:
            case WAKE_UP:
                return Lists.newArrayList(ADJUST_TIME, VERIFY, REMOVE);
            default:
                return Lists.newArrayList();
        }
    }
}
