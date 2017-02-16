package com.hello.suripu.core.models.timeline.v2;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SensorReading;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TimelineEventTest {
    public SleepSegment generateTestSegment(int depth, Event.Type type) {
        DateTime date = DateTime.now(DateTimeZone.UTC);
        return new SleepSegment(
                -1L,
                Collections.<SensorReading>emptyList(),
                depth,
                null,
                date.getMillis(),
                1300,
                date.getZone().getOffset(date),
                type,
                SleepPeriod.Period.NIGHT,
                "Test segment"
        );
    }

    @Test
    public void fromV1() throws Exception {
        final SleepSegment segment = generateTestSegment(50, Event.Type.MOTION);
        final TimelineEvent event = TimelineEvent.fromV1(segment);

        assertThat(event.eventType, is(EventType.GENERIC_MOTION));
        assertThat(event.sleepDepth, is(segment.getSleepDepth()));
        assertThat(event.sleepState, is(SleepState.MEDIUM));
        assertThat(event.duration, is(segment.getDurationInSeconds() * 1000L));
        assertThat(event.timestamp, is(segment.getTimestamp()));
        assertThat(event.timezoneOffset, is(segment.getOffsetMillis()));
        assertThat(event.message, is(segment.getMessage()));
    }

    @Test
    public void sleepWakeConversion() throws Exception {
        final SleepSegment awake = generateTestSegment(50, Event.Type.NONE);
        final TimelineEvent awakeEvent = TimelineEvent.fromV1(awake);
        assertThat(awakeEvent.eventType, is(EventType.IN_BED));
        assertThat(awakeEvent.sleepState, is(SleepState.AWAKE));
        assertThat(awakeEvent.sleepDepth, is(50));


        final SleepSegment sleeping = generateTestSegment(50, Event.Type.SLEEPING);
        final TimelineEvent sleepingEvent = TimelineEvent.fromV1(sleeping);
        assertThat(sleepingEvent.eventType, is(EventType.IN_BED));
        assertThat(sleepingEvent.sleepState, is(SleepState.MEDIUM));
        assertThat(sleepingEvent.sleepDepth, is(50));
    }

    @Test
    public void eventTypeMapping() throws Exception {
        assertThat(TimelineEvent.from(Event.Type.SLEEPING), is(EventType.IN_BED));
        assertThat(TimelineEvent.from(Event.Type.IN_BED), is(EventType.GOT_IN_BED));
        assertThat(TimelineEvent.from(Event.Type.OUT_OF_BED), is(EventType.GOT_OUT_OF_BED));
        assertThat(TimelineEvent.from(Event.Type.SLEEP), is(EventType.FELL_ASLEEP));
        assertThat(TimelineEvent.from(Event.Type.WAKE_UP), is(EventType.WOKE_UP));
        assertThat(TimelineEvent.from(Event.Type.MOTION), is(EventType.GENERIC_MOTION));
        assertThat(TimelineEvent.from(Event.Type.PARTNER_MOTION), is(EventType.PARTNER_MOTION));
        assertThat(TimelineEvent.from(Event.Type.NOISE), is(EventType.GENERIC_SOUND));
        assertThat(TimelineEvent.from(Event.Type.ALARM), is(EventType.ALARM_RANG));
        assertThat(TimelineEvent.from(Event.Type.SNORING), is(EventType.SNORED));
        assertThat(TimelineEvent.from(Event.Type.SLEEP_TALK), is(EventType.SLEEP_TALKED));
        assertThat(TimelineEvent.from(Event.Type.LIGHT), is(EventType.LIGHT));
        assertThat(TimelineEvent.from(Event.Type.LIGHTS_OUT), is(EventType.LIGHTS_OUT));
        assertThat(TimelineEvent.from(Event.Type.SUNSET), is(EventType.SUNSET));
        assertThat(TimelineEvent.from(Event.Type.SUNRISE), is(EventType.SUNRISE));
    }
}
