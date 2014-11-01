package com.hello.suripu.core.models;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by pangwu on 9/18/14.
 */
public class AlarmTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNotMatchWeekday(){
        final Set<Integer> dayOfWeek = new HashSet<>();
        dayOfWeek.add((DateTime.now().getDayOfWeek() + 1) % 7);

        final Alarm alarm = new Alarm.Builder()
                .withYear(DateTime.now().getYear())
                .withMonth(DateTime.now().getMonthOfYear())
                .withDay(DateTime.now().getDayOfMonth())
                .withDayOfWeek(dayOfWeek)
                .withHour(1)
                .withMinute(1)
                .withIsRepeated(false)
                .build();

    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidWeekday(){
        final Set<Integer> dayOfWeek = new HashSet<>();
        dayOfWeek.add(8);

        final Alarm alarm = new Alarm.Builder()
                .withYear(0)
                .withMonth(0)
                .withDay(0)
                .withHour(1)
                .withMinute(1)
                .withDayOfWeek(dayOfWeek)
                .withIsRepeated(true)
                .build();

    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testNonRepeated(){
        final Set<Integer> dayOfWeek = new HashSet<>();
//        dayOfWeek.add(3);
        final Alarm alarm = new Alarm.Builder()
                .withYear(2014)
                .withMonth(10)
                .withDay(29)
                .withHour(1)
                .withMinute(1)
                .withDayOfWeek(dayOfWeek)
                .withIsRepeated(false)
                .build();

        assertThat(alarm.isRepeated, is(false));
    }


}
