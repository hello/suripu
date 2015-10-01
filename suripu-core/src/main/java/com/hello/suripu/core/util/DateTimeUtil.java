package com.hello.suripu.core.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;

/**
 * Created by pangwu on 5/30/14.
 */
public class DateTimeUtil {
    public static final String DYNAMO_DB_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DYNAMO_DB_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final int DAY_STARTS_AT_HOUR = 20;
    public static final int DAY_ENDS_AT_HOUR = 12;

    public static final DateTime MORPHEUS_DAY_ONE = DateTime.parse("2014-04-08", DateTimeFormat.forPattern(DYNAMO_DB_DATE_FORMAT));

    public static String dateToYmdString(final DateTime date) {
        return DateTimeFormat.forPattern(DYNAMO_DB_DATE_FORMAT).print(date);
    }

    public static DateTime ymdStringToDateTime(final String dateString) {
        return DateTime.parse(dateString, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withTimeAtStartOfDay();

    }

    public static int getDateDiffFromNowInDays(final DateTime datetime) {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Duration duration = new Duration(datetime, now);
        return duration.toStandardDays().getDays();
    }

    public static DateTime getTargetDateLocalUTCFromLocalTime(final DateTime localTime){
        if(localTime.getHourOfDay() >= DAY_STARTS_AT_HOUR){
            return localTime.plusMillis(localTime.getZone().getOffset(localTime)).withZone(DateTimeZone.UTC).withTimeAtStartOfDay();
        }

        return localTime.plusMillis(localTime.getZone().getOffset(localTime)).withZone(DateTimeZone.UTC).withTimeAtStartOfDay().minusDays(1);
    }

    public static DateTime possiblySanitizeSampleTime(final DateTime referenceTime, final DateTime sampleTime, final Integer clockSkewInHours) {

        // This is intended to only check for the pathological case of Sense thinking we're 5 months ahead.
        // It is expected that whoever is calling this method will still apply the normal clockSkew check
        // Outage of Sept 30th.
        final DateTime copySampleTime = new DateTime(sampleTime);
        final Integer diffInMinutes = Minutes.minutesBetween(referenceTime, copySampleTime.minusMonths(5)).getMinutes();
        if(Math.abs(diffInMinutes) < clockSkewInHours * 60) {
            return sampleTime.minusMonths(5).withMinuteOfHour(sampleTime.getMinuteOfHour()).withHourOfDay(sampleTime.getHourOfDay());
        }

        // if this is not the exact case above, we return the sample datetime untouched.
        return sampleTime;
    }

}
