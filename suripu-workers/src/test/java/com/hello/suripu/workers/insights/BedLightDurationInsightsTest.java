package com.hello.suripu.workers.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.BedLightDurationMsgEN;
import com.hello.suripu.core.processors.insights.BedLightDuration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jyfan on 8/20/15.
 */
public class BedLightDurationInsightsTest {

    @Test
    public void testNoCardGenerated() {
        final Long accountId = 999L;
        final List<Integer> lightDurationList = new ArrayList<>();
        final Optional<InsightCard> noResult = BedLightDuration.processLightData(lightDurationList, accountId);
        assertThat(noResult.isPresent(), is(Boolean.FALSE));

        //test size 1 no generation
        final Long deviceId = 999L;

        final int light = 2;
        final int zeroLight = 0;
        final int offsetMillis = -28800000;
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offMinuteThreshold = 45;

        final List<DeviceData> data = new ArrayList<>();
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, zeroLight,zeroLight, 0, 0, timestamp.withHourOfDay(18), offsetMillis, 1, 1, 1, 0, 0, 0));

        final Optional<Integer> calculatedLightDurationList = BedLightDuration.processLightDataOneDay(data, offMinuteThreshold);
        assertThat(calculatedLightDurationList.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void testOneDay() {
        final Long accountId = 999L;
        final Long deviceId = 999L;

        final int light = 2;
        final int zeroLight = 0;
        final int offsetMillis = -28800000;
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offMinuteThreshold = 45;

        final List<DeviceData> data = new ArrayList<>();
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, zeroLight,zeroLight, 0, 0, timestamp.withHourOfDay(18), offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp, offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp.withMinuteOfHour(10), offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp.withMinuteOfHour(30), offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp.withMinuteOfHour(45), offsetMillis, 1, 1, 1, 0, 0, 0));

        final Optional<Integer> calculatedLightDurationList = BedLightDuration.processLightDataOneDay(data, offMinuteThreshold);
        final Optional<Integer> expectedLightDurationList = Optional.of(45);
        assertThat(calculatedLightDurationList, is(expectedLightDurationList));
    }

    @Test
    public void testOneDayThreshold() {
        final Long accountId = 999L;
        final Long deviceId = 999L;

        final int light = 2;
        final int zeroLight = 0;
        final int offsetMillis = -28800000;
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offMinuteThreshold = 45;

        final List<DeviceData> data = new ArrayList<>();
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp, offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp.withMinuteOfHour(50), offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp.withMinuteOfHour(51), offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp.withMinuteOfHour(52), offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, zeroLight,zeroLight, 0, 0, timestamp.withMinuteOfHour(55), offsetMillis, 1, 1, 1, 0, 0, 0));

        final Optional<Integer> calculatedLightDurationList = BedLightDuration.processLightDataOneDay(data, offMinuteThreshold);
        final Optional<Integer> expectedLightDurationList = Optional.of(5);
        assertThat(calculatedLightDurationList, is(expectedLightDurationList));
    }

    @Test
    public void testOneDayThresholdEdge1() {
        final Long accountId = 999L;
        final Long deviceId = 999L;

        final int light = 2;
        final int zeroLight = 0;
        final int offsetMillis = -28800000;
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offMinuteThreshold = 45;

        final List<DeviceData> data = new ArrayList<>();
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp, offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp.withMinuteOfHour(50), offsetMillis, 1, 1, 1, 0, 0, 0));

        final Optional<Integer> calculatedLightDurationList = BedLightDuration.processLightDataOneDay(data, offMinuteThreshold);
        final Optional<Integer> expectedLightDurationList = Optional.of(0);
        assertThat(calculatedLightDurationList, is(expectedLightDurationList));
    }

    @Test
    public void testOneDayDivide() {
        final Long accountId = 999L;
        final Long deviceId = 999L;

        final int light = 2;
        final int zeroLight = 0;
        final int offsetMillis = -28800000;
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offMinuteThreshold = 45;

        final List<DeviceData> data = new ArrayList<>();
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp, offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, zeroLight,zeroLight, 0, 0, timestamp.withMinuteOfHour(10), offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, zeroLight,zeroLight, 0, 0, timestamp.withMinuteOfHour(30), offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, zeroLight,zeroLight, 0, 0, timestamp.withMinuteOfHour(45), offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, zeroLight,zeroLight, 0, 0, timestamp.withHourOfDay(21), offsetMillis, 1, 1, 1, 0, 0, 0));

        final Optional<Integer> calculatedLightDurationList = BedLightDuration.processLightDataOneDay(data, offMinuteThreshold);
        final Optional<Integer> expectedLightDurationList = Optional.of(light/4);
        assertThat(calculatedLightDurationList, is(expectedLightDurationList));
    }

    @Test
    public void testDurationLow() {
        final Long accountId = 999L;

        final List<Integer> lightDurationList = new ArrayList<>();
        lightDurationList.add(0);
        lightDurationList.add(60);
        lightDurationList.add(10);
        final Optional<InsightCard> lowResult = BedLightDuration.processLightData(lightDurationList, accountId);
        assertThat(lowResult.isPresent(), is(Boolean.FALSE));

        //comments below for if we want to generate Insight for low values
        /*
        final String expectedTitle = BedLightDurationMsgEN.getLittleLight().title;
        assertThat(lowResult.get().title, is(expectedTitle));

        final String lowMessage = BedLightDurationMsgEN.getLittleLight().title;
        assertThat(lowMessage, is(lowMessage));
        */
    }

    @Test
    public void testDurationMedium() {
        final Long accountId = 999L;

        final List<Integer> lightDurationList = new ArrayList<>();
        lightDurationList.add(60);
        lightDurationList.add(120);
        lightDurationList.add(120);
        final Optional<InsightCard> mediumResult = BedLightDuration.processLightData(lightDurationList, accountId);
        assertThat(mediumResult.isPresent(), is(Boolean.TRUE));

        final String expectedTitle = BedLightDurationMsgEN.getMediumLight().title;
        assertThat(mediumResult.get().title, is(expectedTitle));

        final String mediumMessage = BedLightDurationMsgEN.getMediumLight().title;
        assertThat(mediumMessage, is(mediumMessage));
    }

    @Test
    public void testDurationHigh() {
        final Long accountId = 999L;

        final List<Integer> lightDurationList = new ArrayList<>();
        lightDurationList.add(120);
        lightDurationList.add(200);
        lightDurationList.add(300);
        final Optional<InsightCard> highResult = BedLightDuration.processLightData(lightDurationList, accountId);
        assertThat(highResult.isPresent(), is(Boolean.TRUE));

        final String expectedTitle = BedLightDurationMsgEN.getHighLight().title;
        assertThat(highResult.get().title, is(expectedTitle));

        final String highMessage = BedLightDurationMsgEN.getHighLight().title;
        assertThat(highMessage, is(highMessage));
    }

}
