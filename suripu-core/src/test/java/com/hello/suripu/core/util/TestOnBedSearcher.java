package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.processors.OnlineHmmTest;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.Test;

/**
 * Created by benjo on 2/5/16.
 */
public class TestOnBedSearcher {

    @Test
    public void testOnBedSearcherSimple() {

        final DateTime date = DateTimeUtil.ymdStringToDateTime("2015-09-01");
        final DateTime startTime = date.withHourOfDay(18);
        final DateTime endTime = startTime.plusHours(16);
        final int tzOffset = 5* DateTimeConstants.MILLIS_PER_HOUR;

        //in the middle
        final long tsleep = (startTime.withZone(DateTimeZone.UTC).getMillis() + endTime.withZone(DateTimeZone.UTC).getMillis()) / 2;
        final long tInBed = tsleep - DateTimeConstants.MILLIS_PER_HOUR*1000L;

        final ImmutableList<TrackerMotion> trackerMotion = OnlineHmmTest.getTypicalDayOfPill(startTime,endTime,tzOffset);

        final Event sleep = Event.createFromType(Event.Type.SLEEP,tsleep,tsleep + DateTimeConstants.MILLIS_PER_MINUTE,tzOffset, Optional.of("ZZZZZ"),Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent());

        final Event inbed = Event.createFromType(Event.Type.IN_BED,tInBed,tInBed + DateTimeConstants.MILLIS_PER_MINUTE,tzOffset, Optional.of("INBED"),Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent());

        final Event newInBed = InBedSearcher.getInBedPlausiblyBeforeSleep(startTime,endTime,sleep,inbed,15,trackerMotion);


        TestCase.assertTrue(newInBed.getStartTimestamp() > inbed.getStartTimestamp());
        TestCase.assertTrue(newInBed.getStartTimestamp() > startTime.getMillis());


    }
}
