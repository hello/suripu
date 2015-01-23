package com.hello.suripu.algorithm.pdf;

import com.google.common.collect.Ordering;
import com.hello.suripu.algorithm.core.ScoringFunction;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 12/16/14.
 */
public class LinearRankAscendingScoringFunction implements ScoringFunction<Long, Double> {

    private final double[] cutPercentages;
    private final double startScore;
    private final double endScore;

    public LinearRankAscendingScoringFunction(final double startScore, final double endScore, final double[] cutPercentages){
        this.cutPercentages = new double[]{cutPercentages[0], cutPercentages[1]};
        this.endScore = endScore;
        this.startScore = startScore;

    }
    @Override
    public Map<Long, Double> getPDF(final Collection<Long> data) {
        List<Long> sortedCopy = Ordering.natural().immutableSortedCopy(data);

        final LinkedHashMap<Long, Double> rankingPositions = new LinkedHashMap<>();
        final double startCutBound = data.size() * this.cutPercentages[0];
        final double endCutBound = data.size() * this.cutPercentages[1];

        final int dataSize = data.size();
        for(int i = 0; i < sortedCopy.size(); i++){
            double score = 0;
            if(i >= startCutBound && i <= endCutBound){
                score = Double.valueOf(i - startCutBound) / (endCutBound - startCutBound) * (this.endScore - this.startScore) + this.startScore;
            }

            final Long value = sortedCopy.get(i);
            if(rankingPositions.containsKey(value)){
                continue;
            }
            rankingPositions.put(value, score);
        }
        return rankingPositions;
    }

    @Override
    public Double getScore(final Long data, final Map<Long, Double> pdf) {
        if(pdf.containsKey(data)){
            return pdf.get(data);
        }
        return 0d;
    }
}
