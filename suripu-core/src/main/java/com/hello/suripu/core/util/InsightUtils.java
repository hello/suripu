package com.hello.suripu.core.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
 * Created by jyfan on 5/18/16.
 */
public class InsightUtils {
    public static final Integer DAY_MINUTES = 24 * 60;

    public static String timeConvert(final int minuteOfDay) {
        final int hours = ((minuteOfDay + DAY_MINUTES) % DAY_MINUTES) / 60;
        final int minutes = (minuteOfDay + DAY_MINUTES) % 60;

        final String timeString = DateTimeFormat.forPattern("h:mm aa").print(new DateTime(0).withHourOfDay(hours).withMinuteOfHour(minutes));
        return timeString;
    }
}
