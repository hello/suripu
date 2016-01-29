package com.hello.suripu.core.trends.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.SleepScore;

import java.util.List;

/**
 * Created by ksg on 01/21/16
 */

public class ConditionRange {
    @JsonProperty("min_value")
    public final float minValue;

    @JsonProperty("max_value")
    public final float maxValue;

    @JsonProperty("condition")
    public final Condition condition;

    public ConditionRange(final float minValue, final float maxValue, final Condition condition) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.condition = condition;
    }

    public static List<ConditionRange> getSleepScoreConditionRanges(final float minValue, final float maxValue) {
        final List<ConditionRange> ranges = Lists.newArrayList();

        if (maxValue < SleepScore.WARNING_SCORE_THRESHOLD ) {
            // all scores are in the ALERT range
            ranges.add(new ConditionRange(SleepScore.ALERT_SCORE_THRESHOLD, SleepScore.WARNING_SCORE_THRESHOLD - 1, Condition.ALERT));
            return ranges;
        }

        if (minValue >= SleepScore.IDEAL_SCORE_THRESHOLD) {
            // All scores are in the IDEAL range
            ranges.add(new ConditionRange(SleepScore.IDEAL_SCORE_THRESHOLD, SleepScore.MAX_SCORE, Condition.IDEAL));
            return ranges;
        }

        if (minValue >= SleepScore.WARNING_SCORE_THRESHOLD && maxValue < SleepScore.IDEAL_SCORE_THRESHOLD) {
            // all scores are in the WARNING range
            ranges.add(new ConditionRange(SleepScore.WARNING_SCORE_THRESHOLD, SleepScore.IDEAL_SCORE_THRESHOLD - 1, Condition.WARNING));
            return ranges;
        }

        if (maxValue >= SleepScore.IDEAL_SCORE_THRESHOLD) {
            // max score in the IDEAL range
            ranges.add(new ConditionRange(SleepScore.IDEAL_SCORE_THRESHOLD, SleepScore.MAX_SCORE, Condition.IDEAL));
        } else if (maxValue >= SleepScore.WARNING_SCORE_THRESHOLD) {
            // max score in WARNING range
            ranges.add(new ConditionRange(SleepScore.WARNING_SCORE_THRESHOLD, SleepScore.IDEAL_SCORE_THRESHOLD - 1, Condition.WARNING));
        }

        if (minValue >= SleepScore.WARNING_SCORE_THRESHOLD ) {
            // min score in WARNING range
            ranges.add(new ConditionRange(SleepScore.WARNING_SCORE_THRESHOLD, SleepScore.IDEAL_SCORE_THRESHOLD - 1, Condition.WARNING));
        } else {
            // min score below the WARNING range
            ranges.add(new ConditionRange(SleepScore.ALERT_SCORE_THRESHOLD, SleepScore.WARNING_SCORE_THRESHOLD - 1, Condition.ALERT));
        }

        return ranges;
    }
}
