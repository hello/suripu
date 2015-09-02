package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.TimelineFeedback;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FeedbackUtilsTest {

    @Before
    public void setup() {
       // DateTimeZone.setDefault(DateTimeZone.UTC);
    }

    @Test
    public void TestConsistency() {
        final FeedbackUtils feedbackUtils = new FeedbackUtils();

        final int offset = 3600000; // + 1 hour

        final TimelineFeedback feedback1 = TimelineFeedback.create("2015-04-15","22:41","22:45",Event.Type.IN_BED.name());
        final TimelineFeedback feedback2 = TimelineFeedback.create("2015-04-15","22:42","22:46",Event.Type.SLEEP.name());
        final TimelineFeedback feedback4 = TimelineFeedback.create("2015-04-15","04:41","04:48",Event.Type.OUT_OF_BED.name());

        final List<TimelineFeedback> timelineFeedbacks = new ArrayList<>();
        timelineFeedbacks.add(feedback1);
        timelineFeedbacks.add(feedback2);
        timelineFeedbacks.add(feedback4);

        final TimelineFeedback feedback3a = TimelineFeedback.create("2015-04-15","04:40","04:47",Event.Type.WAKE_UP.name());
        final TimelineFeedback feedback3b = TimelineFeedback.create("2015-04-15","04:41","04:48",Event.Type.WAKE_UP.name());
        final TimelineFeedback feedback3c = TimelineFeedback.create("2015-04-15","04:42","04:49",Event.Type.WAKE_UP.name());

        final Optional<DateTime> wakeTime3a = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback3a,offset);
        final Optional<DateTime> wakeTime3b = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback3b,offset);
        final Optional<DateTime> wakeTime3c = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback3c,offset);

        TestCase.assertTrue(feedbackUtils.checkEventOrdering(ImmutableList.copyOf(timelineFeedbacks),wakeTime3a.get().getMillis(), Event.Type.WAKE_UP,offset));
        TestCase.assertFalse(feedbackUtils.checkEventOrdering(ImmutableList.copyOf(timelineFeedbacks), wakeTime3b.get().getMillis(), Event.Type.WAKE_UP, offset));
        TestCase.assertFalse(feedbackUtils.checkEventOrdering(ImmutableList.copyOf(timelineFeedbacks),wakeTime3c.get().getMillis(), Event.Type.WAKE_UP,offset));

    }

    @Test
    public void TestFeedbackReprocessing() {

        final List<Event> events = new ArrayList<>();

        final long t1 = 1429134060000L; //Wed, 15 Apr 2015 21:41:00 GMT
        final long t2 = t1 + 60000L;
        final int offset = 3600000; // + 1 hour
        final long expectedTime = t1 + 4 * 60000L;


        events.add(Event.createFromType(Event.Type.SLEEP,t1,t2,offset,Optional.of("FOOBARS"),Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent()));

        /*

            @JsonProperty("date_of_night") final String dateOfNight,
            @JsonProperty("old_time_of_event") final String oldTimeOfEvent,
            @JsonProperty("new_time_of_event") final String newTimeOfEvent,
            @JsonProperty("event_type") final String eventTypeString) {


         */
        final TimelineFeedback feedback = TimelineFeedback.create("2015-04-15","22:41","22:45",Event.Type.SLEEP.name());

        final List<TimelineFeedback> timelineFeedbacks = new ArrayList<>();
        timelineFeedbacks.add(feedback);

        final FeedbackUtils utils = new FeedbackUtils();
        final FeedbackUtils.ReprocessedEvents newEvents = utils.reprocessEventsBasedOnFeedback(ImmutableList.copyOf(timelineFeedbacks),ImmutableList.copyOf(events), ImmutableList.copyOf(Collections.EMPTY_LIST), offset);


        final long mysleeptime = newEvents.mainEvents.get(0).getStartTimestamp();

        TestCase.assertEquals(expectedTime, mysleeptime);

    }

    @Test
    public void TestApproximateFeedbackReprocessing() {

        final List<Event> events = new ArrayList<>();

        final long t1 = 1429134060000L; //Wed, 15 Apr 2015 21:41:00 GMT
        final long t2 = t1 + 60000L;
        final int offset = 3600000; // + 1 hour
        final long expectedTime = t1 + 4 * 60000L;


        events.add(Event.createFromType(Event.Type.SLEEP,t1,t2,offset,Optional.of("FOOBARS"),Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent()));

        /*

            @JsonProperty("date_of_night") final String dateOfNight,
            @JsonProperty("old_time_of_event") final String oldTimeOfEvent,
            @JsonProperty("new_time_of_event") final String newTimeOfEvent,
            @JsonProperty("event_type") final String eventTypeString) {


         */
        final TimelineFeedback feedback = TimelineFeedback.create("2015-04-15","22:40","22:45",Event.Type.SLEEP.name());

        final List<TimelineFeedback> timelineFeedbacks = new ArrayList<>();
        timelineFeedbacks.add(feedback);

        final FeedbackUtils utils = new FeedbackUtils();

        final FeedbackUtils.ReprocessedEvents newEvents = utils.reprocessEventsBasedOnFeedback(ImmutableList.copyOf(timelineFeedbacks),ImmutableList.copyOf(events), ImmutableList.copyOf(Collections.EMPTY_LIST), offset);


        final long mysleeptime = newEvents.mainEvents.get(0).getStartTimestamp();

        TestCase.assertEquals(expectedTime, mysleeptime);

    }

    public void TestGettingTheClosestFeedback() {
        FeedbackUtils utils = new FeedbackUtils();
//
// Wed, 15 Apr 2015 21:41:00 GMT
        final long t1 = 1429134060000L; //22:41

        final long t2 = t1 + 1 * 60000L; //22:42
        final long t3 = t1 - 3 * 60000L; //22:38
        final long t4 = t1 + 2 * 60000L; //22:43
        final long t5 = t1 + 3 * 60000L; //22:44

        final int offset = 3600000; // + 1 hour
        final long expectedTime = t1 + 4 * 60000L;

        final List<Event> events = new ArrayList<>();
        final List<Event> extraEvents = new ArrayList<>();

        events.add(Event.createFromType(Event.Type.SLEEP,t1,t1 + 60000L,offset,Optional.of("FOOBARS1"),
                Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent())); //matches feedback2

        events.add(Event.createFromType(Event.Type.SLEEP,t2,t2 + 60000L,offset,Optional.of("FOOBARS2"),
                Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent())); //should remain itself

        events.add(Event.createFromType(Event.Type.SLEEP,t3,t3 + 60000L,offset,Optional.of("FOOBARS3"),
                Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent())); //matches feedback1

        extraEvents.add(Event.createFromType(Event.Type.SLEEP,t4,t4 + 60000L,offset,Optional.of("FOOBARS4"),
                Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent())); //should remain itself

        extraEvents.add(Event.createFromType(Event.Type.SLEEP,t5,t5 + 60000L,offset,Optional.of("FOOBARS5"),
                Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent())); //matches feedback3


        final TimelineFeedback feedback1 = TimelineFeedback.create("2015-04-15","22:30","22:35",Event.Type.SLEEP.name());
        final TimelineFeedback feedback2 = TimelineFeedback.create("2015-04-15","22:40","22:45",Event.Type.SLEEP.name());
        final TimelineFeedback feedback3 = TimelineFeedback.create("2015-04-15","22:50","22:55",Event.Type.SLEEP.name());

        final List<TimelineFeedback> timelineFeedbacks = new ArrayList<>();
        timelineFeedbacks.add(feedback1);
        timelineFeedbacks.add(feedback2);
        timelineFeedbacks.add(feedback3);

        final FeedbackUtils.ReprocessedEvents newEvents = utils.reprocessEventsBasedOnFeedback(ImmutableList.copyOf(timelineFeedbacks),ImmutableList.copyOf(events), ImmutableList.copyOf(extraEvents), offset);

        final long ref = 1429134000000L; //Wed, 15 Apr 2015 21:40:00 GMT

        Map<String,Event> newEventMap = Maps.newHashMap();
        Map<String,Event> extraEventMap = Maps.newHashMap();

        for (Event event : newEvents.mainEvents) {
            newEventMap.put(event.getDescription(),event);
        }

        for (Event event : newEvents.extraEvents) {
            extraEventMap.put(event.getDescription(),event);
        }

        TestCase.assertEquals(newEventMap.get("FOOBARS1").getStartTimestamp(), ref + 5*60000L);
        TestCase.assertEquals(newEventMap.get("FOOBARS2").getStartTimestamp(), t2);
        TestCase.assertEquals(newEventMap.get("FOOBARS3").getStartTimestamp(), ref - 5*60000L);
        TestCase.assertEquals(extraEventMap.get("FOOBARS4").getStartTimestamp(), t4);
        TestCase.assertEquals(extraEventMap.get("FOOBARS5").getStartTimestamp(), ref + 15*60000L);




    }


    @Test
    public void testConvertFeedbackToDateTimeWakeup() {
        final TimelineFeedback feedback = TimelineFeedback.create("2015-02-03", "07:00", "7:15", Event.Type.WAKE_UP.name());
        final Optional<DateTime> optional = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback, 0);
        assertThat(optional.isPresent(), is(true));
        final DateTime dt = optional.get();
        assertThat(dt.getDayOfMonth(), equalTo(4));
        assertThat(dt.getHourOfDay(), equalTo(7));
        assertThat(dt.getMinuteOfHour(), equalTo(15));


        final TimelineFeedback secondFeedback = TimelineFeedback.create("2015-02-03", "07:00", "14:15", Event.Type.WAKE_UP.name());
        final Optional<DateTime> secondOptional = FeedbackUtils.convertFeedbackToDateTimeByNewTime(secondFeedback, 0);
        assertThat(secondOptional.isPresent(), is(true));
        final DateTime secondDt = secondOptional.get();
        assertThat(secondDt.getDayOfMonth(), equalTo(4));
        assertThat(secondDt.getHourOfDay(), equalTo(14));
        assertThat(secondDt.getMinuteOfHour(), equalTo(15));
    }

    @Test
    public void testConvertFeedbackToDateTimeInvalidWakeUp() {
        final TimelineFeedback feedback = TimelineFeedback.create("2015-02-03", "07:00", "23:15", Event.Type.WAKE_UP.name());
        final Optional<DateTime> optional = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback, 0);
        assertThat(optional.isPresent(), is(false));
    }



    @Test
    public void testConvertFeedbackToDateTimeFallasleep() {
        final TimelineFeedback feedback = TimelineFeedback.create("2015-02-03", "23:00", "00:15", Event.Type.SLEEP.name());
        final Optional<DateTime> optional = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback, 0);
        assertThat(optional.isPresent(), is(true));
        final DateTime dt = optional.get();
        assertThat(dt.getDayOfMonth(), equalTo(4));
        assertThat(dt.getHourOfDay(), equalTo(0));
        assertThat(dt.getMinuteOfHour(), equalTo(15));


        final TimelineFeedback secondFeedback = TimelineFeedback.create("2015-02-03", "00:15", "23:15", Event.Type.SLEEP.name());
        final Optional<DateTime> secondOptional = FeedbackUtils.convertFeedbackToDateTimeByNewTime(secondFeedback, 0);
        assertThat(secondOptional.isPresent(), is(true));
        final DateTime secondDt = secondOptional.get();
        assertThat(secondDt.getDayOfMonth(), equalTo(3));
        assertThat(secondDt.getHourOfDay(), equalTo(23));
        assertThat(secondDt.getMinuteOfHour(), equalTo(15));
    }

    @Test
    public void testConvertFeedbackToDateTimeInvalidFallAsleep() {
        final TimelineFeedback feedback = TimelineFeedback.create("2015-02-03", "23:00", "16:00", Event.Type.SLEEP.name());
        final Optional<DateTime> optional = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback, 0);
        assertThat(optional.isPresent(), is(false));
    }
}
