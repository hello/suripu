package com.hello.suripu.core.util;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 12/18/14.
 */
public class TimelineUtilsTest {
    @Test
    public void testGetSleepPeriod(){
        final URL fixtureCSVFile = Resources.getResource("pang_motion_2014_12_02_raw.csv");
        final List<TrackerMotion> trackerMotions = new ArrayList<>();
        try {
            final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 1; i < lines.length; i++){
                final String[] columns = lines[i].split(",");
                trackerMotions.add(new TrackerMotion(0L, 0L, 0L, Long.valueOf(columns[0]), Integer.valueOf(columns[1]), Integer.valueOf(columns[2])));
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }

        final Segment sleepSegment = TimelineUtils.getSleepPeriod(new DateTime(2014, 12, 02, 0, 0, DateTimeZone.UTC),
                trackerMotions,
                Optional.of(new DateTime(1417598760000L, DateTimeZone.UTC)));

        // Out put from python script suripu_light_test.py:
        /*
        sleep at 2014-12-03 01:39:00, prob: 0.395362751303, amp: 5471
        wake up at 2014-12-03 07:09:00, prob: 0.0924221378596, amp: 518
        */

        final DateTime sleepTime = new DateTime(sleepSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.getOffsetMillis()));
        final DateTime wakeUpTime = new DateTime(sleepSegment.getEndTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.getOffsetMillis()));
        final DateTime sleepLocalUTC = new DateTime(sleepTime.getYear(), sleepTime.getMonthOfYear(), sleepTime.getDayOfMonth(), sleepTime.getHourOfDay(), sleepTime.getMinuteOfHour(), DateTimeZone.UTC);
        final DateTime wakeUpLocalUTC = new DateTime(wakeUpTime.getYear(), wakeUpTime.getMonthOfYear(), wakeUpTime.getDayOfMonth(), wakeUpTime.getHourOfDay(), wakeUpTime.getMinuteOfHour(), DateTimeZone.UTC);

        assertThat(sleepLocalUTC, is(new DateTime(2014, 12, 03, 1, 39, DateTimeZone.UTC)));
        assertThat(wakeUpLocalUTC, is(new DateTime(2014, 12, 03, 7, 9, DateTimeZone.UTC)));
    }
}
