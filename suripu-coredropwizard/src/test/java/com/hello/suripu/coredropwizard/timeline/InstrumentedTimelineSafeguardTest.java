package com.hello.suripu.coredropwizard.timeline;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.SensorReading;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.AlgorithmType;
import com.hello.suripu.core.util.CSVLoader;
import com.hello.suripu.core.util.SensorDataTimezoneMap;
import com.hello.suripu.core.util.TimeZoneOffsetMap;
import com.hello.suripu.core.util.TimelineError;
import com.hello.suripu.core.util.TimelineSafeguards;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by benjo on 5/7/15.
 */
public class InstrumentedTimelineSafeguardTest {
    final long t0 = 1477764420000L;
    final long hourInMillis = 3600000L;
    final int hourInMinutes = 60;
    final private ImmutableList<Event> emptyEvents = ImmutableList.copyOf(Collections.EMPTY_LIST);
    final long accountId = 0L;
    final AlgorithmType algType = AlgorithmType.NONE;
    final SleepPeriod sleepPeriod = SleepPeriod.createSleepPeriod(new DateTime(t0, DateTimeZone.UTC));
    final ImmutableList<TrackerMotion> trackerMotions = ImmutableList.copyOf(CSVLoader.loadTrackerMotionFromCSV("fixtures/motion_2016_10_29_dst.csv"));

    private Event getEvent(Event.Type type, final long time) {
        return Event.createFromType(type, time, time + 60000L, 0, Optional.of("BLAH BLAH"), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent());
    }

    private SleepEvents<Optional<Event>> getMainEvents(final long inBedTime, final long sleepTime, final long wakeTime, final long outOfBedTime) {
        SleepEvents<Optional<Event>> mainEvents = SleepEvents.create(
                Optional.of(getEvent(Event.Type.IN_BED, inBedTime)),
                Optional.of(getEvent(Event.Type.SLEEP, sleepTime)),
                Optional.of(getEvent(Event.Type.WAKE_UP, wakeTime)),
                Optional.of(getEvent(Event.Type.OUT_OF_BED, outOfBedTime)));

        return mainEvents;
    }

    private List<Sample> getContiguousLightSensorData(long t0, long tf) {
        List<Sample> mySamples = new ArrayList<>();

        final int N = (int) ((tf - t0) / 60000L);

        for (int i = 0; i < N; i++) {
            mySamples.add(new Sample(t0 + i * 60000L, 42, 0));
        }

        return mySamples;
    }

    @Test
    public void testSimpleTimeline() {
        final SleepEvents<Optional<Event>> mainEventsSucceed = getMainEvents(t0 + 0 * hourInMillis, t0 + 1 * hourInMillis, t0 + 8 * hourInMillis, t0 + 9 * hourInMillis);

        final SleepEvents<Optional<Event>> mainEventsDurationFail = getMainEvents(t0 + 0 * hourInMillis, t0 + 1 * hourInMillis, t0 + 2 * hourInMillis, t0 + 9 * hourInMillis);
        final SleepEvents<Optional<Event>> mainEventsOrderFail1 = getMainEvents(t0 + 2 * hourInMillis, t0 + 1 * hourInMillis, t0 + 2 * hourInMillis, t0 + 9 * hourInMillis);
        final SleepEvents<Optional<Event>> mainEventsOrderFail2 = getMainEvents(t0 + 0 * hourInMillis, t0 + 1 * hourInMillis, t0 + 10 * hourInMillis, t0 + 9 * hourInMillis);
        final SleepEvents<Optional<Event>> mainEventsOrderFail3 = getMainEvents(t0 + 0 * hourInMillis, t0 + 2 * hourInMillis, t0 + 1 * hourInMillis, t0 + 9 * hourInMillis);

        final TimelineSafeguards safeguards = new TimelineSafeguards();

        final int duration1 = safeguards.getTotalSleepDurationInMinutes(mainEventsSucceed.fallAsleep.get(), mainEventsSucceed.wakeUp.get(), emptyEvents);
        final int duration2 = safeguards.getTotalSleepDurationInMinutes(mainEventsDurationFail.fallAsleep.get(), mainEventsDurationFail.wakeUp.get(), emptyEvents);

        TestCase.assertEquals(duration1, 7 * hourInMinutes);
        TestCase.assertEquals(duration2, 1 * hourInMinutes);

        TestCase.assertTrue(safeguards.checkEventOrdering(accountId, algType, mainEventsSucceed, emptyEvents));
        TestCase.assertTrue(safeguards.checkEventOrdering(accountId, algType, mainEventsDurationFail, emptyEvents));
        TestCase.assertFalse(safeguards.checkEventOrdering(accountId, algType, mainEventsOrderFail1, emptyEvents));
        TestCase.assertFalse(safeguards.checkEventOrdering(accountId, algType, mainEventsOrderFail2, emptyEvents));
        TestCase.assertFalse(safeguards.checkEventOrdering(accountId, algType, mainEventsOrderFail3, emptyEvents));

        ImmutableList<Sample> light = ImmutableList.copyOf(getContiguousLightSensorData(t0, 8 * hourInMillis));

        TestCase.assertTrue(safeguards.checkIfValidTimeline(accountId, true,Optional.absent(), algType, mainEventsSucceed, emptyEvents, light, trackerMotions).equals(TimelineError.NO_ERROR));
        TestCase.assertFalse(safeguards.checkIfValidTimeline(accountId, true, Optional.absent(),algType, mainEventsDurationFail, emptyEvents, light, trackerMotions).equals(TimelineError.NO_ERROR));

        //no motion case
        TestCase.assertFalse(safeguards.checkIfValidTimeline(accountId, true,Optional.absent(), algType, mainEventsDurationFail, emptyEvents, light, ImmutableList.copyOf(Collections.EMPTY_LIST)).equals(TimelineError.NO_MOTION_DURING_SLEEP));

    }


    @Test
    public void testMultipleInBedOutOfBeds() {

        final TimelineSafeguards safeguards = new TimelineSafeguards();

        final SleepEvents<Optional<Event>> mainEventsSucceed = getMainEvents(t0 + 0 * hourInMillis, t0 + 1 * hourInMillis, t0 + 8 * hourInMillis, t0 + 9 * hourInMillis);

        final List<Event> goodExtraEvents = new ArrayList<>();

        goodExtraEvents.add(getEvent(Event.Type.WAKE_UP, t0 + (long) (1.5 * hourInMillis)));
        goodExtraEvents.add(getEvent(Event.Type.SLEEP, t0 + (long) (2.5 * hourInMillis)));

        goodExtraEvents.add(getEvent(Event.Type.WAKE_UP, t0 + (long) (3.5 * hourInMillis)));
        goodExtraEvents.add(getEvent(Event.Type.OUT_OF_BED, t0 + (long) (4.5 * hourInMillis)));
        goodExtraEvents.add(getEvent(Event.Type.IN_BED, t0 + (long) (5.5 * hourInMillis)));
        goodExtraEvents.add(getEvent(Event.Type.SLEEP, t0 + (long) (6.5 * hourInMillis)));

        TestCase.assertTrue(safeguards.checkEventOrdering(accountId, algType, mainEventsSucceed, ImmutableList.copyOf(goodExtraEvents)));

    }

    @Test
    public void testExtraEventsTimeline() {
        final TimelineSafeguards safeguards = new TimelineSafeguards();


        ImmutableList<Sample> light = ImmutableList.copyOf(getContiguousLightSensorData(t0, 8 * hourInMillis));

        final SleepEvents<Optional<Event>> mainEventsSucceed = getMainEvents(t0 + 0 * hourInMillis, t0 + 1 * hourInMillis, t0 + 8 * hourInMillis, t0 + 9 * hourInMillis);

        final List<Event> goodExtraEvents = new ArrayList<>();
        final List<Event> badExtraEvents1 = new ArrayList<>();
        final List<Event> badExtraEvents2 = new ArrayList<>();
        final List<Event> badExtraEvents3 = new ArrayList<>();
        final List<Event> badExtraEvents4 = new ArrayList<>();
        final List<Event> badExtraEvents5 = new ArrayList<>();

        final List<Event> extraEventsThatShortenTheDuration = new ArrayList<>();


        goodExtraEvents.add(getEvent(Event.Type.WAKE_UP, t0 + (long) (1.5 * hourInMillis)));
        goodExtraEvents.add(getEvent(Event.Type.SLEEP, t0 + (long) (2.5 * hourInMillis)));

        //has no matching sleep
        badExtraEvents1.add(getEvent(Event.Type.WAKE_UP, t0 + (long) (1.5 * hourInMillis)));

        //has no matching wakeup
        badExtraEvents2.add(getEvent(Event.Type.SLEEP, t0 + (long) (1.5 * hourInMillis)));

        //sleep comes before wakeup
        badExtraEvents3.add(getEvent(Event.Type.SLEEP, t0 + (long) (1.5 * hourInMillis)));
        badExtraEvents3.add(getEvent(Event.Type.WAKE_UP, t0 + (long) (2.5 * hourInMillis)));

        //has an in-bed
        badExtraEvents4.add(getEvent(Event.Type.WAKE_UP, t0 + (long) (1.5 * hourInMillis)));
        badExtraEvents4.add(getEvent(Event.Type.SLEEP, t0 + (long) (2.5 * hourInMillis)));
        badExtraEvents4.add(getEvent(Event.Type.IN_BED, t0 + (long) (3.5 * hourInMillis)));

        //time is out of order in list
        badExtraEvents5.add(getEvent(Event.Type.WAKE_UP, t0 + (long) (2.5 * hourInMillis)));
        badExtraEvents5.add(getEvent(Event.Type.SLEEP, t0 + (long) (1.5 * hourInMillis)));


        extraEventsThatShortenTheDuration.add(getEvent(Event.Type.WAKE_UP, t0 + (long) (1.5 * hourInMillis)));
        extraEventsThatShortenTheDuration.add(getEvent(Event.Type.SLEEP, t0 + (long) (2.5 * hourInMillis)));
        extraEventsThatShortenTheDuration.add(getEvent(Event.Type.WAKE_UP, t0 + (long) (3.5 * hourInMillis)));
        extraEventsThatShortenTheDuration.add(getEvent(Event.Type.SLEEP, t0 + (long) (4.5 * hourInMillis)));

        TestCase.assertTrue(safeguards.checkEventOrdering(accountId, algType, mainEventsSucceed, ImmutableList.copyOf(goodExtraEvents)));
        TestCase.assertFalse(safeguards.checkEventOrdering(accountId, algType, mainEventsSucceed, ImmutableList.copyOf(badExtraEvents1)));
        TestCase.assertFalse(safeguards.checkEventOrdering(accountId, algType, mainEventsSucceed, ImmutableList.copyOf(badExtraEvents2)));
        TestCase.assertFalse(safeguards.checkEventOrdering(accountId, algType, mainEventsSucceed, ImmutableList.copyOf(badExtraEvents3)));
        TestCase.assertFalse(safeguards.checkEventOrdering(accountId, algType, mainEventsSucceed, ImmutableList.copyOf(badExtraEvents4)));
        TestCase.assertFalse(safeguards.checkEventOrdering(accountId, algType, mainEventsSucceed, ImmutableList.copyOf(badExtraEvents5)));


        final int duration1 = safeguards.getTotalSleepDurationInMinutes(mainEventsSucceed.fallAsleep.get(), mainEventsSucceed.wakeUp.get(), ImmutableList.copyOf(extraEventsThatShortenTheDuration));

        TestCase.assertEquals(duration1,5*hourInMinutes);

    }

    @Test
    public void testGapInSensorData() {

        final TimelineSafeguards safeguards = new TimelineSafeguards();
        final SleepEvents<Optional<Event>> mainEventsSucceed = getMainEvents(t0 + 0 * hourInMillis, t0 + 1 * hourInMillis, t0 + 8 * hourInMillis, t0 + 9 * hourInMillis);

        final List<Sample> l1 = getContiguousLightSensorData(t0, t0 + 1 * hourInMillis);
        final List<Sample> l2 = getContiguousLightSensorData(t0 + (long)(1.5 * hourInMillis), t0 +  2 * hourInMillis);
        final List<Sample> l3 = getContiguousLightSensorData(t0 + 3 * hourInMillis,t0 +  4 * hourInMillis);

        final List<Sample> goodList = new ArrayList<>();
        goodList.addAll(l1);
        goodList.addAll(l2);

        final List<Sample> badList = new ArrayList<>();
        badList.addAll(l1);
        badList.addAll(l3);

        final ImmutableList<Sample> lightWithHalfHourGap = ImmutableList.copyOf(goodList);
        final ImmutableList<Sample> lightWithOverOneHourGap = ImmutableList.copyOf(badList);

        final int res1 = safeguards.getMaximumDataGapInMinutes(lightWithHalfHourGap);
        final int res2 = safeguards.getMaximumDataGapInMinutes(lightWithOverOneHourGap);
        TestCase.assertEquals(res1, 30);
        TestCase.assertEquals(res2, 120);
        TestCase.assertTrue(safeguards.checkIfValidTimeline(accountId, true,Optional.absent(),algType, mainEventsSucceed, emptyEvents, lightWithHalfHourGap, trackerMotions).equals(TimelineError.NO_ERROR));
        TestCase.assertFalse(safeguards.checkIfValidTimeline(accountId,true,Optional.absent(),algType, mainEventsSucceed, emptyEvents, lightWithOverOneHourGap, trackerMotions).equals(TimelineError.NO_ERROR));

    }


    @Test
    public void testInvalidNight() {


        final List<TrackerMotion> originalMotionData = Lists.newArrayList();
        final List<TrackerMotion> filteredMotionData = Lists.newArrayList();
        final List<TrackerMotion> originalPartnerMotionData = Lists.newArrayList();
        final List<TrackerMotion> originalMotionDataLowerThreshold = Lists.newArrayList();


        TestCase.assertTrue(InstrumentedTimelineProcessor.isValidNight(0L, originalMotionData, filteredMotionData, originalPartnerMotionData, false, true) == TimelineError.NO_DATA);
        /*@JsonProperty("id") final long id,
                         @JsonProperty("account_id") final long accountId,
                         @JsonProperty("tracker_id") final Long trackerId,
                         @JsonProperty("timestamp") final long timestamp,
                         @JsonProperty("value") final int value,
                         @JsonProperty("timezone_offset") final int timeZoneOffset,
                         final Long motionRange,
                         final Long kickOffCounts,
                         final Long onDurationInSeconds){*/

        final long t1 = 1444331495000L;
        final int lowval = 100;
        final int highval = 10000;

        //add a bunch of low values, not separated in time
        for (int i = 0; i < InstrumentedTimelineProcessor.MIN_TRACKER_MOTION_COUNT - 1; i++) {
            originalMotionData.add(new TrackerMotion(0L,0L,0L,t1 + 60000L * i,lowval,0,0L,0L,0L));
        }
        for (int i = 0; i < InstrumentedTimelineProcessor.MIN_TRACKER_MOTION_COUNT_LOWER_THRESHOLD - 1; i++) {
            originalMotionDataLowerThreshold.add(new TrackerMotion(0L,0L,0L,t1 + 5400000L * i,highval,0,0L,0L,0L));
        }


        TestCase.assertEquals(TimelineError.NOT_ENOUGH_DATA,InstrumentedTimelineProcessor.isValidNight(0L, originalMotionData, originalMotionData,originalPartnerMotionData, false, true));

        originalMotionData.add(new TrackerMotion(0L,0L,0L,t1 + 60000L * InstrumentedTimelineProcessor.MIN_TRACKER_MOTION_COUNT,lowval,0,0L,0L,0L));

        TestCase.assertEquals(TimelineError.LOW_AMP_DATA, InstrumentedTimelineProcessor.isValidNight(0L, originalMotionData, originalMotionData,originalPartnerMotionData, false, true));

        originalMotionData.add(new TrackerMotion(0L,0L,0L,t1 + 60000L * (InstrumentedTimelineProcessor.MIN_TRACKER_MOTION_COUNT + 1),highval,0,0L,0L,0L));

        TestCase.assertEquals(TimelineError.TIMESPAN_TOO_SHORT, InstrumentedTimelineProcessor.isValidNight(0L, originalMotionData, originalMotionData,originalPartnerMotionData, false, true));

        originalMotionData.add(new TrackerMotion(0L,0L,0L,t1 + 60000L * 1000,highval,0,0L,0L,0L));

        TestCase.assertEquals(TimelineError.NO_ERROR,InstrumentedTimelineProcessor.isValidNight(0L, originalMotionData, originalMotionData,originalPartnerMotionData, false, true));

        TestCase.assertEquals(TimelineError.PARTNER_FILTER_REJECTED_DATA,InstrumentedTimelineProcessor.isValidNight(0L, originalMotionData, filteredMotionData,originalPartnerMotionData, false, true));

        for (int i = 0; i < InstrumentedTimelineProcessor.MIN_PARTNER_FILTERED_MOTION_COUNT - 1; i++) {
            filteredMotionData.add(new TrackerMotion(0L,0L,0L,t1 + 60000L * i,lowval,0,0L,0L,0L));
        }

        TestCase.assertEquals(TimelineError.PARTNER_FILTER_REJECTED_DATA,InstrumentedTimelineProcessor.isValidNight(0L, originalMotionData, filteredMotionData,originalPartnerMotionData, false, true));

        filteredMotionData.add(new TrackerMotion(0L,0L,0L,t1 + 60000L * InstrumentedTimelineProcessor.MIN_PARTNER_FILTERED_MOTION_COUNT,lowval,0,0L,0L,0L));

        TestCase.assertEquals(TimelineError.PARTNER_FILTER_REJECTED_DATA,InstrumentedTimelineProcessor.isValidNight(0L, originalMotionData, filteredMotionData,originalPartnerMotionData, false, true));

        filteredMotionData.add(new TrackerMotion(0L,0L,0L,t1 + 60000L * 1000,lowval,0,0L,0L,0L));

        TestCase.assertEquals(TimelineError.NO_ERROR,InstrumentedTimelineProcessor.isValidNight(0L, originalMotionData, filteredMotionData,originalPartnerMotionData, false, true));

        TestCase.assertEquals(TimelineError.NOT_ENOUGH_DATA,InstrumentedTimelineProcessor.isValidNight(0L, originalMotionDataLowerThreshold, originalMotionDataLowerThreshold,originalPartnerMotionData, true, true));
        TestCase.assertEquals(TimelineError.NOT_ENOUGH_DATA,InstrumentedTimelineProcessor.isValidNight(0L, originalMotionDataLowerThreshold, originalMotionDataLowerThreshold,originalPartnerMotionData, true,true));

        originalMotionDataLowerThreshold.add(new TrackerMotion(0L,0L,0L,t1 + 60000L * 1000,highval,0,0L,0L,0L));

        TestCase.assertEquals(TimelineError.NO_ERROR,InstrumentedTimelineProcessor.isValidNight(0L, originalMotionDataLowerThreshold, originalMotionDataLowerThreshold,originalPartnerMotionData, true, true));

        TestCase.assertEquals(TimelineError.NOT_ENOUGH_DATA,InstrumentedTimelineProcessor.isValidNight(0L, originalMotionDataLowerThreshold, originalMotionDataLowerThreshold,originalMotionDataLowerThreshold, true, true));



    }
    @Test
    public void testTimelineTimezoneOffsetMapping() {
        final List<Sample> samples = Lists.newArrayList();
        final long t0 = 1446576648000L;
        int offset = 0;
        long tf = 0;
        long tOfChange = 0;
        int i;

        samples.add(new Sample(t0 - 60000L,42,offset));
        offset += 1;

        for (i = 0; i < 16 * 60; i++) {

            tf = t0 + i*60000L;

            if (i == 8*60) {
                offset = 2;
                tOfChange = tf;
            }

            samples.add(new Sample(tf,42,offset));
        }

        offset += 1;
        samples.add(new Sample(tf + 60000L,42,offset));



        final SensorDataTimezoneMap sensorDataTimezoneMap = SensorDataTimezoneMap.create(samples);

        final int offsetFirst = sensorDataTimezoneMap.get(t0 - 5*60000L);
        final int offsetBefore = sensorDataTimezoneMap.get(tOfChange - 60000L);
        final int offsetAt = sensorDataTimezoneMap.get(tOfChange - 1000L);
        final int offsetAfter = sensorDataTimezoneMap.get(tOfChange + 60001L);
        final int offsetLast = sensorDataTimezoneMap.get(tf + 5*60000L);

        TestCase.assertEquals(0,offsetFirst);
        TestCase.assertEquals(1,offsetBefore);
        TestCase.assertEquals(2,offsetAt);
        TestCase.assertEquals(2,offsetAfter);
        TestCase.assertEquals(3,offsetLast);

        final Event event0 = Event.createFromType(Event.Type.OUT_OF_BED, t0 - 40000L, t0 + 20000L, 42, Optional.<String>absent(), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent());
        final Event event1 = Event.createFromType(Event.Type.OUT_OF_BED, t0 + 60000L, t0 + 2*60000L, 42, Optional.<String>absent(), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent());
        final Event event2 = Event.createFromType(Event.Type.OUT_OF_BED, t0, t0 + 60000L, 42, Optional.<String>absent(), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent());
        final Event event3 = Event.createFromType(Event.Type.OUT_OF_BED, tf - 60000L, tf , 42, Optional.<String>absent(), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent());
        final Event event4 = Event.createFromType(Event.Type.OUT_OF_BED, tf + 1*60000L, tf + 2*60000L, 42, Optional.<String>absent(), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent());

        final SleepSegment sleepSegment0 = new SleepSegment(0L,event0,Lists.<SensorReading>newArrayList());
        final SleepSegment sleepSegment1 = new SleepSegment(0L,event1,Lists.<SensorReading>newArrayList());
        final SleepSegment sleepSegment2 = new SleepSegment(0L,event2,Lists.<SensorReading>newArrayList());
        final SleepSegment sleepSegment3 = new SleepSegment(0L,event3,Lists.<SensorReading>newArrayList());
        final SleepSegment sleepSegment4 = new SleepSegment(0L,event4,Lists.<SensorReading>newArrayList());


        final List<SleepSegment> segments = Lists.newArrayList(sleepSegment0,sleepSegment1,sleepSegment2,sleepSegment3,sleepSegment4);


        final List<SleepSegment> remapped = sensorDataTimezoneMap.remapSleepSegmentOffsets(segments);

        TestCase.assertEquals(remapped.get(0).getOffsetMillis(),0);
        TestCase.assertEquals(remapped.get(1).getOffsetMillis(),1);
        TestCase.assertEquals(remapped.get(2).getOffsetMillis(),1);
        TestCase.assertEquals(remapped.get(3).getOffsetMillis(),2);
        TestCase.assertEquals(remapped.get(4).getOffsetMillis(),3);
    }

    @Test
    public void testTimezoneOffsetMapping() {
        final List<Sample> samples = Lists.newArrayList();
        final long startUTC = 1478408400000L;//1478394000000L; //2016-11-05 18:00:00 local
        final long endUTC = 1478433600000L; //2016-11-06 16:00:00 local
        final String timeZoneID = "America/Los_Angeles";
        final TimeZoneHistory timeZoneHistory1 = new TimeZoneHistory(1468408400000L, -25200000, timeZoneID);
        final TimeZoneHistory timeZoneHistory2 = new TimeZoneHistory(1428408400000L, 3600000, "Europe/Berlin");
        final List<TimeZoneHistory> timeZoneHistoryList = new ArrayList<>();
        timeZoneHistoryList.add(timeZoneHistory1); timeZoneHistoryList.add(timeZoneHistory2);
        final TimeZoneOffsetMap timeZoneOffsetMap = TimeZoneOffsetMap.createFromTimezoneHistoryList(timeZoneHistoryList);
        int offsetA = -25200000;
        int offsetB = -28800000;
        long tf = 0;
        long tOfChange = 1478422740000L;

        final int offsetFirst = timeZoneOffsetMap.getOffsetWithDefaultAsZero(startUTC);
        final int offsetBefore = timeZoneOffsetMap.getOffsetWithDefaultAsZero(startUTC - 60000L);
        final int offsetAt = timeZoneOffsetMap.getOffsetWithDefaultAsZero(tOfChange);
        final int offsetAfter = timeZoneOffsetMap.getOffsetWithDefaultAsZero(tOfChange + 60001L);
        final int offsetLast = timeZoneOffsetMap.getOffsetWithDefaultAsZero(endUTC + 5*60000L);

        TestCase.assertEquals(offsetA,offsetFirst);
        TestCase.assertEquals(offsetA,offsetBefore);
        TestCase.assertEquals(offsetA,offsetAt);
        TestCase.assertEquals(offsetB,offsetAfter);
        TestCase.assertEquals(offsetB,offsetLast);
    }

    @Test
    public void testMaxMotionGap(){
        final long sleep1 = 1477764420000L;
        final long wake1 = 1477805040000L;
        final long sleep2 = 1477746060000L;
        final long wake2 = 1477805040000L;

        final int motionGap1 = TimelineSafeguards.getMaximumMotionGapInMinutes(trackerMotions, sleep1,wake1);
        assert (motionGap1 < TimelineSafeguards.MAXIMUM_ALLOWABLE_MOTION_GAP_ALTERNATIVE_PERIOD);

        final int motionGap2 = TimelineSafeguards.getMaximumMotionGapInMinutes(trackerMotions, sleep2,wake2);
        assert (motionGap2 > TimelineSafeguards.MAXIMUM_ALLOWABLE_MOTION_GAP_ALTERNATIVE_PERIOD);

    }

    @Test
    public void testMotionDuringSleepCheck(){
        //false positive night - motion at start and end of night
        ImmutableList<TrackerMotion> trackerMotions = ImmutableList.copyOf(CSVLoader.loadTrackerMotionFromCSV("fixtures/motion_2016_10_29_dst.csv"));
        long fallAsleepTime = 1478391100000L;
        long wakeUpTime = 1478401100000L;
        boolean testMotionDuringSleep = TimelineSafeguards.motionDuringSleepCheck(1,trackerMotions, fallAsleepTime, wakeUpTime);
        assertThat(testMotionDuringSleep, is(false));
        //okay night
        fallAsleepTime = 1477776780000L;
        wakeUpTime = 1477823640000L;
        testMotionDuringSleep = TimelineSafeguards.motionDuringSleepCheck(1, trackerMotions, fallAsleepTime, wakeUpTime);
        assertThat(testMotionDuringSleep, is(true));

    }

}
