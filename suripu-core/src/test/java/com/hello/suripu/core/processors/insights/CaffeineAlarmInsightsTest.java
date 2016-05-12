package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Insights.InsightCard;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jyfan on 5/11/16.
 */
public class CaffeineAlarmInsightsTest {
    final Long FAKE_ACCOUNT_ID = 999L;

    @Test
    public void testTimeConvert() {
        final int minute = 90;
        final String expectedTime = "1:30 AM";
        final String convertedTime = CaffeineAlarm.timeConvert(minute);
        assertThat(expectedTime, is(convertedTime));
    }

    @Test
    public void testTimeConvert2() {
        final int minute = 420;
        final String expectedTime = "7:00 AM";
        final String convertedTime = CaffeineAlarm.timeConvert(minute);
        assertThat(expectedTime, is(convertedTime));
    }

    @Test
    public void testTimeConvert3() {
        final int minute = 421;
        final String expectedTime = "7:01 AM";
        final String convertedTime = CaffeineAlarm.timeConvert(minute);
        assertThat(expectedTime, is(convertedTime));
    }

    @Test
    public void testTimeConvert4() {
        final int minute = 635;
        final String expectedTime = "10:35 AM";
        final String convertedTime = CaffeineAlarm.timeConvert(minute);
        assertThat(expectedTime, is(convertedTime));
    }

    @Test
    public void testTimeConvert5() {
        final int minute = 2;
        final String expectedTime = "12:02 AM";
        final String convertedTime = CaffeineAlarm.timeConvert(minute);
        assertThat(expectedTime, is(convertedTime));
    }

    @Test
    public void getRecommendedCoffeeTime() {

    }

    @Test
    public void getRecommendedCoffeeTime2() {

    }

    @Test
    public void getRecommendedCoffeeTime3() {

    }

    @Test
    public void testProcessCaffeieneAlarm() {
        final List<Integer> sleepTimeList = Lists.newArrayList(1,2,3);

        final Optional<InsightCard> generatedCard = CaffeineAlarm.processCaffeineAlarm(FAKE_ACCOUNT_ID, sleepTimeList);
//        System.out.print(generatedCard.get().message);
        assertThat(generatedCard.isPresent(), is(Boolean.TRUE));
    }

    @Test
    public void testProcessCaffeieneAlarm2() {
        final List<Integer> sleepTimeList = Lists.newArrayList(1,2);

        final Optional<InsightCard> generatedCard = CaffeineAlarm.processCaffeineAlarm(FAKE_ACCOUNT_ID, sleepTimeList);
        assertThat(generatedCard.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void testProcessCaffeieneAlarm3() {
        final List<Integer> sleepTimeList = Lists.newArrayList();

        final Optional<InsightCard> generatedCard = CaffeineAlarm.processCaffeineAlarm(FAKE_ACCOUNT_ID, sleepTimeList);
        assertThat(generatedCard.isPresent(), is(Boolean.FALSE));
    }

}
