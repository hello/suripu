package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Insights.InsightCard;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jyfan on 5/19/16.
 */
public class SleepAlarmInsightsTest {
    final private long FAKE_ACCOUNT_ID = 999L;
    final private Integer FAKE_AGE_OLD_ADULT = 70;
    final private Integer FAKE_AGE_ADULT = 50;
    final private Integer FAKE_AGE_YOUNG_ADULT = 22;
    final private Integer FAKE_AGE_TEEN = 16;
    final private Integer FAKE_AGE_CHILD = 10;
    final private Integer FAKE_AGE_PRESCHOOL = 4;
    final private Integer FAKE_AGE_NONE = 0;

    @Test
    public void test_NoCardGenerated() {
        //No wake times
        final List<Integer> wakeTimeList = Lists.newArrayList();

        final Optional<InsightCard> noResult = SleepAlarm.processSleepAlarm(FAKE_ACCOUNT_ID, wakeTimeList, FAKE_AGE_ADULT);
        assertThat(noResult.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void test_NoCardGenerated_2() {
        //Not enough wake times
        final List<Integer> wakeTimeList = Lists.newArrayList(1,2);

        final Optional<InsightCard> noResult = SleepAlarm.processSleepAlarm(FAKE_ACCOUNT_ID, wakeTimeList, FAKE_AGE_ADULT);
        assertThat(noResult.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void test_NoCardGenerated_3() {
        //Range too large
        final List<Integer> wakeTimeList = Lists.newArrayList(0, 60, 60*10);

        final Optional<InsightCard> noResult = SleepAlarm.processSleepAlarm(FAKE_ACCOUNT_ID, wakeTimeList, FAKE_AGE_ADULT);
        assertThat(noResult.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void test_generateCard() {

        final List<Integer> wakeTimeList = Lists.newArrayList(60*8, 60*9, 60*10);
        final Optional<InsightCard> card = SleepAlarm.processSleepAlarm(FAKE_ACCOUNT_ID, wakeTimeList, FAKE_AGE_ADULT);
//        System.out.print(card.get().message);
        assertThat(card.isPresent(), is(Boolean.TRUE));
    }

    @Test
    public void test_failSafeGuard() {
        //Range greater than 3 hrs
        final List<Integer> wakeTimeList = Lists.newArrayList(0, 60, 60*4);

        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Integer wakeTime : wakeTimeList) {
            stats.addValue(wakeTime);
        }

        final Boolean passSafeguard = SleepAlarm.checkSafeGuards(stats);
        assertThat(passSafeguard, is(Boolean.FALSE));
    }

    @Test
    public void test_failSafeGuard_2() {
        //Average wake time is afternoon
        final List<Integer> wakeTimeList = Lists.newArrayList(60*12, 60*12, 60*12);

        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Integer wakeTime : wakeTimeList) {
            stats.addValue(wakeTime);
        }

        final Boolean passSafeguard = SleepAlarm.checkSafeGuards(stats);
        assertThat(passSafeguard, is(Boolean.FALSE));
    }

    @Test
    public void test_failSafeGuard_3() {
        //Average wake time is too early
        final List<Integer> wakeTimeList = Lists.newArrayList(60*1, 60*1, 60*2);

        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Integer wakeTime : wakeTimeList) {
            stats.addValue(wakeTime);
        }

        final Boolean passSafeguard = SleepAlarm.checkSafeGuards(stats);
        assertThat(passSafeguard, is(Boolean.FALSE));
    }

    @Test
    public void test_passSafeguard() {
        final List<Integer> wakeTimeList = Lists.newArrayList(60*8, 60*9, 60*10);

        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Integer wakeTime : wakeTimeList) {
            stats.addValue(wakeTime);
        }

        final Boolean passSafeguard = SleepAlarm.checkSafeGuards(stats);
        assertThat(passSafeguard, is(Boolean.TRUE));

    }

    @Test
    public void test_getSleepRecommendation() {
        final Integer sleepRecOldAdult = SleepAlarm.getRecommendedSleepDurationMinutes(FAKE_AGE_OLD_ADULT);
        final Integer sleepRecAdult = SleepAlarm.getRecommendedSleepDurationMinutes(FAKE_AGE_ADULT);
        final Integer sleepRecYoungAdult = SleepAlarm.getRecommendedSleepDurationMinutes(FAKE_AGE_YOUNG_ADULT);
        final Integer sleepRecTeen = SleepAlarm.getRecommendedSleepDurationMinutes(FAKE_AGE_TEEN);
        final Integer sleepRecChild = SleepAlarm.getRecommendedSleepDurationMinutes(FAKE_AGE_CHILD);
        final Integer sleepRecPreChild = SleepAlarm.getRecommendedSleepDurationMinutes(FAKE_AGE_PRESCHOOL);
        final Integer sleepRecNone = SleepAlarm.getRecommendedSleepDurationMinutes(FAKE_AGE_NONE);

        final Integer expectedAdult = (8)*60;
        final Integer expectedOlderAdult = (7)*60 + (1)*30;
        final Integer expectedYoungAdult = (8)*60;
        final Integer expectedTeen = (9)*60;
        final Integer expectedChild = (10)*60;
        final Integer expectedPreChild = (11)*60 + (1)*30;

        assertThat(sleepRecOldAdult, is(expectedOlderAdult));
        assertThat(sleepRecAdult, is(expectedAdult));
        assertThat(sleepRecYoungAdult, is(expectedYoungAdult));
        assertThat(sleepRecTeen, is(expectedTeen));
        assertThat(sleepRecChild, is(expectedChild));
        assertThat(sleepRecPreChild, is(expectedPreChild));
        assertThat(sleepRecNone, is(expectedAdult));
    }

    @Test
    public void test_getSleepTimeRec() {

    }

}
