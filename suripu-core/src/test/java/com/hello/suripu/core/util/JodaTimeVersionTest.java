package com.hello.suripu.core.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

/**
 * Created by pangwu on 4/10/15.
 */
public class JodaTimeVersionTest {
    @Test
    public void testUsingTheRightJodaTime(){
        final DateTime dateTimeFromYmd = new DateTime(2015, 4, 11, 10, 30, 0, 0, DateTimeZone.forID("Europe/Moscow"));

        final long diff = dateTimeFromYmd.getMillis() - 1428733800000L;
        final DateTime dateTimeFromMillis = new DateTime(1428733800000L, DateTimeZone.forID("Europe/Moscow"));
    }
}
