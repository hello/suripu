package com.hello.suripu.core.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

/**
 * Created by pangwu on 5/30/14.
 */
public class DateTimeUtil {
    public static final String DYNAMO_DB_DATE_FORMAT = "yyyy-MM-dd";
    public static final long DAY_IN_MILLIS = 86400000L;


    public static final DateTime MORPHEUS_DAY_ONE = DateTime.parse("2014-04-08", DateTimeFormat.forPattern(DYNAMO_DB_DATE_FORMAT));

    public static String dateToYmdString(final DateTime date) {
        return DateTimeFormat.forPattern(DYNAMO_DB_DATE_FORMAT).print(date);
    }

    public static DateTime ymdStringToDateTime(final String dateString) {
        return DateTime.parse(dateString, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withTimeAtStartOfDay();

    }
}
