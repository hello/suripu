package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.core.models.TimelineFeedback;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
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

        final int offset = 0; // + 1 hour
        final TimeZoneOffsetMap timeZoneOffsetMap = TimeZoneOffsetMap.createFromTimezoneHistoryList(Collections.emptyList());
        final TimelineFeedback feedback1 = TimelineFeedback.create("2015-04-14","22:41","22:45",Event.Type.IN_BED.name());
        final TimelineFeedback feedback2 = TimelineFeedback.create("2015-04-14","22:42","22:46",Event.Type.SLEEP.name());
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

        TestCase.assertTrue(feedbackUtils.checkEventOrdering(ImmutableList.copyOf(timelineFeedbacks),feedback3a,offset));
        TestCase.assertFalse(feedbackUtils.checkEventOrdering(ImmutableList.copyOf(timelineFeedbacks), feedback3b, offset));
        TestCase.assertFalse(feedbackUtils.checkEventOrdering(ImmutableList.copyOf(timelineFeedbacks), feedback3c, offset));

        final Map<Event.Type,Event> eventsByType = FeedbackUtils.getFeedbackAsEventsByType(ImmutableList.copyOf(timelineFeedbacks), offset);

        final Optional<Long> suggestedTime = feedbackUtils.suggestNewEventTimeBasedOnIntendedOrdering(FeedbackUtils.getTimesFromEventsMap(eventsByType), wakeTime3c.get().getMillis(), feedback3c.eventType);

        TestCase.assertTrue(suggestedTime.isPresent());
        TestCase.assertTrue(suggestedTime.get().equals(wakeTime3a.get().getMillis()));
    }

    @Test
    public void TestConsistencyEdgeCases() {
        final FeedbackUtils feedbackUtils = new FeedbackUtils();

        final int offset = 0; // + 1 hour

        final TimelineFeedback feedback1 = TimelineFeedback.create("2015-04-15","22:42","22:46",Event.Type.SLEEP.name());
        final TimelineFeedback feedback2 = TimelineFeedback.create("2015-04-15","04:41","04:48",Event.Type.WAKE_UP.name());
        final TimelineFeedback feedback3 = TimelineFeedback.create("2015-04-15","04:40","04:47",Event.Type.OUT_OF_BED.name());
        final TimelineFeedback feedback4 = TimelineFeedback.create("2015-04-15","22:40","22:50",Event.Type.IN_BED.name());

        final List<TimelineFeedback> timelineFeedbacks = new ArrayList<>();
        timelineFeedbacks.add(feedback1);
        timelineFeedbacks.add(feedback2);

        final Optional<DateTime> feedback1Time = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback1,offset);
        final Optional<DateTime> feedback2Time = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback2,offset);
        final Optional<DateTime> feedback3Time = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback3,offset);
        final Optional<DateTime> feedback4Time = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback4,offset);

        final Map<Event.Type,Event> eventsByType = FeedbackUtils.getFeedbackAsEventsByType(ImmutableList.copyOf(timelineFeedbacks), offset);

        {
            final Optional<Long> suggestedTime = feedbackUtils.suggestNewEventTimeBasedOnIntendedOrdering(FeedbackUtils.getTimesFromEventsMap(eventsByType), feedback3Time.get().getMillis(), feedback3.eventType);

            TestCase.assertTrue(suggestedTime.isPresent());
            TestCase.assertTrue(suggestedTime.get().equals(feedback2Time.get().plusMinutes(1).getMillis()));

        }

        {
            final Optional<Long> suggestedTime = feedbackUtils.suggestNewEventTimeBasedOnIntendedOrdering(FeedbackUtils.getTimesFromEventsMap(eventsByType), feedback4Time.get().getMillis(), feedback4.eventType);
            TestCase.assertTrue(suggestedTime.isPresent());
            TestCase.assertTrue(suggestedTime.get().equals(feedback1Time.get().minusMinutes(1).getMillis()));
        }
    }


    @Test
    public void TestFeedbackReprocessing() {

        final List<Event> events = new ArrayList<>();

        final long t1 = 1429134060000L; //Wed, 15 Apr 2015 21:41:00 GMT
        final long t2 = t1 + 60000L;
        final int offset = 7200000; // + 1 hour
        final long expectedTime = t1 + 4 * 60000L;

        final List<TimeZoneHistory> timeZoneHistoryList = new ArrayList<>();
        final TimeZoneHistory timeZoneHistory = new TimeZoneHistory(1329134060000L,7200000, "Europe/Berlin" );
        timeZoneHistoryList.add(timeZoneHistory);
        final TimeZoneOffsetMap timeZoneOffsetMap = TimeZoneOffsetMap.createFromTimezoneHistoryList(timeZoneHistoryList);
        events.add(Event.createFromType(Event.Type.SLEEP,t1,t2,offset,Optional.of("FOOBARS"),Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent()));
        events.add(Event.createFromType(Event.Type.WAKE_UP,t1 + 42,t2 + 42,offset,Optional.of("FOOBARS2"),Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent()));

        /*

            @JsonProperty("date_of_night") final String dateOfNight,
            @JsonProperty("old_time_of_event") final String oldTimeOfEvent,
            @JsonProperty("new_time_of_event") final String newTimeOfEvent,
            @JsonProperty("event_type") final String eventTypeString) {


         */
        final TimelineFeedback feedback = TimelineFeedback.create("2015-04-15","23:41","23:45",Event.Type.SLEEP.name());

        final List<TimelineFeedback> timelineFeedbacks = new ArrayList<>();
        timelineFeedbacks.add(feedback);

        final FeedbackUtils utils = new FeedbackUtils();
        final FeedbackUtils.ReprocessedEvents newEvents = utils.reprocessEventsBasedOnFeedback(SleepPeriod.Period.NIGHT, ImmutableList.copyOf(timelineFeedbacks),ImmutableList.copyOf(events), ImmutableList.copyOf(Collections.EMPTY_LIST), timeZoneOffsetMap);


        TestCase.assertTrue(newEvents.mainEvents.size() == 2);

        for (final Event event : newEvents.mainEvents.values()) {
            if (event.getType().equals(Event.Type.SLEEP)) {
                final long mysleeptime = event.getStartTimestamp();
                TestCase.assertEquals(expectedTime, mysleeptime);
            }
        }


    }

    @Test
    public void TestApproximateFeedbackReprocessing() {

        final List<Event> events = new ArrayList<>();

        final long t1 = 1429134060000L; //Wed, 15 Apr 2015 21:41:00 GMT
        final long t2 = t1 + 60000L;
        final int offset = 7200000; // + 1 hour
        final long expectedTime = t1 + 4 * 60000L;

        final List<TimeZoneHistory> timeZoneHistoryList = new ArrayList<>();
        final TimeZoneHistory timeZoneHistory = new TimeZoneHistory(1329134060000L,7200000, "Europe/Berlin" );
        timeZoneHistoryList.add(timeZoneHistory);
        final TimeZoneOffsetMap timeZoneOffsetMap = TimeZoneOffsetMap.createFromTimezoneHistoryList(timeZoneHistoryList);


        events.add(Event.createFromType(Event.Type.SLEEP,t1,t2,offset,Optional.of("FOOBARS"),Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent()));

        /*

            @JsonProperty("date_of_night") final String dateOfNight,
            @JsonProperty("old_time_of_event") final String oldTimeOfEvent,
            @JsonProperty("new_time_of_event") final String newTimeOfEvent,
            @JsonProperty("event_type") final String eventTypeString) {


         */
        final TimelineFeedback feedback = TimelineFeedback.create("2015-04-15","23:40","23:45",Event.Type.SLEEP.name());

        final List<TimelineFeedback> timelineFeedbacks = new ArrayList<>();
        timelineFeedbacks.add(feedback);

        final FeedbackUtils utils = new FeedbackUtils();

        final FeedbackUtils.ReprocessedEvents newEvents = utils.reprocessEventsBasedOnFeedback(SleepPeriod.Period.NIGHT, ImmutableList.copyOf(timelineFeedbacks),ImmutableList.copyOf(events), ImmutableList.copyOf(Collections.EMPTY_LIST), timeZoneOffsetMap);


        final long mysleeptime = newEvents.mainEvents.values().asList().get(0).getStartTimestamp();

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

        final int offset = 0; // + 1 hour
        final long expectedTime = t1 + 4 * 60000L;
        final TimeZoneOffsetMap timeZoneOffsetMap = TimeZoneOffsetMap.createFromTimezoneHistoryList(Collections.emptyList());

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

        final FeedbackUtils.ReprocessedEvents newEvents = utils.reprocessEventsBasedOnFeedback(SleepPeriod.Period.NIGHT, ImmutableList.copyOf(timelineFeedbacks),ImmutableList.copyOf(events), ImmutableList.copyOf(extraEvents), timeZoneOffsetMap);

        final long ref = 1429134000000L; //Wed, 15 Apr 2015 21:40:00 GMT

        Map<String,Event> newEventMap = Maps.newHashMap();
        Map<String,Event> extraEventMap = Maps.newHashMap();

        for (Event event : newEvents.mainEvents.values()) {
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
        final TimelineFeedback feedback = TimelineFeedback.create("2015-02-03", "07:00", "19:45", Event.Type.WAKE_UP.name());
        final Optional<DateTime> optional = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback, 0);
        assertThat(optional.isPresent(), is(false));

        final TimelineFeedback feedback2 = TimelineFeedback.create("2015-02-03", "07:00", "20:45", Event.Type.WAKE_UP.name());
        final Optional<DateTime> optional2 = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback2, 0);
        assertThat(optional2.isPresent(), is(true));

        final TimelineFeedback feedback3 = TimelineFeedback.create("2015-02-03",2, "07:00", "15:45", Event.Type.WAKE_UP.name());
        final Optional<DateTime> optional3 = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback3, 0);
        assertThat(optional3.isPresent(), is(true));

        final TimelineFeedback feedback4 = TimelineFeedback.create("2015-02-03", "07:00", "16:45", Event.Type.WAKE_UP.name());
        final Optional<DateTime> optional4 = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback4, 0);
        assertThat(optional4.isPresent(), is(false));

        final TimelineFeedback feedbackMorning = TimelineFeedback.create("2015-02-03", 0, "12:00", "02:04", Event.Type.WAKE_UP.name());
        final Optional<DateTime> optionalMorning = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedbackMorning, 0);
        assertThat(optionalMorning.isPresent(), is(false));

        final TimelineFeedback feedbackMorning2 = TimelineFeedback.create("2015-02-03", 0, "12:00", "04:04", Event.Type.WAKE_UP.name());
        final Optional<DateTime> optionalMorning2 = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedbackMorning2, 0);
        assertThat(optionalMorning2.isPresent(), is(true));

        final TimelineFeedback feedbackMorning3 = TimelineFeedback.create("2015-02-03", 0, "12:00", "23:59", Event.Type.WAKE_UP.name());
        final Optional<DateTime> optionalMorning3 = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedbackMorning3, 0);
        assertThat(optionalMorning3.isPresent(), is(true));

        final TimelineFeedback feedbackAfternoonValid = TimelineFeedback.create("2015-02-03", 1, "22:00", "02:04", Event.Type.WAKE_UP.name());
        final Optional<DateTime> optionalAfternoon = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedbackAfternoonValid, 0);
        assertThat(optionalAfternoon.isPresent(), is(true));

        final TimelineFeedback feedbackAfternoonValid1 = TimelineFeedback.create("2015-02-03", 1, "22:00", "11:04", Event.Type.WAKE_UP.name());
        final Optional<DateTime> optionalAfternoon1 = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedbackAfternoonValid1, 0);
        assertThat(optionalAfternoon1.isPresent(), is(false));

        final TimelineFeedback feedbackAfternoonValid2 = TimelineFeedback.create("2015-02-03", 1, "22:00", "08:01", Event.Type.WAKE_UP.name());
        final Optional<DateTime> optionalAfternoon2 = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedbackAfternoonValid2, 0);
        assertThat(optionalAfternoon2.isPresent(), is(false));
    }

    @Test
    public void testConvertFeedbackToDateTimeInvalidSleep() {
        final TimelineFeedback feedback = TimelineFeedback.create("2015-02-03", "22:00", "17:45", Event.Type.SLEEP.name());
        final Optional<DateTime> optional = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback, 0);
        assertThat(optional.isPresent(), is(false));

        final TimelineFeedback feedback2 = TimelineFeedback.create("2015-02-03", "22:00", "18:45", Event.Type.SLEEP.name());
        final Optional<DateTime> optional2 = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback2, 0);
        assertThat(optional2.isPresent(), is(true));

        final TimelineFeedback feedback3 = TimelineFeedback.create("2015-02-03",2, "22:00", "15:45", Event.Type.SLEEP.name());
        final Optional<DateTime> optional3 = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback3, 0);
        assertThat(optional3.isPresent(), is(true));

        final TimelineFeedback feedback4 = TimelineFeedback.create("2015-02-03", "22:00", "16:45", Event.Type.SLEEP.name());
        final Optional<DateTime> optional4 = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedback4, 0);
        assertThat(optional4.isPresent(), is(false));

        final TimelineFeedback feedbackMorning = TimelineFeedback.create("2015-02-03", 0, "07:00", "1:04", Event.Type.SLEEP.name());
        final Optional<DateTime> optionalMorning = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedbackMorning, 0);
        assertThat(optionalMorning.isPresent(), is(false));

        final TimelineFeedback feedbackMorning2 = TimelineFeedback.create("2015-02-03", 0, "07:00", "02:04", Event.Type.SLEEP.name());
        final Optional<DateTime> optionalMorning2 = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedbackMorning2, 0);
        assertThat(optionalMorning2.isPresent(), is(true));

        final TimelineFeedback feedbackMorning3 = TimelineFeedback.create("2015-02-03", 0, "07:00", "23:59", Event.Type.SLEEP.name());
        final Optional<DateTime> optionalMorning3 = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedbackMorning3, 0);
        assertThat(optionalMorning3.isPresent(), is(true));

        final TimelineFeedback feedbackAfternoonValid = TimelineFeedback.create("2015-02-03", 1, "13:00", "10:00", Event.Type.SLEEP.name());
        final Optional<DateTime> optionalAfternoon = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedbackAfternoonValid, 0);
        assertThat(optionalAfternoon.isPresent(), is(true));

        final TimelineFeedback feedbackAfternoonValid1 = TimelineFeedback.create("2015-02-03", 1, "13:00", "09:59", Event.Type.SLEEP.name());
        final Optional<DateTime> optionalAfternoon1 = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedbackAfternoonValid1, 0);
        assertThat(optionalAfternoon1.isPresent(), is(false));

        final TimelineFeedback feedbackAfternoonValid2 = TimelineFeedback.create("2015-02-03", 1, "13:00", "8:01", Event.Type.SLEEP.name());
        final Optional<DateTime> optionalAfternoon2 = FeedbackUtils.convertFeedbackToDateTimeByNewTime(feedbackAfternoonValid2, 0);
        assertThat(optionalAfternoon2.isPresent(), is(false));
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

    @Test
    public void testInsertConversion() {
        //delta of -7 hours
        final TimelineFeedback feedback1 = TimelineFeedback.create("2015-02-03", "23:00", "16:00", Event.Type.SLEEP.name());

        //delta of -2 min
        final TimelineFeedback feedback2 = TimelineFeedback.create("2015-02-03", "00:00", "23:58", Event.Type.SLEEP.name());

        //delta of +3 min
        final TimelineFeedback feedback3 = TimelineFeedback.create("2015-02-03", "23:59", "00:02", Event.Type.SLEEP.name());

        final int delta1 = feedback1.getDeltaInMinutes();
        final int delta2 = feedback2.getDeltaInMinutes();
        final int delta3 = feedback3.getDeltaInMinutes();

        TestCase.assertEquals(delta1, DateTimeConstants.MINUTES_PER_HOUR * -7);
        TestCase.assertEquals(delta2,  -2);
        TestCase.assertEquals(delta3,  +3);


    }

    @Test
    public void testReallyBadFeedback() {
        final List<Event> events = new ArrayList<>();

        final long t1 = 1429134060000L; //Wed, 15 Apr 2015 21:41:00 GMT
        final long t2 = t1 + 60000L;
        final int offset = 7200000; // + 1 hour

        final List<TimeZoneHistory> timeZoneHistoryList = new ArrayList<>();
        final TimeZoneHistory timeZoneHistory = new TimeZoneHistory(1329134060000L,7200000, "Europe/Berlin" );
        timeZoneHistoryList.add(timeZoneHistory);
        final TimeZoneOffsetMap timeZoneOffsetMap = TimeZoneOffsetMap.createFromTimezoneHistoryList(timeZoneHistoryList);

        events.add(Event.createFromType(Event.Type.IN_BED,t1 - DateTimeConstants.MILLIS_PER_HOUR,t2- DateTimeConstants.MILLIS_PER_HOUR,offset,Optional.of("IN_BEDszses"),Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent()));
        events.add(Event.createFromType(Event.Type.SLEEP,t1,t2,offset,Optional.of("SLEEPS"),Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent()));
        events.add(Event.createFromType(Event.Type.WAKE_UP,t1 + DateTimeConstants.MILLIS_PER_HOUR*8,t2 + DateTimeConstants.MILLIS_PER_HOUR*8,offset,Optional.of("WAKESZ"),Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent()));
        events.add(Event.createFromType(Event.Type.OUT_OF_BED,t1+ DateTimeConstants.MILLIS_PER_HOUR*9,+ DateTimeConstants.MILLIS_PER_HOUR*9,offset,Optional.of("OUTSZ"),Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent()));

        /*

            @JsonProperty("date_of_night") final String dateOfNight,
            @JsonProperty("old_time_of_event") final String oldTimeOfEvent,
            @JsonProperty("new_time_of_event") final String newTimeOfEvent,
            @JsonProperty("event_type") final String eventTypeString) {


         */
        final TimelineFeedback feedback = TimelineFeedback.create("2015-04-15","23:00","13:00",Event.Type.IN_BED.name());

        final List<TimelineFeedback> timelineFeedbacks = new ArrayList<>();
        timelineFeedbacks.add(feedback);

        final FeedbackUtils utils = new FeedbackUtils();
        final FeedbackUtils.ReprocessedEvents newEvents = utils.reprocessEventsBasedOnFeedback(SleepPeriod.Period.NIGHT, ImmutableList.copyOf(timelineFeedbacks),ImmutableList.copyOf(events), ImmutableList.copyOf(Collections.EMPTY_LIST), timeZoneOffsetMap);


        TestCase.assertTrue(newEvents.mainEvents.size() == 4);

        final long refTime = 1429185600000L - DateTimeConstants.MILLIS_PER_HOUR;
        TestCase.assertTrue(newEvents.mainEvents.get(Event.Type.IN_BED).getStartTimestamp() == refTime);
        TestCase.assertTrue(newEvents.mainEvents.get(Event.Type.SLEEP).getStartTimestamp() == refTime + 1 * DateTimeConstants.MILLIS_PER_MINUTE);
        TestCase.assertTrue(newEvents.mainEvents.get(Event.Type.WAKE_UP).getStartTimestamp() == refTime + 2 * DateTimeConstants.MILLIS_PER_MINUTE);
        TestCase.assertTrue(newEvents.mainEvents.get(Event.Type.OUT_OF_BED).getStartTimestamp() == refTime+ 3 * DateTimeConstants.MILLIS_PER_MINUTE);



    }

    @Test
    public void testDayLightSavingsFeedback(){
        final FeedbackUtils utils = new FeedbackUtils();

        final List<TimeZoneHistory> timeZoneHistoryList = new ArrayList<>();
        final TimeZoneHistory timeZoneHistory1 = new TimeZoneHistory(1448110860000L,-2520000 ,"America/Los_Angeles");
        final TimeZoneHistory timeZoneHistory2 = new TimeZoneHistory(1468410860000L,-2520000 ,"America/Los_Angeles");

        timeZoneHistoryList.add(timeZoneHistory1); timeZoneHistoryList.add(timeZoneHistory2);

        final TimeZoneOffsetMap timeZoneOffsetMap = TimeZoneOffsetMap.createFromTimezoneHistoryList(timeZoneHistoryList);//Night of Nov 5


        final TimelineFeedback feedback1 = TimelineFeedback.create("2016-11-05","22:41","22:41",Event.Type.IN_BED.name());
        final TimelineFeedback feedback2 = TimelineFeedback.create("2016-11-05","22:42","22:42",Event.Type.SLEEP.name());
        final TimelineFeedback feedback3 = TimelineFeedback.create("2016-11-06","04:41","04:41",Event.Type.OUT_OF_BED.name());
        final Event inBed = Event.createFromType(Event.Type.IN_BED,1478410860000L,1478410920000L,timeZoneOffsetMap.getOffsetWithDefaultAsZero(1478410860000L),Optional.of("IN_BED"),Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent());
        final Event sleep = Event.createFromType(Event.Type.SLEEP,1478410920000L,1478410980000L,timeZoneOffsetMap.getOffsetWithDefaultAsZero(1478410920000L),Optional.of("SLEEP"),Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent());
        final Event wake = Event.createFromType(Event.Type.WAKE_UP,1478436060000L,1478436060000L,timeZoneOffsetMap.getOffsetWithDefaultAsZero(1478436060000L),Optional.of("WAKE_UP"),Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent());

        final List<Event> events = new ArrayList<>();
        events.add(inBed); events.add(sleep); events.add(wake);

        final List<TimelineFeedback> timelineFeedbacks = new ArrayList<>();
        timelineFeedbacks.add(feedback1); timelineFeedbacks.add(feedback2); timelineFeedbacks.add(feedback3);

        final FeedbackUtils.ReprocessedEvents newEvents = utils.reprocessEventsBasedOnFeedback(SleepPeriod.Period.NIGHT, ImmutableList.copyOf(timelineFeedbacks),ImmutableList.copyOf(events), ImmutableList.copyOf(Collections.EMPTY_LIST), timeZoneOffsetMap);

        TestCase.assertTrue(newEvents.mainEvents.get(Event.Type.IN_BED).getStartTimestamp() == inBed.getStartTimestamp());
        TestCase.assertTrue(newEvents.mainEvents.get(Event.Type.IN_BED).getTimezoneOffset() == inBed.getTimezoneOffset());
        TestCase.assertTrue(newEvents.mainEvents.get(Event.Type.WAKE_UP).getStartTimestamp() == wake.getStartTimestamp());
        TestCase.assertTrue(newEvents.mainEvents.get(Event.Type.WAKE_UP).getTimezoneOffset() == wake.getTimezoneOffset());

    }

    //todo
    @Test
    public void testDifferentSleepPeriods(){

        final List<Event> events = new ArrayList<>();

        final long t1 = 1429134060000L; //Wed, 15 Apr 2015 21:41:00 GMT
        final long t2 = t1 + 60000L;
        final long t3 = 1429155660000L;
        final long t4 = t3 + 60000L;  //Wed, 16 Apr 2015 03:41:00 GMT
        final int offset = 7200000; // + 1 hour
        final long expectedTime =t3 + 5 * 60000L;

        final List<TimeZoneHistory> timeZoneHistoryList = new ArrayList<>();
        final TimeZoneHistory timeZoneHistory = new TimeZoneHistory(1329134060000L,7200000, "Europe/Berlin" );
        timeZoneHistoryList.add(timeZoneHistory);
        final TimeZoneOffsetMap timeZoneOffsetMap = TimeZoneOffsetMap.createFromTimezoneHistoryList(timeZoneHistoryList);
        events.add(Event.createFromType(Event.Type.SLEEP,t1,t2,offset,Optional.of("FOOBARS"),Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent()));
        events.add(Event.createFromType(Event.Type.WAKE_UP,t3,t4,offset,Optional.of("FOOBARS2"),Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent()));

        /*

            @JsonProperty("date_of_night") final String dateOfNight,
            @JsonProperty("old_time_of_event") final String oldTimeOfEvent,
            @JsonProperty("new_time_of_event") final String newTimeOfEvent,
            @JsonProperty("event_type") final String eventTypeString) {


         */
        final TimelineFeedback feedback = TimelineFeedback.create("2015-04-15",1,"23:41","23:45",Event.Type.SLEEP.name());
        final TimelineFeedback feedback2 = TimelineFeedback.create("2015-04-15",2,"05:41","05:46",Event.Type.WAKE_UP.name());


        final List<TimelineFeedback> timelineFeedbacks = new ArrayList<>();
        timelineFeedbacks.add(feedback);
        timelineFeedbacks.add(feedback2);

        final FeedbackUtils utils = new FeedbackUtils();
        final FeedbackUtils.ReprocessedEvents newEvents = utils.reprocessEventsBasedOnFeedback(SleepPeriod.Period.NIGHT, ImmutableList.copyOf(timelineFeedbacks),ImmutableList.copyOf(events), ImmutableList.copyOf(Collections.EMPTY_LIST), timeZoneOffsetMap);


        TestCase.assertTrue(newEvents.mainEvents.size() == 2);

        for (final Event event : newEvents.mainEvents.values()) {
            if (event.getType().equals(Event.Type.SLEEP)) {
                final long mysleeptime = event.getStartTimestamp();
                TestCase.assertEquals(mysleeptime, mysleeptime);
            }
            if (event.getType().equals(Event.Type.WAKE_UP)) {
                final long mysleeptime = event.getStartTimestamp();
                TestCase.assertEquals(mysleeptime, expectedTime);
            }
        }
    }


}
