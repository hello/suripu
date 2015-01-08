package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.core.processors.insights.Lights;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by kingshy on 1/5/15.
 */
public class LightInsightsTest {

    @Test
    public void testLightGood() {
        final Long accountId = 984L;
        final Long deviceId = 1L;
        final int light = 2;
        final int zeroLight = 0;

        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offsetMillis = -28800000;
        final List<DeviceData> data = new ArrayList<>();
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light, 0, 0, timestamp, offsetMillis, 1, 1, 1));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light + 1, 0, 0, timestamp.withMinuteOfHour(10), offsetMillis, 1, 1, 1));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light + 1, 0, 0, timestamp.withMinuteOfHour(30), offsetMillis, 1, 1, 1));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light, 0, 0, timestamp.withMinuteOfHour(45), offsetMillis, 1, 1, 1));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, zeroLight, 0, 0, timestamp.withHourOfDay(21), offsetMillis, 1, 1, 1));

        final Optional<InsightCard> insightCardOptional = Lights.processLightData(accountId, data, new LightData());
        if (insightCardOptional.isPresent()) {
            final String expectedTitle = "Hello, Dark Room";
            assertThat(insightCardOptional.get().title, is(expectedTitle));
        }
    }

    @Test
    public void testLightBright() {
        final Long accountId = 984L;
        final Long deviceId = 1L;
        final int light = 85;
        final int zeroLight = 0;

        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offsetMillis = -28800000;
        final List<DeviceData> data = new ArrayList<>();
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light, 0, 0, timestamp, offsetMillis, 1, 1, 1));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light + 1, 0, 0, timestamp.withMinuteOfHour(10), offsetMillis, 1, 1, 1));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light + 1, 0, 0, timestamp.withMinuteOfHour(30), offsetMillis, 1, 1, 1));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light, 0, 0, timestamp.withMinuteOfHour(45), offsetMillis, 1, 1, 1));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, zeroLight, 0, 0, timestamp.withHourOfDay(21), offsetMillis, 1, 1, 1));

        final Optional<InsightCard> insightCardOptional = Lights.processLightData(accountId, data, new LightData());
        if (insightCardOptional.isPresent()) {
            final String expectedTitle = "Time to Dim It Down";
            assertThat(insightCardOptional.get().title, is(expectedTitle));
        }
    }

}
