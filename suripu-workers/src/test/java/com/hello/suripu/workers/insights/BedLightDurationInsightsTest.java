package com.hello.suripu.workers.insights;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.BedLightDurationMsgEN;
import com.hello.suripu.core.processors.insights.BedLightDuration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
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

        final int durationAverage = BedLightDuration.computeAverage(lightDurationList);

        final Optional<InsightCard> noResult = BedLightDuration.scoreCardBedLightDuration(durationAverage, accountId);
        assertThat(noResult.isPresent(), is(Boolean.FALSE));

        //test size 1 no generation
        final int zeroLight = 0;
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offMinuteThreshold = 45;

        final List<DeviceData> data = new ArrayList<>();
        addDeviceData(data, zeroLight, timestamp.withHourOfDay(18));

        final Integer calculatedLightDurationList = BedLightDuration.findLightOnDurationForDay(data, offMinuteThreshold);
        assertThat(calculatedLightDurationList, is(0));
    }

    @Test
    public void testOneDay() {
        final int light = 2;
        final int zeroLight = 0;
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offMinuteThreshold = 45;

        final List<DeviceData> data = new ArrayList<>();
        addDeviceData(data, zeroLight, timestamp.withHourOfDay(18));
        addDeviceData(data, light, timestamp);
        addDeviceData(data, light, timestamp.withMinuteOfHour(10));
        addDeviceData(data, light, timestamp.withMinuteOfHour(30));
        addDeviceData(data, light, timestamp.withMinuteOfHour(45));

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

        final List<DeviceData> data = new ArrayList<>();
        addDeviceData(data, light, timestamp);
        addDeviceData(data, light, timestamp.withMinuteOfHour(50));
        addDeviceData(data, light, timestamp.withMinuteOfHour(51));
        addDeviceData(data, light, timestamp.withMinuteOfHour(52));
        addDeviceData(data, zeroLight, timestamp.withMinuteOfHour(55));

        final Integer calculatedLightDurationList = BedLightDuration.findLightOnDurationForDay(data, offMinuteThreshold);
        final Integer expectedLightDurationList = 5;
        assertThat(calculatedLightDurationList, is(expectedLightDurationList));
    }

    @Test
    public void testOneDayThresholdEdge1() {
        final int light = 2;
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offMinuteThreshold = 45;

        final List<DeviceData> data = new ArrayList<>();
        addDeviceData(data, light, timestamp);
        addDeviceData(data, light, timestamp.withMinuteOfHour(50));

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

        final List<DeviceData> data = new ArrayList<>();
        addDeviceData(data, light, timestamp);
        addDeviceData(data, zeroLight, timestamp.withMinuteOfHour(10));
        addDeviceData(data, zeroLight, timestamp.withMinuteOfHour(30));
        addDeviceData(data, zeroLight, timestamp.withMinuteOfHour(45));
        addDeviceData(data, zeroLight, timestamp.withHourOfDay(21));

        final Integer calculatedLightDurationList = BedLightDuration.findLightOnDurationForDay(data, offMinuteThreshold);
        final Integer expectedLightDurationList = light/4;
        assertThat(calculatedLightDurationList, is(expectedLightDurationList));
    }

    @Test
    public void testDurationLow() {
        final Long accountId = 999L;

        final List<Integer> lightDurationList = new ArrayList<>();
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

        final List<Integer> lightDurationList = new ArrayList<>();
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

        final List<Integer> lightDurationList = new ArrayList<>();
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

        final List<DeviceData> data = new ArrayList<>();
        addDeviceData(data, 0, timestamp.withHourOfDay(21));
        addDeviceData(data, 0, timestamp.withHourOfDay(23));

        assertThat(BedLightDuration.sameDay(data.get(1), data.get(0)), is(Boolean.TRUE));
    }

    @Test
    public void testSplitDeviceDataByDay() {
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(21).withMinuteOfHour(0);

        final List<DeviceData> data = new ArrayList<>();
        addDeviceData(data, 0, timestamp.withHourOfDay(21));
        addDeviceData(data, 0, timestamp.withHourOfDay(23));
        addDeviceData(data, 0, timestamp.plusDays(1).withHourOfDay(4));

        addDeviceData(data, 1, timestamp.plusDays(1).withHourOfDay(21));
        addDeviceData(data, 1, timestamp.plusDays(2).withHourOfDay(3));
        addDeviceData(data, 1, timestamp.plusDays(2).withHourOfDay(4));

        addDeviceData(data, 2, timestamp.plusDays(2).withHourOfDay(21));

        addDeviceData(data, 3, timestamp.plusDays(4).withHourOfDay(1));
        addDeviceData(data, 3, timestamp.plusDays(4).withHourOfDay(2));

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

        final List<DeviceData> data = new ArrayList<>();
        addDeviceData(data, 0, timestamp.withHourOfDay(21));
        addDeviceData(data, 0, timestamp.withHourOfDay(21).withMinuteOfHour(45));
        addDeviceData(data, 0, timestamp.withHourOfDay(21).withMinuteOfHour(55));
        addDeviceData(data, 0, timestamp.withHourOfDay(22).withMinuteOfHour(15));

        addDeviceData(data, 1, timestamp.plusDays(1).withHourOfDay(21));
        addDeviceData(data, 1, timestamp.plusDays(2).withHourOfDay(3));
        addDeviceData(data, 1, timestamp.plusDays(2).withHourOfDay(4));

        addDeviceData(data, 2, timestamp.plusDays(2).withHourOfDay(21));

        addDeviceData(data, 3, timestamp.plusDays(4).withHourOfDay(1));
        addDeviceData(data, 3, timestamp.plusDays(4).withHourOfDay(2));

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

        final List<DeviceData> deviceData = Lists.newArrayList();
        final List<Integer> lightData = Arrays.asList(58, 58, 57, 57, 57, 56, 56, 55, 55, 54, 52, 51, 50, 48, 45, 43, 41, 39, 37, 36, 35, 33, 30, 29, 28, 29, 28, 28, 27, 25, 23, 20, 18, 16, 14, 13, 12, 11, 10, 10, 9, 9, 8, 7, 6, 6, 5, 101, 198, 199, 199, 200, 199, 197, 197, 197, 197, 197, 196, 197, 197, 198, 197, 196, 198, 199, 200, 200, 200, 200, 200, 201, 202, 202, 124, 208, 190, 193, 205, 209, 198, 194, 207, 209, 208, 195, 181, 177, 182, 182, 178, 165, 167, 182, 194, 203, 196, 202, 211, 219, 224, 226, 223, 215, 216, 226, 227, 226, 215, 220, 216, 223, 213, 200, 195, 200, 196, 199, 191, 186, 191, 197, 186, 180, 186, 187, 182, 177, 171, 168, 171, 173, 161, 162, 167, 166, 160, 172, 172, 168, 169, 165, 159, 161, 161, 158, 160, 153, 161, 159, 159, 161, 161, 162, 158, 158, 157, 158, 161, 170, 179, 178, 170, 170, 169, 168, 176, 184, 192, 191, 180, 176, 175, 153, 151, 170, 178, 184, 180, 183, 185, 190, 193, 195, 198, 198, 195, 196, 198, 196, 192, 193, 19);

        final List<Long> timeStamps = Arrays.asList(1432177200000L, 1432177260000L, 1432177320000L, 1432177380000L, 1432177440000L, 1432177500000L, 1432177560000L, 1432177620000L, 1432177680000L, 1432177740000L, 1432177800000L, 1432177860000L, 1432177920000L, 1432177980000L, 1432178040000L, 1432178100000L, 1432178160000L, 1432178220000L, 1432178280000L, 1432178340000L, 1432178400000L, 1432178460000L, 1432178520000L, 1432178580000L, 1432178640000L, 1432178700000L, 1432178760000L, 1432178820000L, 1432178880000L, 1432178940000L, 1432179000000L, 1432179060000L, 1432179120000L, 1432179180000L, 1432179240000L, 1432179300000L, 1432179360000L, 1432179420000L, 1432179480000L, 1432179540000L, 1432179600000L, 1432179660000L, 1432179720000L, 1432179780000L, 1432179840000L, 1432179900000L, 1432179960000L, 1432201620000L, 1432201680000L, 1432201740000L, 1432201800000L, 1432201860000L, 1432201920000L, 1432201980000L, 1432202040000L, 1432202100000L, 1432202160000L, 1432202220000L, 1432202280000L, 1432202340000L, 1432202400000L, 1432202460000L, 1432202520000L, 1432202580000L, 1432202640000L, 1432202700000L, 1432202760000L, 1432202820000L, 1432202880000L, 1432202940000L, 1432203000000L, 1432203060000L, 1432203120000L, 1432203180000L, 1432225080000L, 1432225140000L, 1432225200000L, 1432225260000L, 1432225320000L, 1432225380000L, 1432225440000L, 1432225500000L, 1432225560000L, 1432225620000L, 1432225680000L, 1432225740000L, 1432225800000L, 1432225860000L, 1432225920000L, 1432225980000L, 1432226040000L, 1432226100000L, 1432226160000L, 1432226220000L, 1432226280000L, 1432226340000L, 1432226400000L, 1432226460000L, 1432226520000L, 1432226580000L, 1432226640000L, 1432226700000L, 1432226760000L, 1432226820000L, 1432226880000L, 1432226940000L, 1432227000000L, 1432227060000L, 1432227120000L, 1432227180000L, 1432227240000L, 1432227300000L, 1432227360000L, 1432227420000L, 1432227480000L, 1432227540000L, 1432227600000L, 1432227660000L, 1432227720000L, 1432227780000L, 1432227840000L, 1432227900000L, 1432227960000L, 1432228020000L, 1432228080000L, 1432228140000L, 1432228200000L, 1432228260000L, 1432228320000L, 1432228380000L, 1432228440000L, 1432228500000L, 1432228560000L, 1432228620000L, 1432228680000L, 1432228740000L, 1432228800000L, 1432228860000L, 1432228920000L, 1432228980000L, 1432229040000L, 1432229100000L, 1432229160000L, 1432229220000L, 1432229280000L, 1432229340000L, 1432229400000L, 1432229460000L, 1432229520000L, 1432229580000L, 1432229640000L, 1432229700000L, 1432229760000L, 1432229820000L, 1432229880000L, 1432229940000L, 1432230000000L, 1432230060000L, 1432230120000L, 1432230180000L, 1432230240000L, 1432230300000L, 1432230360000L, 1432230420000L, 1432230480000L, 1432230540000L, 1432230600000L, 1432230660000L, 1432230720000L, 1432230780000L, 1432230840000L, 1432230900000L, 1432230960000L, 1432231020000L, 1432231080000L, 1432231140000L, 1432231200000L, 1432231260000L, 1432231320000L, 1432231380000L, 1432231440000L, 1432231500000L, 1432231560000L, 1432231620000L, 1432231680000L, 1432231740000L, 1432231800000L, 1432231860000L, 1432231920000L, 1432231980000L, 1432232040000L, 1432232100000L, 1432232160000L
        );

        for (int i = 0; i < lightData.size(); i++) {
            final int myHour = new DateTime(timeStamps.get(i)).getHourOfDay();
            if (myHour > 21 || myHour < 4) {
                addDeviceData(deviceData, lightData.get(i), new DateTime(timeStamps.get(i)));
            }
        }

        //below is copy-pasted code from BedLightDuration.getInsights
        final List<List<DeviceData>> deviceDataByDay = BedLightDuration.splitDeviceDataByDay(deviceData);

        final List<Integer> lightOnDurationList = Lists.newArrayList();

        for (int i = 0; i < deviceDataByDay.size(); i++) { //correct incrementation?
            lightOnDurationList.add(BedLightDuration.findLightOnDurationForDay(deviceDataByDay.get(i), BedLightDuration.OFF_MINUTES_THRESHOLD));// change to add
        }

        final int avgLightOn;

        if (lightOnDurationList.size() == 0) {
            avgLightOn = 0;
        }
        else {
            avgLightOn = BedLightDuration.computeAverage(lightOnDurationList);
        }

        assertThat(avgLightOn, is(26));
//        return BedLightDuration.scoreCardBedLightDuration(avgLightOn, accountId);


    }

    private static void addDeviceData(final List<DeviceData> data, final Integer lightValue, final DateTime timestamp) {
        final Long accountId = 999L;
        final Long deviceId = 9999L;
        final int offsetMillis = -28800000;
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, lightValue, lightValue, 0, 0, timestamp, offsetMillis, 1, 1, 1, 0, 0, 0));
    }

}
