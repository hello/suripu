package com.hello.suripu.algorithm.sleep;

import com.google.common.base.Optional;
import com.hello.suripu.algorithm.pdf.LinearRankAscendingScoringFunction;
import com.hello.suripu.algorithm.pdf.LinearRankDescendingScoringFunction;
import com.hello.suripu.algorithm.pdf.RankPowerScoringFunction;
import com.hello.suripu.algorithm.sleep.InternalScore;
import com.hello.suripu.algorithm.sleep.MotionScoreAlgorithm;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Created by pangwu on 12/14/14.
 */
public class MotionScoreAlgorithmTest {

    @Test
    public void testLinearScoringFunction(){
        final List<Long> tsArray = new ArrayList<>();
        final DateTime now = DateTime.now();
        tsArray.add(now.getMillis());
        tsArray.add(now.plusMinutes(1).getMillis());
        tsArray.add(now.plusMinutes(2).getMillis());
        final LinearRankAscendingScoringFunction linearRankAscendingScoringFunction = new LinearRankAscendingScoringFunction(0d, 1d, new double[]{0d, 1d});

        Map<Long, Double> rankingMap = linearRankAscendingScoringFunction.getPDF(tsArray);
        assertThat(rankingMap.size(), is(3));
        assertThat(rankingMap.get(now.getMillis()), is(0d));
        assertThat(rankingMap.get(now.plusMinutes(1).getMillis()), is(1d / tsArray.size()));
        assertThat(rankingMap.get(now.plusMinutes(2).getMillis()), is(2d / tsArray.size()));
        assertThat(rankingMap.get(0L), is(nullValue()));

        // Test order by DESC
        final LinearRankDescendingScoringFunction linearRankDescendingScoringFunction = new LinearRankDescendingScoringFunction(1d, 0, new double[]{0d, 1d});
        rankingMap = linearRankDescendingScoringFunction.getPDF(tsArray);
        assertThat(rankingMap.size(), is(3));
        assertThat(rankingMap.get(now.getMillis()), is(3d / tsArray.size()));
        assertThat(rankingMap.get(now.plusMinutes(1).getMillis()), is(2d / tsArray.size()));
        assertThat(rankingMap.get(now.plusMinutes(2).getMillis()), is(1d / tsArray.size()));

        assertThat(rankingMap.get(0L), is(nullValue()));

    }

    @Test
    public void testGetHighestScore(){
        final ArrayList<InternalScore> scores = new ArrayList<>();
        for(int i = 0; i < 3; i++){
            scores.add(new InternalScore(i, Double.valueOf(i)));
        }

        final Optional<InternalScore> highestScore = MotionScoreAlgorithm.getHighestScore(scores);
        assertThat(highestScore.isPresent(), is(true));
        assertThat(highestScore.get().score, is(Double.valueOf(scores.size() - 1)));


        final ArrayList<InternalScore> emptyScores = new ArrayList<>();
        assertThat(MotionScoreAlgorithm.getHighestScore(emptyScores).isPresent(), is(false));
    }

    @Test
    public void testGetScoreFromMotionPolyPDF(){
        final List<Double> ampArray = new ArrayList<>();
        final Double startAmplitude = 1d;
        ampArray.add(startAmplitude);
        ampArray.add(startAmplitude + 1);
        ampArray.add(startAmplitude + 2);

        final RankPowerScoringFunction rankPowerScoringFunction = new RankPowerScoringFunction(10);
        Map<Double, Double> pdf = rankPowerScoringFunction.getPDF(ampArray);

        // We should expect a linear result
        assertThat(pdf.get(startAmplitude),
                is(Math.pow(0, 10)));
        assertThat(pdf.get(startAmplitude + 1),
                is(Math.pow(1d / 3, 10)));
        assertThat(pdf.get(startAmplitude + 2),
                is(Math.pow(2d / 3, 10)));
    }

}
