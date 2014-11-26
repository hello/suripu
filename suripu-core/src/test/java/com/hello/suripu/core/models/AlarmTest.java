package com.hello.suripu.core.models;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by pangwu on 9/18/14.
 */
public class AlarmTest {



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


}
