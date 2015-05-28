package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.TimelineFeedback;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FeedbackUtilsTest {

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

        final  ImmutableList<Event>  newEvents = FeedbackUtils.reprocessEventsBasedOnFeedback(ImmutableList.copyOf(timelineFeedbacks), ImmutableList.copyOf(events), offset);


        final long mysleeptime = newEvents.get(0).getStartTimestamp();

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

        final  ImmutableList<Event>  newEvents = FeedbackUtils.reprocessEventsBasedOnFeedback(ImmutableList.copyOf(timelineFeedbacks), ImmutableList.copyOf(events), offset);


        final long mysleeptime = newEvents.get(0).getStartTimestamp();

        TestCase.assertEquals(expectedTime, mysleeptime);

    }

    @Test
    public void TestGettingTheClosestFeedback() {

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
        final TimelineFeedback feedback1 = TimelineFeedback.create("2015-04-15","22:39","22:49",Event.Type.SLEEP.name()); //bad
        final TimelineFeedback feedback2 = TimelineFeedback.create("2015-04-15","22:40","22:45",Event.Type.SLEEP.name()); //good
        final TimelineFeedback feedback3 = TimelineFeedback.create("2015-04-15","22:43","22:49",Event.Type.SLEEP.name()); //bad

        final List<TimelineFeedback> timelineFeedbacks = new ArrayList<>();
        timelineFeedbacks.add(feedback1);
        timelineFeedbacks.add(feedback2);
        timelineFeedbacks.add(feedback3);

        final  ImmutableList<Event>  newEvents = FeedbackUtils.reprocessEventsBasedOnFeedback(ImmutableList.copyOf(timelineFeedbacks), ImmutableList.copyOf(events), offset);


        final long mysleeptime = newEvents.get(0).getStartTimestamp();

        TestCase.assertEquals(expectedTime, mysleeptime);

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
