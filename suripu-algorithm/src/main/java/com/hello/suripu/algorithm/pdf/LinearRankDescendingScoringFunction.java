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
public class LinearRankDescendingScoringFunction implements ScoringFunction<Long, Double> {
    @Override
    public Map<Long, Double> getPDF(final Collection<Long> data) {

        final List<Long> sortedCopy = Ordering.natural().reverse().immutableSortedCopy(data);

        final LinkedHashMap<Long, Double> rankingPositions = new LinkedHashMap<>();
        final int dataSize = data.size();
        for(int i = 0; i < sortedCopy.size(); i++){
            final Long value = sortedCopy.get(i);
            if(rankingPositions.containsKey(value)){
                continue;
            }
            rankingPositions.put(value, Double.valueOf(i) / dataSize);
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
