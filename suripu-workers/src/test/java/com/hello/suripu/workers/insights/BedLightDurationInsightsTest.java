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
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jyfan on 8/20/15.
 */
public class BedLightDurationInsightsTest {

    private final Long fake_accountId1 = 9999L;
    private final int fake_light = 2;
    private final Integer offMinuteThreshold = 45;
    private final DateTime standard_timestamp = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay();

    @Test
    public void test_noCard() {
        //edge case of no light on period for any day, try to take average and score
        final List<Integer> lightDurationList = Lists.newArrayList();
        final Integer durationAverage = BedLightDuration.computeAverage(lightDurationList);
        assertThat(durationAverage, is(0));

        final Optional<InsightCard> noResult = BedLightDuration.scoreCardBedLightDuration(durationAverage, fake_accountId1);
        assertThat(noResult.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void test_findLightOnDurationForDay_noTruncate() {

        final List<DeviceData> data = Lists.newArrayList(
                //no deviceData will be truncated
                createNewDeviceData(fake_light, standard_timestamp),
                createNewDeviceData(fake_light, standard_timestamp.withMinuteOfHour(10)),
                createNewDeviceData(fake_light, standard_timestamp.withMinuteOfHour(30)),
                createNewDeviceData(fake_light, standard_timestamp.withMinuteOfHour(45))
        );

        final Integer calculatedLightDurationList = BedLightDuration.findLightOnDurationForDay(data, offMinuteThreshold, fake_accountId1);
        final Integer expectedLightDurationList = 45;
        assertThat(calculatedLightDurationList, is(expectedLightDurationList));
    }

    @Test
    public void test_findLightOnDurationForDay_truncate() {

        final List<DeviceData> data = Lists.newArrayList(
                createNewDeviceData(fake_light, standard_timestamp),

                //preceding deviceData will be truncated
                createNewDeviceData(fake_light, standard_timestamp.withMinuteOfHour(50)),
                createNewDeviceData(fake_light, standard_timestamp.withMinuteOfHour(51)),
                createNewDeviceData(fake_light, standard_timestamp.withMinuteOfHour(52)),
                createNewDeviceData(fake_light, standard_timestamp.withMinuteOfHour(55))
        );

        final Integer calculatedLightDurationList = BedLightDuration.findLightOnDurationForDay(data, offMinuteThreshold, fake_accountId1);
        final Integer expectedLightDurationList = 5;
        assertThat(calculatedLightDurationList, is(expectedLightDurationList));
    }

    @Test
    public void test_findLightOnDurationForDay_truncate2() {

        final List<DeviceData> data = Lists.newArrayList(
                createNewDeviceData(fake_light, standard_timestamp.withHourOfDay(19)),

                //preceding deviceData will be truncated
                createNewDeviceData(fake_light, standard_timestamp.withHourOfDay(19).withMinuteOfHour(50))
        );

        final Integer calculatedLightDurationList = BedLightDuration.findLightOnDurationForDay(data, offMinuteThreshold, fake_accountId1);
        final Integer expectedLightDurationList = 0;
        assertThat(calculatedLightDurationList, is(expectedLightDurationList));
    }

    @Test
    public void test_findLightDurationForDay_truncate3() {

        final List<DeviceData> data = Lists.newArrayList(
                createNewDeviceData(fake_light, standard_timestamp),
                createNewDeviceData(fake_light, standard_timestamp.withHourOfDay(19).withMinuteOfHour(10)),
                createNewDeviceData(fake_light, standard_timestamp.withHourOfDay(19).withMinuteOfHour(30)),
                createNewDeviceData(fake_light, standard_timestamp.withHourOfDay(19).withMinuteOfHour(45)),

                //preceding deviceData will be truncated
                createNewDeviceData(fake_light, standard_timestamp.withHourOfDay(21))
        );

        final Integer calculatedLightDuration = BedLightDuration.findLightOnDurationForDay(data, offMinuteThreshold, fake_accountId1);
        final Integer expectedLightDuration = 0;
        assertThat(calculatedLightDuration, is(expectedLightDuration));
    }

    @Test
    public void test_computeAverage_scoreCardBedLightDuration_Low() {

        final List<Integer> lightDurationList = Lists.newArrayList(
                0,
                60,
                10);

        final Integer durationAverage = BedLightDuration.computeAverage(lightDurationList);
        final Optional<InsightCard> lowResult = BedLightDuration.scoreCardBedLightDuration(durationAverage, fake_accountId1);
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
    public void test_computeAverage_scoreCardBedLightDuration_Medium() {

        final List<Integer> lightDurationList = Lists.newArrayList(
                60,
                120,
                120);

        final Integer durationAverage = BedLightDuration.computeAverage(lightDurationList);

        final Optional<InsightCard> mediumResult = BedLightDuration.scoreCardBedLightDuration(durationAverage, fake_accountId1);
        assertThat(mediumResult.isPresent(), is(Boolean.TRUE));

        final String expectedTitle = BedLightDurationMsgEN.getMediumLight().title;
        assertThat(mediumResult.get().title, is(expectedTitle));

        final String mediumMessage = BedLightDurationMsgEN.getMediumLight().title;
        assertThat(mediumMessage, is(mediumMessage));
    }

    @Test
    public void test_scoreCardBedLightDuration_High() {

        final List<Integer> lightDurationList = Lists.newArrayList(
                120,
                200,
                300);

        final Integer durationAverage = BedLightDuration.computeAverage(lightDurationList);

        final Optional<InsightCard> highResult = BedLightDuration.scoreCardBedLightDuration(durationAverage, fake_accountId1);
        assertThat(highResult.isPresent(), is(Boolean.TRUE));

        final String expectedTitle = BedLightDurationMsgEN.getHighLight().title;
        assertThat(highResult.get().title, is(expectedTitle));

        final String highMessage = BedLightDurationMsgEN.getHighLight().title;
        assertThat(highMessage, is(highMessage));
    }

    @Test
    public void test_SameDay() {

        final List<DeviceData> data = Lists.newArrayList(
                createNewDeviceData(0, standard_timestamp.withHourOfDay(21)),
                createNewDeviceData(0, standard_timestamp.withHourOfDay(23))
        );

        assertThat(BedLightDuration.sameDay(data.get(1), data.get(0)), is(Boolean.TRUE));
    }

    @Test
    public void test_SameDay_2() {

        final List<DeviceData> data = Lists.newArrayList(
                createNewDeviceData(0, standard_timestamp.withHourOfDay(4)),
                createNewDeviceData(0, standard_timestamp.withHourOfDay(21))
        );

        assertThat(BedLightDuration.sameDay(data.get(1), data.get(0)), is(Boolean.FALSE));
    }

    @Test
    public void test_splitDeviceDataByDay_findLightOnDurationForDay() {

        final List<DeviceData> data = Lists.newArrayList(
                createNewDeviceData(0, standard_timestamp.withHourOfDay(21)),
                createNewDeviceData(0, standard_timestamp.withHourOfDay(23)),
                createNewDeviceData(0, standard_timestamp.plusDays(1).withHourOfDay(4)),

                createNewDeviceData(1, standard_timestamp.plusDays(1).withHourOfDay(21)),
                createNewDeviceData(1, standard_timestamp.plusDays(2).withHourOfDay(3)),
                createNewDeviceData(1, standard_timestamp.plusDays(2).withHourOfDay(4)),

                createNewDeviceData(2, standard_timestamp.plusDays(2).withHourOfDay(21)),

                createNewDeviceData(3, standard_timestamp.plusDays(4).withHourOfDay(1)),
                createNewDeviceData(3, standard_timestamp.plusDays(4).withHourOfDay(2))
        );

        final List<List<DeviceData>> deviceDataByDay = BedLightDuration.splitDeviceDataByDay(data);
        assertThat(deviceDataByDay.size(), is(4));

        //off_minute_threshold of 420 minutes = 7 hours, we expect no data to be truncated
        final Integer day1Duration = BedLightDuration.findLightOnDurationForDay(deviceDataByDay.get(0), 420, fake_accountId1);
        final Integer day2Duration = BedLightDuration.findLightOnDurationForDay(deviceDataByDay.get(1), 420, fake_accountId1);
        final Integer day3Duration = BedLightDuration.findLightOnDurationForDay(deviceDataByDay.get(2), 420, fake_accountId1);
        final Integer day4Duration = BedLightDuration.findLightOnDurationForDay(deviceDataByDay.get(3), 420, fake_accountId1);

        assertThat(day1Duration, is(7*60));
        assertThat(day2Duration, is(7*60));
        assertThat(day3Duration, is(0*60));
        assertThat(day4Duration, is(1*60));
    }

    @Test
    public void test_splitDeviceDataByDay_findLightOnDurationForDay_2() {

        final List<DeviceData> data = Lists.newArrayList(
                createNewDeviceData(0, standard_timestamp.withHourOfDay(21)),
                createNewDeviceData(0, standard_timestamp.withHourOfDay(21)),
                createNewDeviceData(0, standard_timestamp.withHourOfDay(21).withMinuteOfHour(45)),
                createNewDeviceData(0, standard_timestamp.withHourOfDay(21).withMinuteOfHour(55)),
                createNewDeviceData(0, standard_timestamp.withHourOfDay(22).withMinuteOfHour(15)),

                createNewDeviceData(1, standard_timestamp.plusDays(1).withHourOfDay(21)),
                createNewDeviceData(1, standard_timestamp.plusDays(2).withHourOfDay(3)),
                createNewDeviceData(1, standard_timestamp.plusDays(2).withHourOfDay(4)),

                createNewDeviceData(2, standard_timestamp.plusDays(2).withHourOfDay(21)),

                createNewDeviceData(3, standard_timestamp.plusDays(4).withHourOfDay(1)),
                createNewDeviceData(3, standard_timestamp.plusDays(4).withHourOfDay(2))
        );

        final List<List<DeviceData>> deviceDataByDay = BedLightDuration.splitDeviceDataByDay(data);
        assertThat(deviceDataByDay.size(), is(4));

        final Integer day1Duration = BedLightDuration.findLightOnDurationForDay(deviceDataByDay.get(0), offMinuteThreshold, fake_accountId1);
        final Integer day2Duration = BedLightDuration.findLightOnDurationForDay(deviceDataByDay.get(1), offMinuteThreshold, fake_accountId1);
        final Integer day3Duration = BedLightDuration.findLightOnDurationForDay(deviceDataByDay.get(2), offMinuteThreshold, fake_accountId1);
        final Integer day4Duration = BedLightDuration.findLightOnDurationForDay(deviceDataByDay.get(3), offMinuteThreshold, fake_accountId1);

        assertThat(day1Duration, is(30));
        assertThat(day2Duration, is(0*60));
        assertThat(day3Duration, is(0*60));
        assertThat(day4Duration, is(0*60));
    }


// Below: testing getInsightHelper and its call

    @Test
    public void testSomeUser() throws IOException{
//        Data pulled from 8-01-2015 from accountId 20894 - python script returns 25.2 minutes

        final List<Integer> lightData = readLightData("insights/bedLightDuration/lightDataUser1.csv");
        final List<Long> timeStamps = readTimeStamps("insights/bedLightDuration/timeStampUser1.csv");

        final List<DeviceData> deviceData = Lists.newArrayList();
        for (int i = 0; i < lightData.size(); i++) {
            final Integer myHour = new DateTime(timeStamps.get(i), DateTimeZone.forOffsetMillis(-25200000)).getHourOfDay();
            if (myHour > 21 || myHour < 4) {
                deviceData.add(createNewDeviceData(lightData.get(i), new DateTime(timeStamps.get(i))));
            }
        }

        final Integer avgLightOn = BedLightDuration.getInsightsHelper(deviceData, fake_accountId1);
        assertThat(avgLightOn, is(26));
    }

    @Test
    public void testSomeUser2() throws IOException{
//        Data pulled from 8-01-2015 from accountId 22262 - python script returns 0 minutes

        final List<Integer> lightData = readLightData("insights/bedLightDuration/lightDataUser2.csv");
        final List<Long> timeStamps = readTimeStamps("insights/bedLightDuration/timeStampUser2.csv");


        final List<DeviceData> deviceData = Lists.newArrayList();

        for (int i = 0; i < lightData.size(); i++) {
            final Integer myHour = new DateTime(timeStamps.get(i), DateTimeZone.forOffsetMillis(-25200000)).getHourOfDay();
            if (myHour > 21 || myHour < 4) {
                deviceData.add(createNewDeviceData(lightData.get(i), new DateTime(timeStamps.get(i))));
            }
        }
        
        final Integer avgLightOn = BedLightDuration.getInsightsHelper(deviceData, fake_accountId1);
        assertThat(avgLightOn, is(0));
    }

    @Test
    public void testSomeUser3() throws IOException{
//        Data pulled from 8-01-2015 from accountId 27053 - python script returns 175.6 minutes

        final List<Integer> lightData = readLightData("insights/bedLightDuration/lightDataUser3.csv");
        final List<Long> timeStamps = readTimeStamps("insights/bedLightDuration/timeStampUser3.csv");

        final List<DeviceData> deviceData = Lists.newArrayList();

        for (int i = 0; i < lightData.size(); i++) {
            final Integer myHour = new DateTime(timeStamps.get(i), DateTimeZone.forOffsetMillis(-25200000)).getHourOfDay();
            if (myHour > 21 || myHour < 4) {
                deviceData.add(createNewDeviceData(lightData.get(i), new DateTime(timeStamps.get(i))));
            }
        }

        final Integer avgLightOn = BedLightDuration.getInsightsHelper(deviceData, fake_accountId1);
        assertThat(avgLightOn, is(176));
    }

    private static List<Integer> readLightData(final String fileName) throws IOException{
        final List<Integer> lightData = Lists.newArrayList();
        final URL userCSVFile = Resources.getResource(fileName);
        if (userCSVFile==null) {
            throw new IOException();
        }
        final String csvString = Resources.toString(userCSVFile, Charsets.UTF_8);
        final String[] lightDataStringList = csvString.replaceAll("\\s+","").split(",");
        for (String lightDataString : lightDataStringList) {
            lightData.add(Integer.parseInt(lightDataString));
        }
        return lightData;
    }

    private static List<Long> readTimeStamps(final String fileName) throws IOException{
        final List<Long> timeStamps = Lists.newArrayList();
        final URL timeStampCSVFile = Resources.getResource(fileName);
        if (timeStampCSVFile==null) {
            throw new IOException();
        }
        final String csvString = Resources.toString(timeStampCSVFile, Charsets.UTF_8);
        final String[] timeStampStringList = csvString.replaceAll("\\s+","").replaceAll("L","").split(",");
        for (String timeStampString : timeStampStringList) {
            timeStamps.add(Long.parseLong(timeStampString));
        }
        return timeStamps;
    }

    private static DeviceData createNewDeviceData(final Integer lightValue, final DateTime timestamp) {
        final Long mock_accountId = 9999L;
        final Long mock_deviceId = 9999L;
        final Integer offsetMillis = -28800000;
        return new DeviceData(mock_accountId, mock_deviceId, 0, 0, 0, 0, 0, 0, 0, lightValue, lightValue, 0, 0, timestamp, offsetMillis, 1, 1, 1, 0, 0, 0);
    }

}
