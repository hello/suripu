package com.hello.suripu.algorithm.bayes;

import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.core.Segment;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Created by benjo on 6/24/15.
 */
public class ProbabilitySegmenter {
    private static Comparator<Double> aboveComparator = new Comparator<Double>() {
        @Override
        public int compare(Double o1, Double o2) {
            if (o1 > o2) {
                return 1;
            }

            if (o1 < o2) {
                return -1;
            }

            return 0;
        }
    };

    private static Comparator<Double> belowComparator = new Comparator<Double>() {
        @Override
        public int compare(Double o1, Double o2) {
            if (o1 < o2) {
                return 1;
            }

            if (o1 > o2) {
                return -1;
            }

            return 0;
        }
    };

    public static List<ProbabilitySegment> getSegmentsFromProbabilitySignal(final List <Double> probs, final double stateThreshold, boolean aboveThreshold) {
        return getSegmentsFromProbabilitySignal(probs,stateThreshold,aboveThreshold,"");
    }

    public static List<ProbabilitySegment> getSegmentsFromProbabilitySignal(final List <Double> probs, final double stateThreshold, boolean aboveThreshold, String tag) {

        Comparator<Double> comparator = null;
        if (aboveThreshold) {
            comparator = aboveComparator;
        }
        else {
            comparator = belowComparator;
        }

        boolean valid = false;

        final List<ProbabilitySegment> segments = Lists.newArrayList();

        int iBegin = 0;

        for (int i = 0; i < probs.size(); i++) {
            final double p = probs.get(i);

            if (comparator.compare(p,stateThreshold) >= 0) {

                if (!valid) {
                    iBegin = i;
                }

                valid = true;
            }
            else {
                if (valid) {
                    segments.add(new ProbabilitySegment(iBegin,i - 1,tag));
                }

                valid = false;
            }

        }

        return segments;
    }

}
