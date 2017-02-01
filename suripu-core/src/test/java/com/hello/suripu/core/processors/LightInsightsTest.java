package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.LightMsgEN;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.core.processors.insights.Lights;
import com.hello.suripu.core.sense.data.SenseOneFiveExtraData;
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
        final int light = 2; //Note this is in lux values, NOT raw light values because 1.0 lux gets converted in DeviceDataDAODynamoDB

        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offsetMillis = -28800000;
        final List<DeviceData> data = new ArrayList<>();
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp, offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light + 1,light + 1, 0, 0, timestamp.withMinuteOfHour(10), offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light - 1,light - 1, 0, 0, timestamp.withMinuteOfHour(30), offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp.withMinuteOfHour(45), offsetMillis, 1, 1, 1, 0, 0, 0, 0));

        final Optional<InsightCard> insightCardOptional = Lights.processLightData(accountId, data, Optional.of(Device.Color.WHITE), Optional.absent(), new LightData());
        if (insightCardOptional.isPresent()) {
            final String expectedTitle = LightMsgEN.getLightDark(0,0).title;
            assertThat(insightCardOptional.get().title, is(expectedTitle));
        }
    }

    @Test
    public void testLightGood_2() {
        final Long accountId = 984L;
        final Long deviceId = 1L;
        final int light = 2;

        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offsetMillis = -28800000;
        final List<DeviceData> data = new ArrayList<>();
        data.add(DeviceData.senseOneFive(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, timestamp, offsetMillis, 0, 0, 0, 0, 0, 0, 0), SenseOneFiveExtraData.create(0, 0, 0, "fakergb", 0, 0, light, 0)));
        data.add(DeviceData.senseOneFive(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, timestamp.withMinuteOfHour(10), offsetMillis, 1, 1, 1, 0, 0, 0, 0), SenseOneFiveExtraData.create(0, 0, 0, "fakergb", 0, 0, light + 1, 0)));
        data.add(DeviceData.senseOneFive(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, timestamp.withMinuteOfHour(30), offsetMillis, 1, 1, 1, 0, 0, 0, 0), SenseOneFiveExtraData.create(0, 0, 0, "fakergb", 0, 0, light + 1, 0)));
        data.add(DeviceData.senseOneFive(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, timestamp.withMinuteOfHour(45), offsetMillis, 1, 1, 1, 0, 0, 0, 0), SenseOneFiveExtraData.create(0, 0, 0, "fakergb", 0, 0, 0, 0)));

        final Optional<InsightCard> insightCardOptional = Lights.processLightData(accountId, data, Optional.of(Device.Color.WHITE), Optional.absent(), new LightData());
        if (insightCardOptional.isPresent()) {
            final String expectedTitle = LightMsgEN.getLightDark(0,0).title;
            assertThat(insightCardOptional.get().title, is(expectedTitle));
        }
    }

    @Test
    public void testLightNotDarkEnough() {
        final Long accountId = 984L;
        final Long deviceId = 1L;
        final int light = 5;

        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offsetMillis = -28800000;
        final List<DeviceData> data = new ArrayList<>();
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp, offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light + 1,light + 1, 0, 0, timestamp.withMinuteOfHour(10), offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light - 1,light - 1, 0, 0, timestamp.withMinuteOfHour(30), offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp.withMinuteOfHour(45), offsetMillis, 1, 1, 1, 0, 0, 0, 0));

        final Optional<InsightCard> insightCardOptional = Lights.processLightData(accountId, data, Optional.of(Device.Color.WHITE), Optional.absent(), new LightData());
        if (insightCardOptional.isPresent()) {
            final String expectedTitle = LightMsgEN.getLightNotDarkEnough(0, 0).title;
            assertThat(insightCardOptional.get().title, is(expectedTitle));
        }
    }

    @Test
    public void testLightALittleBright() {
        final Long accountId = 984L;
        final Long deviceId = 1L;
        final int light = 20;

        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offsetMillis = -28800000;
        final List<DeviceData> data = new ArrayList<>();
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp, offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light + 1,light + 1, 0, 0, timestamp.withMinuteOfHour(10), offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light - 1,light - 1, 0, 0, timestamp.withMinuteOfHour(30), offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp.withMinuteOfHour(45), offsetMillis, 1, 1, 1, 0, 0, 0, 0));

        final Optional<InsightCard> insightCardOptional = Lights.processLightData(accountId, data, Optional.of(Device.Color.WHITE), Optional.absent(), new LightData());
        if (insightCardOptional.isPresent()) {
            final String expectedTitle = LightMsgEN.getLightALittleBright(0, 0).title;
            assertThat(insightCardOptional.get().title, is(expectedTitle));
        }
    }

    @Test
    public void testLightQuiteBright() {
        final Long accountId = 984L;
        final Long deviceId = 1L;
        final int light = 40;

        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offsetMillis = -28800000;
        final List<DeviceData> data = new ArrayList<>();
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp, offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light + 1,light + 1, 0, 0, timestamp.withMinuteOfHour(10), offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light - 1,light - 1, 0, 0, timestamp.withMinuteOfHour(30), offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp.withMinuteOfHour(45), offsetMillis, 1, 1, 1, 0, 0, 0, 0));

        final Optional<InsightCard> insightCardOptional = Lights.processLightData(accountId, data, Optional.of(Device.Color.WHITE), Optional.absent(), new LightData());
        if (insightCardOptional.isPresent()) {
            final String expectedTitle = LightMsgEN.getLightQuiteBright(0, 0).title;
            assertThat(insightCardOptional.get().title, is(expectedTitle));
        }
    }

    @Test
    public void testLightBright() {
        final Long accountId = 984L;
        final Long deviceId = 1L;
        final int light = 100;

        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offsetMillis = -28800000;
        final List<DeviceData> data = new ArrayList<>();
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp, offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light + 1,light + 1, 0, 0, timestamp.withMinuteOfHour(10), offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light - 1,light - 1, 0, 0, timestamp.withMinuteOfHour(30), offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp.withMinuteOfHour(45), offsetMillis, 1, 1, 1, 0, 0, 0, 0));


        final Optional<InsightCard> insightCardOptional = Lights.processLightData(accountId, data, Optional.of(Device.Color.WHITE), Optional.absent(), new LightData());
        if (insightCardOptional.isPresent()) {
            final String expectedTitle = LightMsgEN.getLightTooBright(0,0).title;
            assertThat(insightCardOptional.get().title, is(expectedTitle));
        }
    }

    @Test
    public void testLightWayTooBright() {
        final Long accountId = 984L;
        final Long deviceId = 1L;
        final int light = 101;

        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offsetMillis = -28800000;
        final List<DeviceData> data = new ArrayList<>();
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp, offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light + 1,light + 1, 0, 0, timestamp.withMinuteOfHour(10), offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light - 1,light - 1, 0, 0, timestamp.withMinuteOfHour(30), offsetMillis, 1, 1, 1, 0, 0, 0, 0));
        data.add(DeviceData.senseOne(accountId, deviceId, "", 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp.withMinuteOfHour(45), offsetMillis, 1, 1, 1, 0, 0, 0, 0));

        final Optional<InsightCard> insightCardOptional = Lights.processLightData(accountId, data, Optional.of(Device.Color.WHITE), Optional.absent(), new LightData());
        if (insightCardOptional.isPresent()) {
            final String expectedTitle = LightMsgEN.getLightWayTooBright(0,0).title;
            assertThat(insightCardOptional.get().title, is(expectedTitle));
        }
    }

}
