package com.hello.suripu.core.util;

/**
 * Created by jyfan on 5/18/16.
 */
public class InsightUtils {
    public static final Integer DAY_MINUTES = 24 * 60;

    public static String timeConvert(final int minuteOfDay) {
        final int hours = (minuteOfDay % DAY_MINUTES) / 60;
        final int minutes = minuteOfDay % 60;
        final String meridiem = getMeridiem(hours);

        if (hours == 0) {
            return String.format("12:%02d %s", minutes, meridiem);
        }
        if (hours > 12) {
            final int civilianHour = hours - 12;
            return String.format("%d:%02d %s", civilianHour, minutes, meridiem);
        }
        return String.format("%d:%02d %s", hours, minutes, meridiem);
    }

    public static String getMeridiem(final int hour) {
        if (hour < 12) {
            return "AM";
        }
        return "PM";
    }
}
