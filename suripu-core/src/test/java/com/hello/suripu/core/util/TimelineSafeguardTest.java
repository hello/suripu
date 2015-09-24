package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.SleepSegment;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by benjo on 5/7/15.
 */
public class TimelineSafeguardTest {
    final long t0 = 1431044132000L;
    final long hourInMillis = 3600000L;
    final int hourInMinutes = 60;
    final private ImmutableList<Event> emptyEvents = ImmutableList.copyOf(Collections.EMPTY_LIST);

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

        TestCase.assertTrue(safeguards.checkEventOrdering(mainEventsSucceed, emptyEvents));
        TestCase.assertTrue(safeguards.checkEventOrdering(mainEventsDurationFail, emptyEvents));
        TestCase.assertFalse(safeguards.checkEventOrdering(mainEventsOrderFail1, emptyEvents));
        TestCase.assertFalse(safeguards.checkEventOrdering(mainEventsOrderFail2, emptyEvents));
        TestCase.assertFalse(safeguards.checkEventOrdering(mainEventsOrderFail3, emptyEvents));

        ImmutableList<Sample> light = ImmutableList.copyOf(getContiguousLightSensorData(t0, 8 * hourInMillis));

        TestCase.assertTrue(safeguards.checkIfValidTimeline(mainEventsSucceed, emptyEvents, light).equals(TimelineError.NO_ERROR));
        TestCase.assertFalse(safeguards.checkIfValidTimeline(mainEventsDurationFail, emptyEvents, light).equals(TimelineError.NO_ERROR));


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

        TestCase.assertTrue(safeguards.checkEventOrdering(mainEventsSucceed, ImmutableList.copyOf(goodExtraEvents)));

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

        TestCase.assertTrue(safeguards.checkEventOrdering(mainEventsSucceed, ImmutableList.copyOf(goodExtraEvents)));
        TestCase.assertFalse(safeguards.checkEventOrdering(mainEventsSucceed, ImmutableList.copyOf(badExtraEvents1)));
        TestCase.assertFalse(safeguards.checkEventOrdering(mainEventsSucceed, ImmutableList.copyOf(badExtraEvents2)));
        TestCase.assertFalse(safeguards.checkEventOrdering(mainEventsSucceed, ImmutableList.copyOf(badExtraEvents3)));
        TestCase.assertFalse(safeguards.checkEventOrdering(mainEventsSucceed, ImmutableList.copyOf(badExtraEvents4)));
        TestCase.assertFalse(safeguards.checkEventOrdering(mainEventsSucceed, ImmutableList.copyOf(badExtraEvents5)));


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

        TestCase.assertTrue(safeguards.checkIfValidTimeline(mainEventsSucceed, emptyEvents, lightWithHalfHourGap).equals(TimelineError.NO_ERROR));
        TestCase.assertFalse(safeguards.checkIfValidTimeline(mainEventsSucceed, emptyEvents, lightWithOverOneHourGap).equals(TimelineError.NO_ERROR));

    }
}
