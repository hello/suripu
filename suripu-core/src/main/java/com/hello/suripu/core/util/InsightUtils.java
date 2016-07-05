package com.hello.suripu.core.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormatter;

/**
 * Created by jyfan on 5/18/16.
 */
public class InsightUtils {

    public static String timeConvert(final int minuteOfDay, final DateTimeFormatter timeFormat) {
        final int hours = ((minuteOfDay + DateTimeConstants.MINUTES_PER_DAY) % DateTimeConstants.MINUTES_PER_DAY) / 60;
        final int minutes = (minuteOfDay + DateTimeConstants.MINUTES_PER_DAY) % 60;

        final String timeString = timeFormat.print(new DateTime(0).withHourOfDay(hours).withMinuteOfHour(minutes));

        return timeString;
    }

    public static String timeConvertRound(final int minuteOfDay, final DateTimeFormatter timeFormat) {
        final int minutes = (minuteOfDay + DateTimeConstants.MINUTES_PER_DAY) % 60;

        final int minutesRound;
        final int hourAdd;
        if (minutes < 15) {
            minutesRound = 0;
            hourAdd = 0;
        } else if (minutes < 45) {
            minutesRound = 30;
            hourAdd = 0;
        } else {
            minutesRound = 0;
            hourAdd = 1;
        }

        final int hours = ((minuteOfDay + DateTimeConstants.MINUTES_PER_DAY) % DateTimeConstants.MINUTES_PER_DAY) / 60;
        final int hoursRound = hours + hourAdd;

        final String timeString = timeFormat.print(new DateTime(0).withHourOfDay(hoursRound).withMinuteOfHour(minutesRound));
        return timeString;
    }
}
