package com.hello.suripu.core.pill.heartbeat;

import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jakepiccolo on 1/4/16.
 */
public class PillHeartBeatTest {
    @Test
    public void testFromTrackerMotion() {
        final DateTime dateTime = new DateTime(2016, 1, 1, 0, 0, 0, DateTimeZone.UTC);
        final String id = "pillId";
        final TrackerMotion trackerMotion = new TrackerMotion.Builder()
                .withValue(1)
                .withTimestampMillis(dateTime.getMillis())
                .withExternalTrackerId(id)
                .build();
        final PillHeartBeat heartBeat = PillHeartBeat.fromTrackerMotion(trackerMotion);
        assertThat(heartBeat.createdAtUTC, is(dateTime));
        assertThat(heartBeat.pillId, is(id));
        assertThat(heartBeat.batteryLevel, is(100));
    }
}