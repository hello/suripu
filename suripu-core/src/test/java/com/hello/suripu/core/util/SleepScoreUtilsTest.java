package com.hello.suripu.core.util;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by kingshy on 2/25/15.
 */
public class SleepScoreUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepScoreUtilsTest.class);

    @Test
    public void testPerfectScore() {
        final int age = 23;
        final List<Integer> sleepDurationMinutes = Lists.newArrayList(420, 450, 480, 510, 540);

        for (int i = 0; i < sleepDurationMinutes.size(); i++) {
            final int score = SleepScoreUtils.getSleepDurationScore(age, sleepDurationMinutes.get(i));
            LOGGER.trace("value {} -> {}", sleepDurationMinutes.get(i), score);
            assertThat(score, is(SleepScoreUtils.DURATION_MAX_SCORE));
        }
    }

    @Test
    public void testTerribleScores() {
        final List<Integer> ages = Lists.newArrayList(16, 23, 27, 68);
        final List<Integer> duration = Lists.newArrayList(330, 299, 299, 790);
        for (int i = 0; i < duration.size(); i++) {
            final int score = SleepScoreUtils.getSleepDurationScore(ages.get(i), duration.get(i));
            LOGGER.trace("value {} -> {}", duration.get(i), score);
            assertThat(score, is(SleepScoreUtils.DURATION_MIN_SCORE));
        }
    }

    @Test
    public void testRandomScores() {
        final int age = 23;
        final List<Integer> duration = Lists.newArrayList(330, 400, 600, 650);
        final List<Integer> correct = Lists.newArrayList(27, 68, 68, 59);
        for (int i = 0; i < duration.size(); i++) {
            final int score = SleepScoreUtils.getSleepDurationScore(age, duration.get(i));
            LOGGER.trace("value {} -> {}", duration.get(i), score);
            assertThat(score, is(correct.get(i)));
        }

    }
}
