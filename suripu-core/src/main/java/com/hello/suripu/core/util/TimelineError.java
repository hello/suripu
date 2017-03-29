package com.hello.suripu.core.util;

/**
 * Created by pangwu on 6/4/15.
 */
public enum TimelineError {
    NO_ERROR(0),
    TIMESPAN_TOO_SHORT(1),
    LOW_AMP_DATA(2),
    NOT_ENOUGH_DATA(3),
    NO_DATA(4),
    INVALID_SLEEP_SCORE(5),
    MISSING_KEY_EVENTS(6),
    NOT_ENOUGH_HOURS_OF_SLEEP(7),
    DATA_GAP_TOO_LARGE(8),
    EVENTS_OUT_OF_ORDER(9),
    PARTNER_FILTER_REJECTED_DATA(10),
    UNEXEPECTED(10);


    private final Integer value;

    TimelineError(final Integer value) {
        this.value = value;
    }
    public int getValue() {
        return this.value;
    }

    public static TimelineError fromInteger(int value) {
        for (final TimelineError timelineError : TimelineError.values()) {
            if (timelineError.getValue() == value) {
                return timelineError;
            }
        }
        return NO_ERROR;
    }

}
