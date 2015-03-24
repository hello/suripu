package com.hello.suripu.algorithm;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

/**
 * Created by pangwu on 3/23/15.
 */
public class DateTimeTestUtils {
    public static DateTime stringToLocalUTC(final String stringDate){
        final DateTime local =  DateTime.parse(stringDate, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
        return new DateTime(local.getYear(), local.getMonthOfYear(), local.getDayOfMonth(),
                local.getHourOfDay(), local.getMinuteOfHour(), local.getSecondOfMinute(), DateTimeZone.UTC);
    }

    public static DateTime millisToLocalUTC(final long millis, final int offsetMillis){
        return new DateTime(millis, DateTimeZone.forOffsetMillis(offsetMillis)).plusMillis(offsetMillis).withZone(DateTimeZone.UTC);
    }
}
