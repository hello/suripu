package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class SunData {

    final private static String data = "2014-09-01,06:40,19:39\n" +
            "2014-09-02,06:41,19:37\n" +
            "2014-09-03,06:42,19:36\n" +
            "2014-09-04,06:42,19:34\n" +
            "2014-09-05,06:43,19:33\n" +
            "2014-09-06,06:44,19:31\n" +
            "2014-09-07,06:45,19:30\n" +
            "2014-09-08,06:46,19:28\n" +
            "2014-09-09,06:47,19:27\n" +
            "2014-09-10,06:47,19:25\n" +
            "2014-09-11,06:48,19:24\n" +
            "2014-09-12,06:49,19:22\n" +
            "2014-09-13,06:50,19:20\n" +
            "2014-09-14,06:51,19:19\n" +
            "2014-09-15,06:52,19:17\n" +
            "2014-09-16,06:53,19:16\n" +
            "2014-09-17,06:53,19:14\n" +
            "2014-09-18,06:54,19:13\n" +
            "2014-09-19,06:55,19:11\n" +
            "2014-09-20,06:56,19:10\n" +
            "2014-09-21,06:57,19:08\n" +
            "2014-09-22,06:58,19:06\n" +
            "2014-09-23,06:58,19:05\n" +
            "2014-09-24,06:59,19:03\n" +
            "2014-09-25,07:00,19:02\n" +
            "2014-09-26,07:01,19:00\n" +
            "2014-09-27,07:02,18:59\n" +
            "2014-09-28,07:03,18:57\n" +
            "2014-09-29,07:04,18:56\n" +
            "2014-09-30,07:05,18:54\n" +
            "2014-10-01,07:05,18:53\n" +
            "2014-10-02,07:06,18:51\n" +
            "2014-10-03,07:07,18:50\n" +
            "2014-10-04,07:08,18:48\n" +
            "2014-10-05,07:09,18:47\n" +
            "2014-10-06,07:10,18:45\n" +
            "2014-10-07,07:11,18:44\n" +
            "2014-10-08,07:12,18:42\n" +
            "2014-10-09,07:13,18:41\n" +
            "2014-10-10,07:14,18:39\n" +
            "2014-10-11,07:14,18:38\n" +
            "2014-10-12,07:15,18:36\n" +
            "2014-10-13,07:16,18:35\n" +
            "2014-10-14,07:17,18:34\n" +
            "2014-10-15,07:18,18:32\n" +
            "2014-10-16,07:19,18:31\n" +
            "2014-10-17,07:20,18:29\n" +
            "2014-10-18,07:21,18:28\n" +
            "2014-10-19,07:22,18:27\n" +
            "2014-10-20,07:23,18:25\n" +
            "2014-10-21,07:24,18:24\n" +
            "2014-10-22,07:25,18:23\n" +
            "2014-10-23,07:26,18:22\n" +
            "2014-10-24,07:27,18:20\n" +
            "2014-10-25,07:28,18:19\n" +
            "2014-10-26,07:29,18:18\n" +
            "2014-10-27,07:30,18:17\n" +
            "2014-10-28,07:31,18:15\n" +
            "2014-10-29,07:32,18:14\n" +
            "2014-10-30,07:33,18:13\n" +
            "2014-10-31,07:34,18:12\n" +
            "2014-11-01,07:35,18:11\n" +
            "2014-11-02,06:36,17:10\n" +
            "2014-11-03,06:37,17:09\n" +
            "2014-11-04,06:38,17:08\n" +
            "2014-11-05,06:39,17:07\n" +
            "2014-11-06,06:40,17:06\n" +
            "2014-11-07,06:42,17:05\n" +
            "2014-11-08,06:43,17:04\n" +
            "2014-11-09,06:44,17:03\n" +
            "2014-11-10,06:45,17:02\n" +
            "2014-11-11,06:46,17:01\n" +
            "2014-11-12,06:47,17:01\n" +
            "2014-11-13,06:48,17:00\n" +
            "2014-11-14,06:49,16:59\n" +
            "2014-11-15,06:50,16:58\n" +
            "2014-11-16,06:51,16:58\n" +
            "2014-11-17,06:52,16:57\n" +
            "2014-11-18,06:53,16:56\n" +
            "2014-11-19,06:54,16:56\n" +
            "2014-11-20,06:55,16:55\n" +
            "2014-11-21,06:56,16:55\n" +
            "2014-11-22,06:57,16:54\n" +
            "2014-11-23,06:58,16:54\n" +
            "2014-11-24,06:59,16:53\n" +
            "2014-11-25,07:01,16:53\n" +
            "2014-11-26,07:02,16:52\n" +
            "2014-11-27,07:03,16:52\n" +
            "2014-11-28,07:04,16:52\n" +
            "2014-11-29,07:04,16:52\n" +
            "2014-11-30,07:05,16:51\n" +
            "2014-12-01,07:06,16:51\n" +
            "2014-12-02,07:07,16:51\n" +
            "2014-12-03,07:08,16:51\n" +
            "2014-12-04,07:09,16:51\n" +
            "2014-12-05,07:10,16:51\n" +
            "2014-12-06,07:11,16:51\n" +
            "2014-12-07,07:12,16:51\n" +
            "2014-12-08,07:13,16:51\n" +
            "2014-12-09,07:13,16:51\n" +
            "2014-12-10,07:14,16:51\n" +
            "2014-12-11,07:15,16:51\n" +
            "2014-12-12,07:16,16:51\n" +
            "2014-12-13,07:17,16:52\n" +
            "2014-12-14,07:17,16:52\n" +
            "2014-12-15,07:18,16:52\n" +
            "2014-12-16,07:19,16:52\n" +
            "2014-12-17,07:19,16:53\n" +
            "2014-12-18,07:20,16:53\n" +
            "2014-12-19,07:20,16:54\n" +
            "2014-12-20,07:21,16:54\n" +
            "2014-12-21,07:21,16:54\n" +
            "2014-12-22,07:22,16:55\n" +
            "2014-12-23,07:22,16:56\n" +
            "2014-12-24,07:23,16:56\n" +
            "2014-12-25,07:23,16:57\n" +
            "2014-12-26,07:24,16:57\n" +
            "2014-12-27,07:24,16:58\n" +
            "2014-12-28,07:24,16:59\n" +
            "2014-12-29,07:25,16:59\n" +
            "2014-12-30,07:25,17:00\n" +
            "2014-12-31,07:25,17:01\n";

    private final ImmutableMap<String, List<String>> lookup;

    public SunData() {
        final Map<String, List<String>> temp = new HashMap<>();
        final String[] rows = data.split("\n");
        for(String row : rows) {
            final String[] parts = row.split(",");
            temp.put(parts[0], Lists.newArrayList(parts[1], parts[2]));
        }

        lookup = ImmutableMap.copyOf(temp);

    }


    public Optional<DateTime> sunrise(final String dateString) {
        final List<String> data = lookup.get(dateString);
        if(data == null) {
            return Optional.absent();
        }

        final String sunrise = data.get(0);
        final DateTime dt = DateTime.parse(dateString + " " + sunrise, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm"));
        final DateTime dtTz = new DateTime(
                dt.getYear(), dt.getMonthOfYear(), dt.getDayOfMonth(),
                dt.getHourOfDay(), dt.getMinuteOfHour(), 0,
                DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/Los_Angeles"))
        );
        return Optional.of(dtTz);
    }

    public Optional<DateTime> sunset(final String dateString) {
        final List<String> data = lookup.get(dateString);
        if(data == null) {
            return Optional.absent();
        }

        final String sunset = data.get(1);
        final DateTime dt = DateTime.parse(dateString + " " + sunset, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm"));
        final DateTime dtTz = new DateTime(
                dt.getYear(), dt.getMonthOfYear(), dt.getDayOfMonth(),
                dt.getHourOfDay(), dt.getMinuteOfHour(), 0,
                DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/Los_Angeles"))
        );
        return Optional.of(dtTz);
    }


}
