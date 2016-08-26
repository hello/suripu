package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.insights.InsightCard;
import com.hello.suripu.core.insights.models.text.SleepMotionMsgEN;
import com.hello.suripu.core.insights.models.text.Text;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.insights.models.SleepMotion;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by kingshy on 4/8/15.
 */
public class SleepMotionTest {

    @Test
    public void testLessMovement() {
        // account,1310,nights,35,avg_motion,0.0756
        final Long accountId = 1310L;
        final DateTime now = DateTime.now();

        final List<Integer> numMotions = Lists.newArrayList(63, 39, 26, 19, 22, 30, 34, 15, 34, 32, 59, 31, 33, 19, 21, 26, 31, 30, 31, 21, 35, 22, 27, 18, 38, 36, 33, 24, 37, 26, 39, 47, 41, 42, 108);
        final List<Integer> durations = Lists.newArrayList(494 ,439 ,654 ,231 ,345 ,650 ,596 ,267 ,375 ,437 ,629 ,343 ,435 ,454 ,470 ,401 ,395 ,369 ,511 ,419 ,551 ,429 ,439 ,334 ,446 ,457 ,452 ,433 ,426 ,421 ,474 ,508 ,429 ,479 ,516);
        final SleepStats sleepStats = new SleepStats(1, 0, 2, 3, false, 4, 10L, 20L, 0);
        final List<AggregateSleepStats> aggSleepStats = Lists.newArrayList();
        final int numData = numMotions.size();
        for (int i = 0; i < numData; i ++) {
            final MotionScore motionScore = new MotionScore(numMotions.get(i), durations.get(i), 1.0f, 1000, 10);
            aggSleepStats.add(new AggregateSleepStats(accountId, now, -252000, 10, "0.2", motionScore, 0, 0, 0, sleepStats));
        }

        final Optional<InsightCard> optionalCard = SleepMotion.processData(accountId, ImmutableList.copyOf(aggSleepStats), false);
//        System.out.print(optionalCard.get().message + "\n");
        if (optionalCard.isPresent()) {
            final Text expected = SleepMotionMsgEN.lessMovement(10, 10, 10);
            assertThat(optionalCard.get().title, is(expected.title));
        }
    }

    @Test
    public void testMoreMovement() {
        // account,1085,nights,44,avg_motion,0.3445
        final Long accountId = 1085L;
        final DateTime now = DateTime.now();
        final List<Integer> numMotions = Lists.newArrayList(187, 150, 142, 134, 139, 189, 134, 143, 136, 193, 140, 106, 103, 220, 135, 132, 166, 126, 161, 132, 209, 230, 207, 106, 150, 167, 267, 260, 171, 124, 117, 144, 101, 171, 215, 204, 97, 99, 91, 86, 91, 85, 97, 86);
        final List<Integer> durations = Lists.newArrayList(518 ,358 ,431 ,399 ,375 ,560 ,530 ,413 ,426 ,402 ,348 ,412 ,761 ,361 ,447 ,419 ,403 ,440 ,535 ,490 ,525 ,534 ,524 ,299 ,519 ,421 ,546 ,546 ,422 ,338 ,358 ,370 ,364 ,390 ,598 ,492, 473 ,559 ,305 ,265 ,399 ,307 ,319 ,290);
        final SleepStats sleepStats = new SleepStats(1, 0, 2, 3, false, 4, 10L, 20L, 0);
        final List<AggregateSleepStats> aggSleepStats = Lists.newArrayList();
        final int numData = numMotions.size();
        for (int i = 0; i < numData; i ++) {
            final MotionScore motionScore = new MotionScore(numMotions.get(i), durations.get(i), 1.0f, 1000, 10);
            aggSleepStats.add(new AggregateSleepStats(accountId, now, -252000, 10, "0.2", motionScore, 0, 0, 0, sleepStats));
        }

        final Optional<InsightCard> optionalCard = SleepMotion.processData(accountId, ImmutableList.copyOf(aggSleepStats), false);
//        System.out.print(optionalCard.get().message + "\n");
        if (optionalCard.isPresent()) {
            final Text expected = SleepMotionMsgEN.moreMovement(10, 10, 10);
            assertThat(optionalCard.get().title, is(expected.title));
        }
    }

    @Test
    public void testEqualMovement() {
        // account,1001,nights,37,avg_motion,0.1353
        final Long accountId = 1001L;
        final DateTime now = DateTime.now();
        final List<Integer> numMotions = Lists.newArrayList(112, 104, 72, 51, 51, 68, 56, 61, 54, 66, 71, 58, 10, 82, 41, 60, 77, 86, 91, 80, 58, 83, 69, 27, 83, 64, 59, 79, 76, 65, 20, 46, 95, 41, 37, 21, 47);
        final List<Integer> durations = Lists.newArrayList(635 ,351 ,604 ,482 ,363 ,495 ,459 ,535 ,422 ,451 ,439 ,433 ,59 ,442 ,386 ,438 ,547 ,604 ,585 ,543 ,532 ,501 ,542 ,304 ,575 ,512 ,483 ,511 ,524 ,491 ,166 ,505 ,560 ,357 ,508 ,394 ,447);
        final SleepStats sleepStats = new SleepStats(1, 0, 2, 3, false, 4, 10L, 20L, 0);
        final List<AggregateSleepStats> aggSleepStats = Lists.newArrayList();
        final int numData = numMotions.size();
        for (int i = 0; i < numData; i ++) {
            final MotionScore motionScore = new MotionScore(numMotions.get(i), durations.get(i), 1.0f, 1000, 10);
            aggSleepStats.add(new AggregateSleepStats(accountId, now, -252000, 10, "0.2", motionScore, 0, 0, 0, sleepStats));
        }

        final Optional<InsightCard> optionalCard = SleepMotion.processData(accountId, ImmutableList.copyOf(aggSleepStats), false);
//        System.out.print(optionalCard.get().message + "\n");
        if (optionalCard.isPresent()) {
            final Text expected = SleepMotionMsgEN.moreMovement(10, 10, 10);
            assertThat(optionalCard.get().title, is(expected.title));
        }

    }

    @Test
    public void testReallyEqualMovement() {
        // account,1001,nights,37,avg_motion,0.1353
        final Long accountId = 1001L;
        final DateTime now = DateTime.now();
        final List<Integer> numMotions = Lists.newArrayList(112, 104, 72, 51, 51, 68, 56, 61, 54, 66, 71, 58, 10, 82, 41, 60, 77, 86, 91, 80, 58, 83, 69, 27, 83, 64, 59, 79, 76, 65, 20, 46, 95, 41, 37, 21, 47);
        final List<Integer> durations = Lists.newArrayList(2800 ,351 ,604 ,482 ,363 ,495 ,459 ,535 ,422 ,451 ,439 ,433 ,59 ,442 ,386 ,438 ,547 ,604 ,585 ,543 ,532 ,501 ,542 ,304 ,575 ,512 ,483 ,511 ,524 ,491 ,166 ,505 ,560 ,357 ,508 ,394 ,447);
        final SleepStats sleepStats = new SleepStats(1, 0, 2, 3, false, 4, 10L, 20L, 0);
        final List<AggregateSleepStats> aggSleepStats = Lists.newArrayList();
        final int numData = numMotions.size();
        for (int i = 0; i < numData; i ++) {
            final MotionScore motionScore = new MotionScore(numMotions.get(i), durations.get(i), 1.0f, 1000, 10);
            aggSleepStats.add(new AggregateSleepStats(accountId, now, -252000, 10, "0.2", motionScore, 0, 0, 0, sleepStats));
        }

        final Optional<InsightCard> optionalCard = SleepMotion.processData(accountId, ImmutableList.copyOf(aggSleepStats), false);
//        System.out.print(optionalCard.get().message + "\n");
        if (optionalCard.isPresent()) {
            final Text expected = SleepMotionMsgEN.lessMovement(10, 10, 10);
            assertThat(optionalCard.get().title, is(expected.title));
        }

    }


}
