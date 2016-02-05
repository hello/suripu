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

    public static List<ConditionRange> SLEEP_SCORE_CONDITION_RANGES = Lists.newArrayList(
            new ConditionRange(SleepScore.IDEAL_SCORE_THRESHOLD, SleepScore.MAX_SCORE, Condition.IDEAL),
            new ConditionRange(SleepScore.WARNING_SCORE_THRESHOLD, SleepScore.IDEAL_SCORE_THRESHOLD - 1, Condition.WARNING),
            new ConditionRange(SleepScore.ALERT_SCORE_THRESHOLD, SleepScore.WARNING_SCORE_THRESHOLD - 1, Condition.ALERT)
    );
}
