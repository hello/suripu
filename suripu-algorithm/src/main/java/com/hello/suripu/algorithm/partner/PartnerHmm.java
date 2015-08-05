package com.hello.suripu.algorithm.partner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.HmmDecodedResult;
import com.hello.suripu.algorithm.hmm.HmmPdfInterface;
import com.hello.suripu.algorithm.hmm.DiscreteAlphabetPdf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;

/**
 * Created by benjo on 6/16/15.
 */
public class PartnerHmm {

    private static final Logger LOGGER = LoggerFactory.getLogger(PartnerHmm.class);


    static final Double MINUTES_ON_BED = 60.0 * 0.5; //rough order of magnitude, this will limit the number of transitions seen
    static final Double DECISION_FRACTION = 0.5;
    static final int NUM_STATES = 7;
    static final int NUM_OBS = 4;
    static final Double PROB_NOT_ON_BED = 0.9998;
    static final Double NOT_ME_PENALTY = 1e-3;
    static final double MIN_LIKELIHOOD = 1e-100;
    static final double NOT_ON_BED_PENALTY = 0.30;


    private static Double getSelfTermFromDuration(final Double durationInMinutes, final int durationOfPeriodInMinutes) {
        Double n = (durationInMinutes + ((double)durationOfPeriodInMinutes/2)) / durationOfPeriodInMinutes;

        //      1 / (1 - a) = T
        //      1/T = 1 - a
        //      1 - 1/T = a


        if (n < 1.0) {
            n = 1.0;
        }

        return 1.0 - 1.0 / n;

    }

    private static double [][] getStateTransitionMatrix(final int numMinutesInPeriod) {
        double [][] A = new double[NUM_STATES][NUM_STATES];

        final double selfTermLong = getSelfTermFromDuration(MINUTES_ON_BED,numMinutesInPeriod);
        final double transitionTermLong3 = (1.0 - selfTermLong) / 3.0;
        final double transitionTermLong2 = (1.0 - selfTermLong) / 2.0;
        final double transitionTermLong1 = (1.0 - selfTermLong) / 1.0;

        //believe it or not, in order for the HMM to work, these rows do not need to add up to 1.0
        //the deal with this?  Okay, so you can
        //
        // go from no on one bed, to myself, you, or both of us on bed
        // go from myself on bed, to no one, or myself.... but I can't go from myself (and only myself) to you, and only you.
        // i.e. we don't sleep in shifts!

         /*

    STATE 0
    not on bed (pre) —>
        I’m on bed (pre) [1]
        You’re on bed (pre) [2]
        We’re on bed [3]

    STATE 1
    I’m on bed (pre) —>
       we’re on bed  [3]
       nobody’s on bed (post) [6]

    STATE 2
    You’re on bed (pre) —>
        we’re on bed [3]
         nobody’s on bed (post) [6]

    STATE 3
    we’re on bed —>
        I’m on bed (post) [4]
        you’re on bed (post) [5]
        nobody’s on bed (post) [6]

    STATE4
    I’m on bed (post) —>
	    nobody’s on bed (post) [6]

    STATE 5
    You’re on bed(post) —>
	    nobody’s on bed (post) [6]

    STATE 6
    nobody’s on bed (post) —>
         */

        A[0][0] = selfTermLong;
        A[0][1] = transitionTermLong3;
        A[0][2] = transitionTermLong3;
        A[0][3] = transitionTermLong3;

        A[1][1] = selfTermLong;
        A[1][3] = transitionTermLong2;
        A[1][6] = transitionTermLong2;

        A[2][2] = selfTermLong;
        A[2][3] = transitionTermLong2;
        A[2][6] = transitionTermLong2;


        A[3][3] = selfTermLong;
        A[3][4] = transitionTermLong3;
        A[3][5] = transitionTermLong3;
        A[3][6] = transitionTermLong3;

        A[4][4] = selfTermLong;
        A[4][6] = transitionTermLong1;

        A[5][5] = selfTermLong;
        A[5][6] = transitionTermLong1;

        A[6][6] = 1.0;

        return A;

    }

    private static  HmmPdfInterface [] getObservationModel() {
        final List<Double> probsOfNobodyOnBed = Lists.newArrayList();
        for (int i = 0; i < NUM_OBS; i++) {
            probsOfNobodyOnBed.add((1.0 - PROB_NOT_ON_BED) / (double)(NUM_STATES - 1));
        }

        probsOfNobodyOnBed.set(0,PROB_NOT_ON_BED);


        /*
          0 - nothing on both
          1 -  mine
          2 -  yours
          3 - ambiguous, but definitely not nothing
         */


        //these numbers are made up
        final List<Double> probsOfMeOnBed = Lists.newArrayList();
        probsOfMeOnBed.add(1.0 - NOT_ON_BED_PENALTY);
        probsOfMeOnBed.add(NOT_ON_BED_PENALTY * (4.0/7.0) - NOT_ME_PENALTY);
        probsOfMeOnBed.add(NOT_ME_PENALTY);
        probsOfMeOnBed.add(NOT_ON_BED_PENALTY * (3.0/7.0));


        final List<Double> probsOfYouOnBed = Lists.newArrayList();
        probsOfYouOnBed.add(1.0 - NOT_ON_BED_PENALTY);
        probsOfYouOnBed.add(NOT_ME_PENALTY);
        probsOfYouOnBed.add(NOT_ON_BED_PENALTY * (4.0/7.0) - NOT_ME_PENALTY);
        probsOfYouOnBed.add(NOT_ON_BED_PENALTY * (3.0/7.0));

        final List<Double> probsItsAParty = Lists.newArrayList();
        probsItsAParty.add(1.0 - NOT_ON_BED_PENALTY);
        probsItsAParty.add(NOT_ON_BED_PENALTY * (2.0/7.0));
        probsItsAParty.add(NOT_ON_BED_PENALTY * (2.0/7.0));
        probsItsAParty.add(NOT_ON_BED_PENALTY * (3.0/7.0));


        final HmmPdfInterface [] obsModels = new HmmPdfInterface[NUM_STATES];

        obsModels[0] = new DiscreteAlphabetPdf(probsOfNobodyOnBed,0);
        obsModels[1] = new DiscreteAlphabetPdf(probsOfMeOnBed,0);
        obsModels[2] = new DiscreteAlphabetPdf(probsOfYouOnBed,0);
        obsModels[3] = new DiscreteAlphabetPdf(probsItsAParty,0);
        obsModels[4] = new DiscreteAlphabetPdf(probsOfMeOnBed,0);
        obsModels[5] = new DiscreteAlphabetPdf(probsOfYouOnBed,0);
        obsModels[6] = new DiscreteAlphabetPdf(probsOfNobodyOnBed,0);

        return obsModels;

    }

    public static class MeasurementPlusDebugInfo {
        public MeasurementPlusDebugInfo(Double diff, Double total, Double frac, Double alphabet) {
            this.diff = diff;
            this.total = total;
            this.frac = frac;
            this.alphabet = alphabet;
        }

        final Double diff;
        final Double total;
        final Double frac;
        final Double alphabet;

    }

    public static MeasurementPlusDebugInfo getMeasurementAsAlphabet(final Double myDuration, final Double partnerDuration) {

         /*
          0 - nothing on both
          1 -  mine
          2 -  yours
          3 - ambiguous, but definitely not nothing
         */

        final Double diff = myDuration - partnerDuration;
        final Double total = myDuration + partnerDuration;

        if (total < 1e-6) {
            return new MeasurementPlusDebugInfo(0.0,0.0,0.0,0.0);
        }

        final Double frac =  diff / total;


        //is fraction of differences significant?
        if (frac > DECISION_FRACTION) {
            return new MeasurementPlusDebugInfo(diff,total,frac,1.0);

        }

        if (frac < -DECISION_FRACTION) {
            return new MeasurementPlusDebugInfo(diff,total,frac,2.0);
        }

        return new MeasurementPlusDebugInfo(diff,total,frac,3.0);


    }

    public ImmutableList<Integer> interpretPath(final ImmutableList<Integer> path) {
        final List<Integer> myMotionBins = Lists.newArrayList();
        //interpret path

        for (int i = 0; i < path.size() - 1; i++) {
            final Integer state = path.get(i + 1);

            if (state.equals(0) || state.equals(2) || state.equals(5) || state.equals(6)) {
                myMotionBins.add(0);
            }
            else {
                myMotionBins.add(1);
            }
        }

        myMotionBins.add(0);

        return ImmutableList.copyOf(myMotionBins);
    }


    public ImmutableList<Integer> decodeSensorData(final Double []  myDurations,final Double []  partnerDurations,final int numMinutesInPeriod) {


        if (partnerDurations.length != myDurations.length) {
            throw new AlgorithmException("partnerDurations size did not match myDurations");
        }

        final double [][] x = new double[1][myDurations.length];

        final double[] fracs = new double[myDurations.length];
        final double[] totals = new double[myDurations.length];

        for (int i = 0; i < myDurations.length; i++) {
            final MeasurementPlusDebugInfo info = getMeasurementAsAlphabet(myDurations[i],partnerDurations[i]) ;
            x[0][i] = info.alphabet;
            fracs[i] = info.frac;
            totals[i] = info.total;
        }



        final double [][] A = getStateTransitionMatrix(numMinutesInPeriod);

        final HmmPdfInterface [] obsModels = getObservationModel();

        final double [] initStateProbs = new double[NUM_STATES];
        initStateProbs[0] = 1.0;

        final HiddenMarkovModel hmm = new HiddenMarkovModel(NUM_STATES,A,initStateProbs,obsModels,0);

        final Integer [] endStates = new Integer[1];
        endStates[0] = NUM_STATES - 1;

        final HmmDecodedResult result = hmm.decode(x, endStates, MIN_LIKELIHOOD);

        LOGGER.debug("meas = {}",x[0]);
       // LOGGER.debug("fracs = {}",fracs);
       // LOGGER.debug("totals = {}",totals);
        LOGGER.debug("path = {}",result.bestPath);

        return result.bestPath;

    }


}
