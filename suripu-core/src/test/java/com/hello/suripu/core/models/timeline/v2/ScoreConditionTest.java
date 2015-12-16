package com.hello.suripu.core.models.timeline.v2;

import com.hello.suripu.core.models.DataCompleteness;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jakepiccolo on 12/14/15.
 */
public class ScoreConditionTest {

    @Test
    public void testFromScoreNoData() {
        assertThat(ScoreCondition.fromScore(60, DataCompleteness.NO_DATA), is(ScoreCondition.NO_DATA));
        assertThat(ScoreCondition.fromScore(100, DataCompleteness.NO_DATA), is(ScoreCondition.NO_DATA));
        assertThat(ScoreCondition.fromScore(0, DataCompleteness.NO_DATA), is(ScoreCondition.NO_DATA));
    }

    @Test
    public void testFromScoreNotEnoughData() {
        assertThat(ScoreCondition.fromScore(60, DataCompleteness.NOT_ENOUGH_DATA), is(ScoreCondition.INCOMPLETE));
        assertThat(ScoreCondition.fromScore(100, DataCompleteness.NOT_ENOUGH_DATA), is(ScoreCondition.INCOMPLETE));
        assertThat(ScoreCondition.fromScore(0, DataCompleteness.NOT_ENOUGH_DATA), is(ScoreCondition.INCOMPLETE));
    }

    @Test
    public void testFromScoreUnavailable() {
        assertThat(ScoreCondition.fromScore(0, DataCompleteness.ENOUGH_DATA), is(ScoreCondition.UNAVAILABLE));
    }

    @Test
    public void testFromScoreAlert() {
        assertThat(ScoreCondition.fromScore(1, DataCompleteness.ENOUGH_DATA), is(ScoreCondition.ALERT));
        assertThat(ScoreCondition.fromScore(10, DataCompleteness.ENOUGH_DATA), is(ScoreCondition.ALERT));
        assertThat(ScoreCondition.fromScore(50, DataCompleteness.ENOUGH_DATA), is(ScoreCondition.ALERT));
        assertThat(ScoreCondition.fromScore(59, DataCompleteness.ENOUGH_DATA), is(ScoreCondition.ALERT));
    }

    @Test
    public void testFromScoreWarning() {
        assertThat(ScoreCondition.fromScore(60, DataCompleteness.ENOUGH_DATA), is(ScoreCondition.WARNING));
        assertThat(ScoreCondition.fromScore(79, DataCompleteness.ENOUGH_DATA), is(ScoreCondition.WARNING));
    }

    @Test
    public void testFromScoreIdeal() {
        assertThat(ScoreCondition.fromScore(80, DataCompleteness.ENOUGH_DATA), is(ScoreCondition.IDEAL));
        assertThat(ScoreCondition.fromScore(89, DataCompleteness.ENOUGH_DATA), is(ScoreCondition.IDEAL));
        assertThat(ScoreCondition.fromScore(99, DataCompleteness.ENOUGH_DATA), is(ScoreCondition.IDEAL));
        assertThat(ScoreCondition.fromScore(100, DataCompleteness.ENOUGH_DATA), is(ScoreCondition.IDEAL));
    }
}