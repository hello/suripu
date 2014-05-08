package com.hello.suripu.service;

import com.hello.suripu.core.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.LinkedList;

/**
 * Created by pangwu on 5/8/14.
 */
public class Util {
    public static double getAverageSVM(LinkedList<TrackerMotion> buffer){
        double average = 0.0;
        for(TrackerMotion datum:buffer){
            average += datum.val;
        }

        return average / buffer.size();
    }

    public static DateTime roundTimestampToMinuteUTC(long timestamp){
        DateTime dateTimeUTC = new DateTime(timestamp, DateTimeZone.UTC);
        DateTime roundedDateTimeUTC = new DateTime(
                dateTimeUTC.getYear(),
                dateTimeUTC.getMonthOfYear(),
                dateTimeUTC.getDayOfMonth(),
                dateTimeUTC.getHourOfDay(),
                dateTimeUTC.getMinuteOfHour(),
                DateTimeZone.UTC
        );

        return roundedDateTimeUTC;
    }
}
