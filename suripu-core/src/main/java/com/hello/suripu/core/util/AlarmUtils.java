package com.hello.suripu.core.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

public class AlarmUtils {

    public final static Integer CLOCK_SKEW_TOLERATED_MILLIS = 10000;

    public static boolean isWithinReasonableBounds(final DateTime reference, final long clientTime) {
        final Long now = reference.toDateTime(DateTimeZone.UTC).getMillis();
        final Long timeDiff =  now - clientTime;
        return Math.abs(timeDiff) <= DateTimeConstants.MILLIS_PER_MINUTE;
    }

    public static boolean isWithinReasonableBounds(final DateTime reference, final long clientTime, final Integer skew) {
        final Long now = reference.toDateTime(DateTimeZone.UTC).getMillis();
        final Long timeDiff =  now - clientTime;
        return Math.abs(timeDiff) <= DateTimeConstants.MILLIS_PER_MINUTE + skew;
    }

    public static boolean isWithinReasonableBoundsApproximately(final DateTime reference, final long clientTime) {
        final Long now = reference.toDateTime(DateTimeZone.UTC).getMillis();
        final Long timeDiff =  now - clientTime;
        return Math.abs(timeDiff) <= DateTimeConstants.MILLIS_PER_MINUTE + CLOCK_SKEW_TOLERATED_MILLIS;
    }
}
