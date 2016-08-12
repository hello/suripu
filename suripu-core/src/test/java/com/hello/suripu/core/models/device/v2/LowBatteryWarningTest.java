package com.hello.suripu.core.models.device.v2;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.analytics.AnalyticsTracker;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.pill.heartbeat.PillHeartBeat;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;

public class LowBatteryWarningTest {

    @Test
    public void lowBatteryFiltering() {
        final List<DeviceAccountPair> accountPairs = Lists.newArrayList(
                new DeviceAccountPair(1L, 1L, "pill1", DateTime.now(DateTimeZone.UTC)),
                new DeviceAccountPair(1L, 2L, "pill2", DateTime.now(DateTimeZone.UTC))
        );
        final List<PillHeartBeat> heartBeats = Lists.newArrayList(
                PillHeartBeat.create("pill1", 12, 888,123, DateTime.now(DateTimeZone.UTC)),
                PillHeartBeat.create("pill2", 50, 888,123, DateTime.now(DateTimeZone.UTC))
        );

        final List<Pill> pills = Lists.newArrayList(
                Pill.create(accountPairs.get(0), Optional.of(heartBeats.get(0)), Optional.<Pill.Color>absent()),
                Pill.create(accountPairs.get(1), Optional.of(heartBeats.get(1)), Optional.<Pill.Color>absent())
        );

        final Account account = new Account.Builder().withId(1L).build();
        AnalyticsTracker tracker = mock(AnalyticsTracker.class);

        final List<Pill> updated = DeviceProcessor.replaceBatteryWarning(pills, account, tracker);

        assertEquals(pills.get(0).state, Pill.State.LOW_BATTERY);
        assertEquals(pills.get(1).state, Pill.State.NORMAL);

        assertEquals(updated.size(), pills.size());
        for(final Pill pill : updated) {
            assertEquals(pill.state, Pill.State.NORMAL);
        }
    }

}
