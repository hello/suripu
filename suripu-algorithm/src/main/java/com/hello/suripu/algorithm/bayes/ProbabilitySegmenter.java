package com.hello.suripu.algorithm.bayes;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.hmm.BetaPdf;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.HmmDecodedResult;
import com.hello.suripu.algorithm.hmm.HmmPdfInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Created by benjo on 6/24/15.
 */
public class ProbabilitySegmenter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProbabilitySegmenter.class);
    private static Integer SEGMENT_STATE_FOR_HIGH_PROBABILITY = 1;
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

    //find the sections where prob ~ 1.0
    public static Optional<ProbabilitySegment> getBestSegment(final int expectedNumberOfSamplesBeforeTrue, final int expectedNumberOfSamplesDuringTrue, final int expectedNumberOfSamplesAfterTrue,final List <Double> probs) {
        // nsamples = 1 / (1 - a)
        // 1 - 1 / nsamples = a

        final double preA = 1.0  -  1.0 / (double)expectedNumberOfSamplesBeforeTrue;
        final double duringA = 1.0  -  1.0 / (double)expectedNumberOfSamplesDuringTrue;
        final double postA = 1.0  -  1.0 / (double)expectedNumberOfSamplesAfterTrue;

        final double [][] A = {{preA, 1.0 - preA, 0.0},{0.0,duringA,1.0 - duringA},{0.0,0.0,postA}};

        //this really tries to find stuff closer to 1.0, so it's biased that way I guess
        final HmmPdfInterface preObsModel = new BetaPdf(1.0,10.0,0);
        final HmmPdfInterface duringObsModel = new BetaPdf(40.0,2.0,0); //see, peak is really close to 1.0
        final HmmPdfInterface postObsModel = new BetaPdf(2.0,4.0,0);
        final HmmPdfInterface [] obsModels = {preObsModel,duringObsModel,postObsModel};
        final double [] pi = {1.0,0.0,0.0};

        final HiddenMarkovModel segmentingHiddenMarkovModel = new HiddenMarkovModel(3,A,pi,obsModels,6);

        final Integer [] possibleEndStates = {2};

        final double [][] meas = new double[1][probs.size()];
        final double [] vec = meas[0];
        for (int t = 0; t < probs.size(); t++) {
            vec[t] = probs.get(t);
        }

        HmmDecodedResult res = segmentingHiddenMarkovModel.decode(meas, possibleEndStates,1e-100);
        LOGGER.debug("probs = {}",probs);
        LOGGER.debug("segment = {}",res.bestPath);

        final List<ProbabilitySegment> segments = Lists.newArrayList();

        int i1 = 0;
        int i2 = 0;
        boolean foundFirst = false;
        for (int t = 0; t < res.bestPath.size(); t++) {

            if (res.bestPath.get(t).equals(SEGMENT_STATE_FOR_HIGH_PROBABILITY)) {
                if (!foundFirst) {
                    foundFirst = true;
                    i1 = t;
                }
            }
            else {

                if (foundFirst) {
                    foundFirst = false;
                    i2 = t - 1;
                    segments.add(new ProbabilitySegment(i1,i2,""));
                }
            }
        }

        if (segments.isEmpty()) {
            LOGGER.info("found no segments");
            return Optional.absent();
        }

        ProbabilitySegment bestSeg = segments.get(0);

        if (segments.size() > 1) {
            int maxDuration = 0;
            final List<Integer> durations = Lists.newArrayList();
            for (final ProbabilitySegment segment : segments) {
                durations.add(segment.getDuration());
            }

            for (final ProbabilitySegment segment : segments) {
                if (maxDuration < segment.getDuration()) {
                    bestSeg = segment;
                    maxDuration = segment.getDuration();
                }
            }

            LOGGER.info("found more than one segment, duration = {}, going to use the longest segment of duration {}",durations,maxDuration);

        }

        return Optional.of(bestSeg);
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
