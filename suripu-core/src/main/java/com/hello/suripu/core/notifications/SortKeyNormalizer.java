package com.hello.suripu.core.notifications;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class SortKeyNormalizer {


    static DateTime normalize(PushNotificationEvent pushNotificationEvent) {
        switch(pushNotificationEvent.periodicity) {
            case WEEKLY:
                return byWeek(pushNotificationEvent.timestamp, pushNotificationEvent.timeZone);
            case DAILY:
                return byDay(pushNotificationEvent.timestamp, pushNotificationEvent.timeZone);
        }

        return pushNotificationEvent.timestamp;
    }

    static DateTime byDay(final DateTime dateTime, final DateTimeZone dateTimeZone) {
        return new DateTime(dateTime, dateTimeZone).withTimeAtStartOfDay();
    }

    static DateTime byWeek(final DateTime dateTime, final DateTimeZone dateTimeZone) {
        DateTime local = new DateTime(dateTime, dateTimeZone);

        DateTime onTheFirstDayOfTheWeek = dateTime.withWeekyear(dateTime.getWeekyear()).withWeekOfWeekyear(local.getWeekOfWeekyear()).withDayOfWeek(1).withTimeAtStartOfDay();
        return onTheFirstDayOfTheWeek;
    }
}
