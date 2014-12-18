package com.hello.suripu.algorithm.sleep.scores;

import com.google.common.collect.Ordering;
import com.hello.suripu.algorithm.core.ScoringFunction;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 12/16/14.
 */
public class MotionScoringFunction implements ScoringFunction<Double, Double> {
    private final double maxPower;
    public MotionScoringFunction(final double maxPower){
        this.maxPower = maxPower;
    }

    @Override
    public Map<Double, Double> getPDF(final Collection<Double> data) {
        final List<Double> sortedCopy = Ordering.natural().immutableSortedCopy(data);

        final LinkedHashMap<Double, Double> rankingPositions = new LinkedHashMap<>();
        for(int i = 0; i < sortedCopy.size(); i++){
            rankingPositions.put(sortedCopy.get(i), Math.pow(Double.valueOf(i) / data.size(), this.maxPower));
        }
        return rankingPositions;
    }

    @Override
    public Double getScore(final Double data, final Map<Double, Double> pdf) {
        if(pdf.containsKey(data)){
            return pdf.get(data);
        }
        return 0d;
    }
}
