package com.hello.suripu.workers.insights;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.BedLightDurationMsgEN;
import com.hello.suripu.core.processors.insights.BedLightDuration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
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
        final List<Integer> lightDurationList = Lists.newArrayList();

        final int durationAverage = BedLightDuration.computeAverage(lightDurationList);

        final Optional<InsightCard> noResult = BedLightDuration.scoreCardBedLightDuration(durationAverage, accountId);
        assertThat(noResult.isPresent(), is(Boolean.FALSE));

        //test size 1 no generation
        final int zeroLight = 0;
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offMinuteThreshold = 45;

        final List<DeviceData> data = Lists.newArrayList();
        data.add(createNewDeviceData(zeroLight, timestamp.withHourOfDay(18)));

        final Integer calculatedLightDurationList = BedLightDuration.findLightOnDurationForDay(data, offMinuteThreshold);
        assertThat(calculatedLightDurationList, is(0));
    }

    @Test
    public void testOneDay() {
        final int light = 2;
        final int zeroLight = 0;
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offMinuteThreshold = 45;

        final List<DeviceData> data = Lists.newArrayList();
        data.add(createNewDeviceData(zeroLight, timestamp.withHourOfDay(18)));
        data.add(createNewDeviceData(light, timestamp));
        data.add(createNewDeviceData(light, timestamp.withMinuteOfHour(10)));
        data.add(createNewDeviceData(light, timestamp.withMinuteOfHour(30)));
        data.add(createNewDeviceData(light, timestamp.withMinuteOfHour(45)));

        final Integer calculatedLightDurationList = BedLightDuration.findLightOnDurationForDay(data, offMinuteThreshold);
        final Integer expectedLightDurationList = 45;
        assertThat(calculatedLightDurationList, is(expectedLightDurationList));
    }

    @Test
    public void testOneDayThreshold() {
        final int light = 2;
        final int zeroLight = 0;
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offMinuteThreshold = 45;

        final List<DeviceData> data = Lists.newArrayList();
        data.add(createNewDeviceData(light, timestamp));
        data.add(createNewDeviceData(light, timestamp.withMinuteOfHour(50)));
        data.add(createNewDeviceData(light, timestamp.withMinuteOfHour(51)));
        data.add(createNewDeviceData(light, timestamp.withMinuteOfHour(52)));
        data.add(createNewDeviceData(zeroLight, timestamp.withMinuteOfHour(55)));

        final Integer calculatedLightDurationList = BedLightDuration.findLightOnDurationForDay(data, offMinuteThreshold);
        final Integer expectedLightDurationList = 5;
        assertThat(calculatedLightDurationList, is(expectedLightDurationList));
    }

    @Test
    public void testOneDayThresholdEdge1() {
        final int light = 2;
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offMinuteThreshold = 45;

        final List<DeviceData> data = Lists.newArrayList();
        data.add(createNewDeviceData(light, timestamp));
        data.add(createNewDeviceData(light, timestamp.withMinuteOfHour(50)));

        final Integer calculatedLightDurationList = BedLightDuration.findLightOnDurationForDay(data, offMinuteThreshold);
        final Integer expectedLightDurationList = 0;
        assertThat(calculatedLightDurationList, is(expectedLightDurationList));
    }

    @Test
    public void testOneDayDivide() {
        final int light = 2;
        final int zeroLight = 0;
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offMinuteThreshold = 45;

        final List<DeviceData> data = Lists.newArrayList();
        data.add(createNewDeviceData(light, timestamp));
        data.add(createNewDeviceData(zeroLight, timestamp.withMinuteOfHour(10)));
        data.add(createNewDeviceData(zeroLight, timestamp.withMinuteOfHour(30)));
        data.add(createNewDeviceData(zeroLight, timestamp.withMinuteOfHour(45)));
        data.add(createNewDeviceData(zeroLight, timestamp.withHourOfDay(21)));

        final Integer calculatedLightDurationList = BedLightDuration.findLightOnDurationForDay(data, offMinuteThreshold);
        final Integer expectedLightDurationList = light/4;
        assertThat(calculatedLightDurationList, is(expectedLightDurationList));
    }

    @Test
    public void testDurationLow() {
        final Long accountId = 999L;

        final List<Integer> lightDurationList = Lists.newArrayList();
        lightDurationList.add(0);
        lightDurationList.add(60);
        lightDurationList.add(10);

        final int durationAverage = BedLightDuration.computeAverage(lightDurationList);
        final Optional<InsightCard> lowResult = BedLightDuration.scoreCardBedLightDuration(durationAverage, accountId);
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

        final List<Integer> lightDurationList = Lists.newArrayList();
        lightDurationList.add(60);
        lightDurationList.add(120);
        lightDurationList.add(120);
        final int durationAverage = BedLightDuration.computeAverage(lightDurationList);

        final Optional<InsightCard> mediumResult = BedLightDuration.scoreCardBedLightDuration(durationAverage, accountId);
        assertThat(mediumResult.isPresent(), is(Boolean.TRUE));

        final String expectedTitle = BedLightDurationMsgEN.getMediumLight().title;
        assertThat(mediumResult.get().title, is(expectedTitle));

        final String mediumMessage = BedLightDurationMsgEN.getMediumLight().title;
        assertThat(mediumMessage, is(mediumMessage));
    }

    @Test
    public void testDurationHigh() {
        final Long accountId = 999L;

        final List<Integer> lightDurationList = Lists.newArrayList();
        lightDurationList.add(120);
        lightDurationList.add(200);
        lightDurationList.add(300);
        final int durationAverage = BedLightDuration.computeAverage(lightDurationList);

        final Optional<InsightCard> highResult = BedLightDuration.scoreCardBedLightDuration(durationAverage, accountId);
        assertThat(highResult.isPresent(), is(Boolean.TRUE));

        final String expectedTitle = BedLightDurationMsgEN.getHighLight().title;
        assertThat(highResult.get().title, is(expectedTitle));

        final String highMessage = BedLightDurationMsgEN.getHighLight().title;
        assertThat(highMessage, is(highMessage));
    }

    @Test
    public void testSameDay() {
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(21).withMinuteOfHour(0);

        final List<DeviceData> data = Lists.newArrayList();
        data.add(createNewDeviceData(0, timestamp.withHourOfDay(21)));
        data.add(createNewDeviceData(0, timestamp.withHourOfDay(23)));

        assertThat(BedLightDuration.sameDay(data.get(1), data.get(0)), is(Boolean.TRUE));
    }

    @Test
    public void testSplitDeviceDataByDay() {
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(21).withMinuteOfHour(0);

        final List<DeviceData> data = Lists.newArrayList();
        data.add(createNewDeviceData(0, timestamp.withHourOfDay(21)));
        data.add(createNewDeviceData(0, timestamp.withHourOfDay(23)));
        data.add(createNewDeviceData(0, timestamp.plusDays(1).withHourOfDay(4)));

        data.add(createNewDeviceData(1, timestamp.plusDays(1).withHourOfDay(21)));
        data.add(createNewDeviceData(1, timestamp.plusDays(2).withHourOfDay(3)));
        data.add(createNewDeviceData(1, timestamp.plusDays(2).withHourOfDay(4)));

        data.add(createNewDeviceData(2, timestamp.plusDays(2).withHourOfDay(21)));

        data.add(createNewDeviceData(3, timestamp.plusDays(4).withHourOfDay(1)));
        data.add(createNewDeviceData(3, timestamp.plusDays(4).withHourOfDay(2)));

        final List<List<DeviceData>> deviceDataByDay = BedLightDuration.splitDeviceDataByDay(data);
        assertThat(deviceDataByDay.size(), is(4));

        final Integer day1Duration = BedLightDuration.findLightOnDurationForDay(deviceDataByDay.get(0), 420);
        final Integer day2Duration = BedLightDuration.findLightOnDurationForDay(deviceDataByDay.get(1), 420);
        final Integer day3Duration = BedLightDuration.findLightOnDurationForDay(deviceDataByDay.get(2), 420);
        final Integer day4Duration = BedLightDuration.findLightOnDurationForDay(deviceDataByDay.get(3), 420);

        assertThat(day1Duration, is(7*60));
        assertThat(day2Duration, is(7*60));
        assertThat(day3Duration, is(0*60));
        assertThat(day4Duration, is(1*60));
    }

    @Test
    public void testSplitDeviceDataByDayWithThreshold() {
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(21).withMinuteOfHour(0);

//        final List<DeviceData> data = Lists.newArrayList();
        final List<DeviceData> data = Lists.newArrayList(
                createNewDeviceData(0, timestamp.withHourOfDay(21)),
                createNewDeviceData(0, timestamp.withHourOfDay(21)),
                createNewDeviceData(0, timestamp.withHourOfDay(21).withMinuteOfHour(45)),
                createNewDeviceData(0, timestamp.withHourOfDay(21).withMinuteOfHour(55)),
                createNewDeviceData(0, timestamp.withHourOfDay(22).withMinuteOfHour(15)),

                createNewDeviceData(1, timestamp.plusDays(1).withHourOfDay(21)),
                createNewDeviceData(1, timestamp.plusDays(2).withHourOfDay(3)),
                createNewDeviceData(1, timestamp.plusDays(2).withHourOfDay(4)),

                createNewDeviceData(2, timestamp.plusDays(2).withHourOfDay(21)),

                createNewDeviceData(3, timestamp.plusDays(4).withHourOfDay(1)),
                createNewDeviceData(3, timestamp.plusDays(4).withHourOfDay(2))
        );
        final List<List<DeviceData>> deviceDataByDay = BedLightDuration.splitDeviceDataByDay(data);
        assertThat(deviceDataByDay.size(), is(4));

        final Integer day1Duration = BedLightDuration.findLightOnDurationForDay(deviceDataByDay.get(0), BedLightDuration.OFF_MINUTES_THRESHOLD);
        final Integer day2Duration = BedLightDuration.findLightOnDurationForDay(deviceDataByDay.get(1), BedLightDuration.OFF_MINUTES_THRESHOLD);
        final Integer day3Duration = BedLightDuration.findLightOnDurationForDay(deviceDataByDay.get(2), BedLightDuration.OFF_MINUTES_THRESHOLD);
        final Integer day4Duration = BedLightDuration.findLightOnDurationForDay(deviceDataByDay.get(3), BedLightDuration.OFF_MINUTES_THRESHOLD);

        assertThat(day1Duration, is(30));
        assertThat(day2Duration, is(0*60));
        assertThat(day3Duration, is(0*60));
        assertThat(day4Duration, is(0*60));
    }

    @Test
    public void testSomeUser() {
//        Data pulled from 8-01-2015 from accountId 20894 - python script returns 25.2 minutes

        List<Integer> lightData = Lists.newArrayList();
        final URL userCSVFile = Resources.getResource("insights/test/bedlightduration/lightDataUser1.csv");
        try {
            final String csvString = Resources.toString(userCSVFile, Charsets.UTF_8);
            final String[] lightDataStringList = csvString.replaceAll("\\s+","").split(",");
            for (String lightDataString : lightDataStringList) {
                lightData.add(Integer.parseInt(lightDataString));
            }
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }

        List<Long> timeStamps = Lists.newArrayList();
        final URL timeStampCSVFile = Resources.getResource("insights/test/bedlightduration/timeStampUser1.csv");
        try {
            final String csvString = Resources.toString(timeStampCSVFile, Charsets.UTF_8);
            final String[] timeStampStringList = csvString.replaceAll("\\s+","").replaceAll("L","").split(",");
            for (String timeStampString : timeStampStringList) {
                timeStamps.add(Long.parseLong(timeStampString));
            }
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }

        final List<DeviceData> deviceData = Lists.newArrayList();
        for (int i = 0; i < lightData.size(); i++) {
            final int myHour = new DateTime(timeStamps.get(i), DateTimeZone.forOffsetMillis(-25200000)).getHourOfDay();
            if (myHour > 21 || myHour < 4) {
                deviceData.add(createNewDeviceData(lightData.get(i), new DateTime(timeStamps.get(i))));
            }
        }

        final int avgLightOn = BedLightDuration.getInsightsHelper(deviceData);
        assertThat(avgLightOn, is(26));
//        return BedLightDuration.scoreCardBedLightDuration(avgLightOn, accountId);
    }

    @Test
    public void testSomeUser2() {
//        Data pulled from 8-01-2015 from accountId 22262 - python script returns 0 minutes

        List<Integer> lightData = Lists.newArrayList();
        final URL userCSVFile = Resources.getResource("insights/test/bedlightduration/lightDataUser2.csv");
        try {
            final String csvString = Resources.toString(userCSVFile, Charsets.UTF_8);
            final String[] lightDataStringList = csvString.replaceAll("\\s+","").split(",");
            for (String lightDataString : lightDataStringList) {
                lightData.add(Integer.parseInt(lightDataString));
            }
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }

        List<Long> timeStamps = Lists.newArrayList();
        final URL timeStampCSVFile = Resources.getResource("insights/test/bedlightduration/timeStampUser2.csv");
        try {
            final String csvString = Resources.toString(timeStampCSVFile, Charsets.UTF_8);
            final String[] timeStampStringList = csvString.replaceAll("\\s+","").replaceAll("L","").split(",");
            for (String timeStampString : timeStampStringList) {
                timeStamps.add(Long.parseLong(timeStampString));
            }
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }

        final List<DeviceData> deviceData = Lists.newArrayList();

        for (int i = 0; i < lightData.size(); i++) {
            final int myHour = new DateTime(timeStamps.get(i), DateTimeZone.forOffsetMillis(-25200000)).getHourOfDay();
            if (myHour > 21 || myHour < 4) {
                deviceData.add(createNewDeviceData(lightData.get(i), new DateTime(timeStamps.get(i))));
            }
        }
        
        final int avgLightOn = BedLightDuration.getInsightsHelper(deviceData);
        assertThat(avgLightOn, is(0));
//        return BedLightDuration.scoreCardBedLightDuration(avgLightOn, accountId);
    }

    @Test
    public void testSomeUser3() {
//        Data pulled from 8-01-2015 from accountId 27053 - python script returns 175.6 minutes

        List<Integer> lightData = Lists.newArrayList();
        final URL userCSVFile = Resources.getResource("insights/test/bedlightduration/lightDataUser3.csv");
        try {
            final String csvString = Resources.toString(userCSVFile, Charsets.UTF_8);
            final String[] lightDataStringList = csvString.replaceAll("\\s+","").split(",");
            for (String lightDataString : lightDataStringList) {
                lightData.add(Integer.parseInt(lightDataString));
            }
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }

        List<Long> timeStamps = Lists.newArrayList();
        final URL timeStampCSVFile = Resources.getResource("insights/test/bedlightduration/timeStampUser3.csv");
        try {
            final String csvString = Resources.toString(timeStampCSVFile, Charsets.UTF_8);
            final String[] timeStampStringList = csvString.replaceAll("\\s+","").replaceAll("L","").split(",");
            for (String timeStampString : timeStampStringList) {
                timeStamps.add(Long.parseLong(timeStampString));
            }
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }

        final List<DeviceData> deviceData = Lists.newArrayList();

        for (int i = 0; i < lightData.size(); i++) {
            final int myHour = new DateTime(timeStamps.get(i), DateTimeZone.forOffsetMillis(-25200000)).getHourOfDay();
            if (myHour > 21 || myHour < 4) {
                deviceData.add(createNewDeviceData(lightData.get(i), new DateTime(timeStamps.get(i))));
            }
        }

        final int avgLightOn = BedLightDuration.getInsightsHelper(deviceData);
        assertThat(avgLightOn, is(176));
    }

    private static DeviceData createNewDeviceData(final Integer lightValue, final DateTime timestamp) {
        final Long accountId = 999L;
        final Long deviceId = 9999L;
        final int offsetMillis = -28800000;
        return new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, lightValue, lightValue, 0, 0, timestamp, offsetMillis, 1, 1, 1, 0, 0, 0);
    }

}
