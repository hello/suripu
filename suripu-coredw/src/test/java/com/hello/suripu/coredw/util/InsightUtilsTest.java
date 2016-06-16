package com.hello.suripu.coredw.util;

import com.hello.suripu.core.preferences.TimeFormat;
import com.hello.suripu.core.util.InsightUtils;

import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


/**
 * Created by jyfan on 5/18/16.
 */
public class InsightUtilsTest {

    @Test
    public void testTimeConvert() {
        final int minute = 90;
        final String expectedTime = "1:30 AM";
        final String convertedTime = InsightUtils.timeConvert(minute, TimeFormat.CIVILIAN);
        assertThat(expectedTime, is(convertedTime));
    }

    @Test
    public void testTimeConvert2() {
        final int minute = 420;
        final String expectedTime = "7:00 AM";
        final String convertedTime = InsightUtils.timeConvert(minute, TimeFormat.CIVILIAN);
        assertThat(expectedTime, is(convertedTime));
    }

    @Test
    public void testTimeConvert3() {
        final int minute = 421;
        final String expectedTime = "7:01 AM";
        final String convertedTime = InsightUtils.timeConvert(minute, TimeFormat.CIVILIAN);
        assertThat(expectedTime, is(convertedTime));
    }

    @Test
    public void testTimeConvert4() {
        final int minute = 635;
        final String expectedTime = "10:35 AM";
        final String convertedTime = InsightUtils.timeConvert(minute, TimeFormat.CIVILIAN);
        assertThat(expectedTime, is(convertedTime));
    }

    @Test
    public void testTimeConvert5() {
        final int minute = 2;
        final String expectedTime = "12:02 AM";
        final String convertedTime = InsightUtils.timeConvert(minute, TimeFormat.CIVILIAN);
        assertThat(expectedTime, is(convertedTime));
    }

    @Test
    public void testTimeConvert6() {
        final int minute = 2 + (24 * 60) * 2;
        final String expectedTime = "12:02 AM";
        final String convertedTime = InsightUtils.timeConvert(minute, TimeFormat.CIVILIAN);
        assertThat(expectedTime, is(convertedTime));
    }

    @Test
    public void testTimeConvertRound3() {
        final int minute = 421;
        final String expectedTime = "7:00 AM";
        final String convertedTime = InsightUtils.timeConvertRound(minute, TimeFormat.CIVILIAN);
        assertThat(expectedTime, is(convertedTime));
    }

    @Test
    public void testTimeConvertRound4() {
        final int minute = 635;
        final String expectedTime = "10:30 AM";
        final String convertedTime = InsightUtils.timeConvertRound(minute, TimeFormat.CIVILIAN);
        assertThat(expectedTime, is(convertedTime));
    }

    @Test
    public void testTimeConvertRound5() {
        final int minute = 2;
        final String expectedTime = "12:00 AM";
        final String convertedTime = InsightUtils.timeConvertRound(minute, TimeFormat.CIVILIAN);
        assertThat(expectedTime, is(convertedTime));
    }

    @Test
    public void testTimeConvertRound6() {
        final int minute = 2 + (24 * 60) * 2;
        final String expectedTime = "12:00 AM";
        final String convertedTime = InsightUtils.timeConvertRound(minute, TimeFormat.CIVILIAN);
        assertThat(expectedTime, is(convertedTime));
    }

    @Test
    public void testTimeConvertRound7() {
        final int minute = 2;
        final String expectedTime = "00:00";
        final String convertedTime = InsightUtils.timeConvertRound(minute, TimeFormat.MILITARY);
        assertThat(expectedTime, is(convertedTime));
    }

    @Test
    public void testTimeConvertRound8() {
        final int minute = 635;
        final String expectedTime = "10:30";
        final String convertedTime = InsightUtils.timeConvertRound(minute, TimeFormat.MILITARY);
        assertThat(expectedTime, is(convertedTime));
    }
}
