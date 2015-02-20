package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.TimelineFeedback;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FeedbackUtilsTest {

    @Test
    public void testConvertFeedbackToDateTimeWakeup() {
        final TimelineFeedback feedback = TimelineFeedback.create("2015-02-03", "07:00", "7:15", Event.Type.WAKE_UP.name());
        final Optional<DateTime> optional = FeedbackUtils.convertFeedbackToDateTime(feedback, 0);
        assertThat(optional.isPresent(), is(true));
        final DateTime dt = optional.get();
        assertThat(dt.getDayOfMonth(), equalTo(4));
        assertThat(dt.getHourOfDay(), equalTo(7));
        assertThat(dt.getMinuteOfHour(), equalTo(15));


        final TimelineFeedback secondFeedback = TimelineFeedback.create("2015-02-03", "07:00", "14:15", Event.Type.WAKE_UP.name());
        final Optional<DateTime> secondOptional = FeedbackUtils.convertFeedbackToDateTime(secondFeedback,0);
        assertThat(secondOptional.isPresent(), is(true));
        final DateTime secondDt = secondOptional.get();
        assertThat(secondDt.getDayOfMonth(), equalTo(4));
        assertThat(secondDt.getHourOfDay(), equalTo(14));
        assertThat(secondDt.getMinuteOfHour(), equalTo(15));
    }

    @Test
    public void testConvertFeedbackToDateTimeInvalidWakeUp() {
        final TimelineFeedback feedback = TimelineFeedback.create("2015-02-03", "07:00", "23:15", Event.Type.WAKE_UP.name());
        final Optional<DateTime> optional = FeedbackUtils.convertFeedbackToDateTime(feedback,0);
        assertThat(optional.isPresent(), is(false));
    }



    @Test
    public void testConvertFeedbackToDateTimeFallasleep() {
        final TimelineFeedback feedback = TimelineFeedback.create("2015-02-03", "23:00", "00:15", Event.Type.SLEEP.name());
        final Optional<DateTime> optional = FeedbackUtils.convertFeedbackToDateTime(feedback,0);
        assertThat(optional.isPresent(), is(true));
        final DateTime dt = optional.get();
        assertThat(dt.getDayOfMonth(), equalTo(4));
        assertThat(dt.getHourOfDay(), equalTo(0));
        assertThat(dt.getMinuteOfHour(), equalTo(15));


        final TimelineFeedback secondFeedback = TimelineFeedback.create("2015-02-03", "00:15", "23:15", Event.Type.SLEEP.name());
        final Optional<DateTime> secondOptional = FeedbackUtils.convertFeedbackToDateTime(secondFeedback,0);
        assertThat(secondOptional.isPresent(), is(true));
        final DateTime secondDt = secondOptional.get();
        assertThat(secondDt.getDayOfMonth(), equalTo(3));
        assertThat(secondDt.getHourOfDay(), equalTo(23));
        assertThat(secondDt.getMinuteOfHour(), equalTo(15));
    }

    @Test
    public void testConvertFeedbackToDateTimeInvalidFallAsleep() {
        final TimelineFeedback feedback = TimelineFeedback.create("2015-02-03", "23:00", "16:00", Event.Type.SLEEP.name());
        final Optional<DateTime> optional = FeedbackUtils.convertFeedbackToDateTime(feedback,0);
        assertThat(optional.isPresent(), is(false));
    }
}
