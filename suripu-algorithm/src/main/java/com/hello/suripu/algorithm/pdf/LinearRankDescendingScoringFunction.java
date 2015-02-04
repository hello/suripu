package com.hello.suripu.algorithm.pdf;

import com.hello.suripu.algorithm.core.ScoringFunction;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by pangwu on 12/16/14.
 */
public class LinearRankDescendingScoringFunction<T> implements ScoringFunction<T, Double> {
    private final double[] cutPercentages;
    private final double startScore;
    private final double endScore;

    public LinearRankDescendingScoringFunction(final double startScore, final double endScore, final double[] cutPercentages){
        this.cutPercentages = new double[]{cutPercentages[0], cutPercentages[1]};
        this.endScore = endScore;
        this.startScore = startScore;

    }

    @Override
    public Map<T, Double> getPDF(final Collection<T> data) {
        final T[] sortedCopy = Arrays.copyOf((T[])data.toArray(), data.size());
        Arrays.sort(sortedCopy);

        final LinkedHashMap<T, Double> rankingPositions = new LinkedHashMap<>();
        final int startCutBound = (int) (data.size() * this.cutPercentages[0]);
        final int endCutBound = (int) (data.size() * this.cutPercentages[1]);

        for(int i = 0; i < sortedCopy.length; i++){
            double score = 0;
            if(i >= startCutBound && i <= endCutBound){
                score = Double.valueOf(endCutBound - i) / (endCutBound - startCutBound) * (this.startScore - this.endScore) + this.endScore;
            }

            final T value = sortedCopy[i];
            if(rankingPositions.containsKey(value)){
                continue;
            }
            rankingPositions.put(value, score);
        }
        return rankingPositions;
    }

    @Override
    public Double getScore(final T data, final Map<T, Double> pdf) {
        if(pdf.containsKey(data)){
            return pdf.get(data);
        }
        return 0d;
    }
}
