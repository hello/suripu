package com.hello.suripu.core.util;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        final int sleepDurationMinutes[] = new int[] {420, 450, 480, 510, 540};

        final int values[] = new int[] {1179, 1570};
        final int correct[] = new int[] {171, 211};
        for (int i = 0; i < sleepDurationMinutes.length; i++) {
            final int score = SleepScoreUtils.getSleepDurationScore(age, sleepDurationMinutes[i]);
            LOGGER.debug("value {} -> {}", sleepDurationMinutes[i], score);
            assertThat(score, is(SleepScoreUtils.DURATION_MAX_SCORE));
        }
    }

    @Test
    public void testTerribleScores() {
        final int ages[] = new int[] {16, 23, 27, 68};
        final int duration[] = new int[] {330, 299, 299, 790};
        for (int i = 0; i < duration.length; i++) {
            final int score = SleepScoreUtils.getSleepDurationScore(ages[i], duration[i]);
            LOGGER.debug("value {} -> {}", duration[i], score);
            assertThat(score, is(SleepScoreUtils.DURATION_MIN_SCORE));
        }
    }

    @Test
    public void testRandomScores() {
        final int age = 23;
        final int duration[] = new int[] {330, 400, 600, 650};
        final int correct[] = new int[] {31, 81, 81, 69};
        for (int i = 0; i < duration.length; i++) {
            final int score = SleepScoreUtils.getSleepDurationScore(age, duration[i]);
            LOGGER.debug("value {} -> {}", duration[i], score);
            assertThat(score, is(correct[i]));
        }

    }
}
