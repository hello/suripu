package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Insights.InsightCard;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.format.DateTimeFormat;
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
    public void test_CardGenerated_fail() {
        //no data
        final List<Integer> sleepTimeList = Lists.newArrayList();

        final Optional<InsightCard> card = CaffeineAlarm.processCaffeineAlarm(FAKE_ACCOUNT_ID, sleepTimeList, DateTimeFormat.forPattern("h:mm aa"));
        assertThat(card.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void test_CardGenerated_fail_2() {
        //not enough data
        final List<Integer> sleepTimeList = Lists.newArrayList(1,2);

        final Optional<InsightCard> card = CaffeineAlarm.processCaffeineAlarm(FAKE_ACCOUNT_ID, sleepTimeList, DateTimeFormat.forPattern("h:mm aa"));
        assertThat(card.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void test_CardGenerated_fail_3() {
        //range too large
        final List<Integer> sleepTimeList = Lists.newArrayList(0, 60, 60*10);

        final Optional<InsightCard> card = CaffeineAlarm.processCaffeineAlarm(FAKE_ACCOUNT_ID, sleepTimeList, DateTimeFormat.forPattern("h:mm aa"));
        assertThat(card.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void test_ProcessCaffeieneAlarm() {
        final List<Integer> sleepTimeList = Lists.newArrayList(1,2,3);

        final Optional<InsightCard> card = CaffeineAlarm.processCaffeineAlarm(FAKE_ACCOUNT_ID, sleepTimeList, DateTimeFormat.forPattern("h:mm aa"));
//        System.out.print(card.get().message);
        assertThat(card.isPresent(), is(Boolean.TRUE));
    }

    @Test
    public void test_ProcessCaffeieneAlarm_2() {
        final List<Integer> sleepTimeList = Lists.newArrayList(30,30,30);

        final Optional<InsightCard> card = CaffeineAlarm.processCaffeineAlarm(FAKE_ACCOUNT_ID, sleepTimeList, DateTimeFormat.forPattern("HH:mm"));
//        System.out.print(card.get().message);
        assertThat(card.isPresent(), is(Boolean.TRUE));
    }

    @Test
    public void test_failSafeGuard() {
        //Average sleep time is morning
        final List<Integer> sleepTimeList = Lists.newArrayList(60 * 9, 60 * 9, 60 * 10);

        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Integer sleepTime : sleepTimeList) {
            stats.addValue(sleepTime);
        }

        final Boolean passSafeguard = CaffeineAlarm.checkSafeGuards(stats);
        assertThat(passSafeguard, is(Boolean.FALSE));
    }

}
