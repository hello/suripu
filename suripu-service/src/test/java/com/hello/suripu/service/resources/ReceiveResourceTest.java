package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.service.configuration.SenseUploadConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by pangwu on 1/23/15.
 */
public class ReceiveResourceTest {

    @Test
    public void testComputeNextUploadInterval(){
        final SenseUploadConfiguration senseUploadConfiguration = new SenseUploadConfiguration();
        final long actualRingTime = DateTime.now().plusMinutes(3).withSecondOfMinute(0).withMillisOfSecond(0).getMillis();

        final RingTime nextRingTime = new RingTime(actualRingTime, actualRingTime, new long[0], false);
        final int uploadCycle = ReceiveResource.computeNextUploadInterval(nextRingTime, DateTime.now(), senseUploadConfiguration, false);
        assertThat(uploadCycle, is(SenseUploadConfiguration.DEFAULT_UPLOAD_INTERVAL));

    }

    @Test
    public void testShouldWriteRingHistory(){
        final DateTime now = DateTime.now();
        final DateTime actualRingTime = now.plusMinutes(2);
        final DateTime expectedRingTime = now.plusMinutes(3);
        final RingTime ringTime = new RingTime(actualRingTime.getMillis(), expectedRingTime.getMillis(), new long[0], true);

        assertThat(ReceiveResource.shouldWriteRingTimeHistory(now, ringTime, 3), is(true));
        assertThat(ReceiveResource.shouldWriteRingTimeHistory(now, ringTime, 1), is(false));
        assertThat(ReceiveResource.shouldWriteRingTimeHistory(now.plusMinutes(5), ringTime, 1), is(false));
    }


    @Test
    public void testComputeNextUploadInterval1HourInFuture(){


        for(int i = 1; i < DateTimeConstants.MINUTES_PER_DAY; i++) {
            final SenseUploadConfiguration senseUploadConfiguration = new SenseUploadConfiguration();
            final long actualRingTime = DateTime.now().plusMinutes(i).withSecondOfMinute(0).withMillisOfSecond(0).getMillis();

            final RingTime nextRingTime = new RingTime(actualRingTime, actualRingTime, new long[0], false);
            final int uploadCycle = ReceiveResource.computeNextUploadInterval(nextRingTime, DateTime.now(), senseUploadConfiguration, false);
            assertThat(uploadCycle <= 4, is(true));
        }
    }

    @Test
    public void testComputeNextUploadIntervalAlarmInThePast(){


        for(int i = 1; i < DateTimeConstants.MINUTES_PER_DAY; i++) {
            final SenseUploadConfiguration senseUploadConfiguration = new SenseUploadConfiguration();
            final long actualRingTime = DateTime.now().minusMinutes(i).withSecondOfMinute(0).withMillisOfSecond(0).getMillis();

            final RingTime nextRingTime = new RingTime(actualRingTime, actualRingTime, new long[0], false);
            final int uploadCycle = ReceiveResource.computeNextUploadInterval(nextRingTime, DateTime.now(), senseUploadConfiguration, false);
            assertThat(uploadCycle <= senseUploadConfiguration.getDefaultUploadInterval(), is(true));
        }
    }

    @Test
    public void testComputeNextUploadIntervalShouldBePositive(){

        for(int i = 1; i < DateTimeConstants.MINUTES_PER_DAY; i++) {
            final SenseUploadConfiguration senseUploadConfiguration = new SenseUploadConfiguration();
            final long actualRingTime = DateTime.now().minusMinutes(i).withSecondOfMinute(0).withMillisOfSecond(0).getMillis();

            final RingTime nextRingTime = new RingTime(actualRingTime, actualRingTime, new long[0], false);
            final int uploadCycle = ReceiveResource.computeNextUploadInterval(nextRingTime, DateTime.now(), senseUploadConfiguration, false);
            assertThat(uploadCycle > 0, is(true));
        }
    }

    @Test
    public void testComputeNextUploadIntervalRandomNow(){

        final Random random = new Random(DateTime.now().getMillis());

        for(int i = 1; i < DateTimeConstants.MINUTES_PER_DAY; i++) {
            final SenseUploadConfiguration senseUploadConfiguration = new SenseUploadConfiguration();
            final long actualRingTime = DateTime.now().plusMinutes(i).withSecondOfMinute(0).withMillisOfSecond(0).getMillis();

            final DateTime current = new DateTime(random.nextLong());
            final RingTime nextRingTime = new RingTime(actualRingTime, actualRingTime, new long[0], false);
            final int uploadCycle = ReceiveResource.computeNextUploadInterval(nextRingTime, current, senseUploadConfiguration, false);
            assertThat(uploadCycle <= 5, is(true));
        }
    }

    @Test
    public void testComputePassRingTimeUploadIntervalRandomNow(){

        final Random random = new Random(DateTime.now().getMillis());

        for(int i = 1; i < DateTimeConstants.MINUTES_PER_DAY; i++) {
            final long actualRingTime = DateTime.now().plusMinutes(i).withSecondOfMinute(0).withMillisOfSecond(0).getMillis();

            final DateTime current = new DateTime(random.nextLong());
            final RingTime nextRingTime = new RingTime(actualRingTime, actualRingTime, new long[0], false);
            final int uploadCycle = ReceiveResource.computePassRingTimeUploadInterval(nextRingTime, current, 10);
            assertThat(uploadCycle <= 10, is(true));
        }
    }

    @Test
    public void testComputeNextUploadIntervalReduced(){

        final SenseUploadConfiguration senseUploadConfiguration = new SenseUploadConfiguration();
        final long actualRingTime = DateTime.now(DateTimeZone.UTC).withDayOfWeek(3).withHourOfDay(12).withSecondOfMinute(0).withMillisOfSecond(0).getMillis();

        final RingTime nextRingTime = new RingTime(actualRingTime, actualRingTime, new long[0], false);
        final int increasedUploadCycle = ReceiveResource.computeNextUploadInterval(nextRingTime, DateTime.now(DateTimeZone.UTC).withDayOfWeek(3).withHourOfDay(13).withMinuteOfHour(0), senseUploadConfiguration, true);
        final int uploadCycle = ReceiveResource.computeNextUploadInterval(nextRingTime, DateTime.now(DateTimeZone.UTC).withDayOfWeek(3).withHourOfDay(13).withMinuteOfHour(0), senseUploadConfiguration, false);

        final Integer increasedInterval = SenseUploadConfiguration.INCREASED_INTERVAL_NON_PEAK;
        assertThat(increasedInterval.equals(increasedInterval), is(true));
        assertThat(increasedInterval > uploadCycle, is(true));
    }

    @Test
    public void testFWBugClockSkewNotCorrected() {
        DateTime sampleDateTime = DateTime.now().plusHours(10);
        final Optional<Long> notCorrectedFutureDT = ReceiveResource.correctForPillClockSkewBug(sampleDateTime, DateTime.now());
        Long timestamp = 0L;
        if (notCorrectedFutureDT.isPresent()) {
            timestamp = notCorrectedFutureDT.get();
        }
        assertThat(timestamp, is(0L));

        sampleDateTime = DateTime.now().minusHours(10);
        final Optional<Long> notCorrectedPastDT = ReceiveResource.correctForPillClockSkewBug(sampleDateTime, DateTime.now());
        if (notCorrectedPastDT.isPresent()) {
            timestamp = notCorrectedFutureDT.get();
        }
        assertThat(timestamp, is(0L));
    }

    @Test
    public void testFWClockSkewBugCorrected(){
        final DateTime now = DateTime.now();
        DateTime sampleDateTime = now.plusMonths(6).plusSeconds(1);
        final Optional<Long> correctedDT = ReceiveResource.correctForPillClockSkewBug(sampleDateTime, now);
        Long timestamp = 0L;
        if (correctedDT.isPresent()) {
            timestamp = correctedDT.get();
        }
        final DateTime correctDT = now.plusSeconds(1);
        assertThat(timestamp, is(correctDT.getMillis()/1000L));
    }

    @Test
    public void testFWClockSkewBugWithFixedDate() {
        final DateTime now = DateTimeUtil.datetimeStringToDateTime("2015-10-07 17:56:53");
        final DateTime sampleDateTime = DateTimeUtil.datetimeStringToDateTime("2016-04-07 17:56:49");
        final Optional<Long> correctedDT = ReceiveResource.correctForPillClockSkewBug(sampleDateTime, now);
        Long timestamp = 0L;
        if (correctedDT.isPresent()) {
            timestamp = correctedDT.get();
        }
        final Long expectedTimestamp = sampleDateTime.minusMonths(DateTimeUtil.MONTH_OFFSET_FOR_CLOCK_BUG).getMillis()/1000L;
        assertThat(timestamp, is(expectedTimestamp));
    }
}
