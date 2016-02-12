package com.hello.suripu.core.util;

import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.Insights.DowSample;
import com.hello.suripu.core.models.Insights.TrendGraph;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.SleepStats;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by kingshy on 2/25/15.
 */
public class TrendGraphUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrendGraphUtilsTest.class);

    private List<AggregateSleepStats> getValues(final DateTime now) {
        final int offsetMillis = -25200000;
        final String version = "0.0.2";

        final MotionScore motionScore = new MotionScore(0, 0, 1.0f, 0, 0);
        final List<AggregateSleepStats> stats = new ArrayList<>();
        stats.add(new AggregateSleepStats(1L, now, offsetMillis, 80, version, motionScore,
                new SleepStats(300, 0, 200, 400, false, 10, 0L, 0L, 0)));
        stats.add(new AggregateSleepStats(1L, now.minusDays(1), offsetMillis, 75, version, motionScore,
                new SleepStats(300, 0, 200, 410, false, 10, 0L, 0L, 0)));
        stats.add(new AggregateSleepStats(1L, now.minusDays(2), offsetMillis, 70, version, motionScore,
                new SleepStats(300, 0, 200, 420, false, 10, 0L, 0L, 0)));
        stats.add(new AggregateSleepStats(1L, now.minusDays(3), offsetMillis, 65, version, motionScore,
                new SleepStats(300, 0, 200, 430, false, 10, 0L, 0L, 0)));
        stats.add(new AggregateSleepStats(1L, now.minusDays(4), offsetMillis, 60, version, motionScore,
                new SleepStats(300, 0, 200, 440, false, 10, 0L, 0L, 0)));
        stats.add(new AggregateSleepStats(1L, now.minusDays(5), offsetMillis, 55, version, motionScore,
                new SleepStats(300, 0, 200, 450, false, 10, 0L, 0L, 0)));
        stats.add(new AggregateSleepStats(1L, now.minusDays(6), offsetMillis, 50, version, motionScore,
                new SleepStats(300, 0, 200, 460, false, 10, 0L, 0L, 0)));
        stats.add(new AggregateSleepStats(1L, now.minusDays(7), offsetMillis, 45, version, motionScore,
                new SleepStats(300, 0, 200, 470, false, 10, 0L, 0L, 0)));
        return stats;
    }

    @Test
    public void getScoresDOW() {
        final DateTime now = DateTime.now().withTimeAtStartOfDay();
        final int startDOW = now.getDayOfWeek();

        final List<AggregateSleepStats> stats = getValues(now);
        final int size = stats.size();
        final float scoreCheckValue = (stats.get(0).sleepScore + stats.get(size-1).sleepScore) / 2.0f;

        final List<DowSample> dow = TrendGraphUtils.aggregateDOWData(stats, TrendGraph.DataType.SLEEP_SCORE);
        assertThat(dow.get(startDOW - 1).value, is(scoreCheckValue));
    }

    @Test
    public void lessScores() {
        final DateTime now = DateTime.now().withTimeAtStartOfDay();
        final int startDOW = now.getDayOfWeek();

        final List<AggregateSleepStats> stats = getValues(now);

        final int size = stats.size();
        stats.remove(size - 1);
        stats.remove(size - 2);

        final float scoreCheckValue = (float) stats.get(0).sleepScore;

        final List<DowSample> dow = TrendGraphUtils.aggregateDOWData(stats, TrendGraph.DataType.SLEEP_SCORE);
        assertThat(dow.get(startDOW - 1).value, is(scoreCheckValue));
    }

    @Test
    public void getDurationDOW() {
        final DateTime now = DateTime.now().withTimeAtStartOfDay();
        final int startDOW = now.getDayOfWeek();

        final List<AggregateSleepStats> stats = getValues(now);
        final int size = stats.size();
        final float durationCheckValue = (stats.get(0).sleepStats.sleepDurationInMinutes +
                stats.get(size-1).sleepStats.sleepDurationInMinutes) / 2.0f;

        final List<DowSample> dow = TrendGraphUtils.aggregateDOWData(stats, TrendGraph.DataType.SLEEP_DURATION);
        assertThat(dow.get(startDOW - 1).value, is(durationCheckValue));
    }
}
