package com.hello.suripu.algorithm.partner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.HmmDecodedResult;
import com.hello.suripu.algorithm.hmm.HmmPdfInterface;
import com.hello.suripu.algorithm.hmm.DiscreteAlphabetPdf;


import java.util.List;

/**
 * Created by benjo on 6/16/15.
 */
public class PartnerHmm {
    static final Double MINUTES_ON_BED = 60.0 * 4.0; //rough order of magnitude, this will limit the number of transitions seen
    static final Double SLOP_FACTOR = 3.0;
    static final Double DECISION_FRACTION = 0.2;
    static final int NUM_STATES = 4;
    static final double MIN_LIKELIHOOD = 1e-100;


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

        final double selfTerm = getSelfTermFromDuration(MINUTES_ON_BED,numMinutesInPeriod);
        final double transitionTerm3 = (1.0 - selfTerm) / 3.0;

        //believe it or not, in order for the HMM to work, these rows do not need to add up to 1.0
        //the deal with this?  Okay, so you can
        //
        // go from no on one bed, to myself, you, or both of us on bed
        // go from myself on bed, to no one, or myself.... but I can't go from myself (and only myself) to you, and only you.
        // i.e. we don't sleep in shifts!

         /*
            Meaning of states
            0 - no one is on bed
            1 - I'm on bed, but you are not
            2 - you're on bed, but I'm not
            3 - we both are on bed
         */

        A[0][0] = selfTerm;
        A[0][1] = transitionTerm3;
        A[0][2] = transitionTerm3;
        A[0][3] = transitionTerm3;

        A[1][0] = transitionTerm3;
        A[1][1] = selfTerm;
        A[1][3] = transitionTerm3;

        A[2][0] = transitionTerm3;
        A[2][2] = selfTerm;
        A[2][3] = transitionTerm3;

        A[3][0] = transitionTerm3;
        A[3][1] = transitionTerm3;
        A[3][2] = transitionTerm3;
        A[3][3] = selfTerm;

        return A;

    }

    private static  HmmPdfInterface [] getObservationModel() {
        final List<Double> probsOfNobodyOnBed = Lists.newArrayList();
        for (int i = 0; i < NUM_STATES; i++) {
            probsOfNobodyOnBed.add(MIN_LIKELIHOOD);
        }

        probsOfNobodyOnBed.set(0,1.0);


        /*
          0 - nothing on both
          1 - only mine
          2 - only yours
          3 - significantly mine
          4 - significantly yours
          5 - about the same
         */


        //these numbers are made up
        final List<Double> probsOfMeOnBed = Lists.newArrayList();
        probsOfMeOnBed.add(0.8);
        probsOfMeOnBed.add(0.07);
        probsOfMeOnBed.add(0.01);
        probsOfMeOnBed.add(0.07);
        probsOfMeOnBed.add(0.02);
        probsOfMeOnBed.add(0.01);




        final List<Double> probsOfYouOnBed = Lists.newArrayList();
        probsOfYouOnBed.add(0.8);
        probsOfYouOnBed.add(0.01);
        probsOfYouOnBed.add(0.07);
        probsOfYouOnBed.add(0.02);
        probsOfYouOnBed.add(0.07);
        probsOfYouOnBed.add(0.01);


        final List<Double> probsItsAParty = Lists.newArrayList();
        probsItsAParty.add(0.8);
        probsItsAParty.add(0.01);
        probsItsAParty.add(0.01);
        probsItsAParty.add(0.04);
        probsItsAParty.add(0.04);
        probsItsAParty.add(0.10);


        final HmmPdfInterface [] obsModels = new HmmPdfInterface[NUM_STATES];

        obsModels[0] = new DiscreteAlphabetPdf(probsOfNobodyOnBed,0);
        obsModels[1] = new DiscreteAlphabetPdf(probsOfMeOnBed,0);
        obsModels[2] = new DiscreteAlphabetPdf(probsOfYouOnBed,0);
        obsModels[3] = new DiscreteAlphabetPdf(probsItsAParty,0);

        return obsModels;

    }

    private static Double getMeasurementAsAlphabet(final Double myDuration, final Double partnerDuration) {

        /*
          0 - nothing on both
          1 - only mine
          2 - only yours
          3 - significantly mine
          4 - significantly yours
          5 - about the same
         */

        if (myDuration.equals(0.0) && partnerDuration.equals(0.0)) {
            return 0.0;
        }
        else if (partnerDuration.equals(0.0)) {
            return 1.0;
        }
        else if (myDuration.equals(0.0)) {
            return 2.0;
        }

        final Double diff = myDuration - partnerDuration;
        final Double total = myDuration + partnerDuration + SLOP_FACTOR;
        final Double frac =  diff / total;

        /*  mine is 3.0, yours is 2.0
        *
        *   diff = 1.0
        *   total = 5.0 + slop_factor
        *
        *   1.0 / (5.0 + 1.0) > 0.2?  No.  they're the same.
        *
        * */

        //is fraction of differences significant?
        if (frac > DECISION_FRACTION) {
            return 3.0;
        }

        if (frac < -DECISION_FRACTION) {
            return 4.0;
        }

        return 5.0;

    }



    public ImmutableList<Integer> decodeSensorData(final Double []  myDurations,final Double []  partnerDurations,final int numMinutesInPeriod) {


        if (partnerDurations.length != myDurations.length) {
            throw new AlgorithmException("partnerDurations size did not match myDurations");
        }

        final double [][] x = new double[1][myDurations.length];

        for (int i = 0; i < myDurations.length; i++) {
            x[0][i] = getMeasurementAsAlphabet(myDurations[i],partnerDurations[i]) ;
        }


        final double [][] A = getStateTransitionMatrix(numMinutesInPeriod);

        final HmmPdfInterface [] obsModels = getObservationModel();

        final double [] initStateProbs = {1.0,0.0,0.0,0.0};

        final HiddenMarkovModel hmm = new HiddenMarkovModel(NUM_STATES,A,initStateProbs,obsModels,0);

        final Integer [] endStates = new Integer[1];
        endStates[0] = 0;

        final HmmDecodedResult result = hmm.decode(x, endStates, MIN_LIKELIHOOD);

        final List<Integer> myMotionBins = Lists.newArrayList();
        //interpret path
        for (int i = 0; i < result.bestPath.size(); i++) {
            final Integer state = result.bestPath.get(i);

            if (state.equals(0) || state.equals(2)) {
                myMotionBins.add(0);
            }
            else {
                myMotionBins.add(1);
            }
        }

        return ImmutableList.copyOf(myMotionBins);
    }


}
