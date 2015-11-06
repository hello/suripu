package com.hello.suripu.algorithm.hmm;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 2/21/15.
 *
 *  serialize/deserialize
 *  viterbi decoding
 *
 */
public class HiddenMarkovModelLogMath implements HiddenMarkovModelInterface {
    public final int numStates;
    final int numFreeParams;
    double [][] A;
    double [] initialState;
    HmmPdfInterface [] obsModels;
    private static final Logger LOGGER = LoggerFactory.getLogger(HiddenMarkovModelLogMath.class);

    //not currently used... but maybe soon
    private class AlphaResult {
        public double [][] alpha;
        public double [] c;
    }

    //ctor
    protected HiddenMarkovModelLogMath(final int numStates, final double [][] A,final double [] initialStateProbs, final HmmPdfInterface [] obsModels, final int numFreeParams) {
        this.numStates = numStates;
        this.A = A;
        this.initialState = initialStateProbs;
        this.obsModels = obsModels;
        this.numFreeParams = numFreeParams; //whatever, not important here since this is only used for test now
    }



    double [][] getLogBMap(final double[][] observations) {
        double [][] logBMap = new double[this.numStates][];

        for (int iState = 0; iState < this.numStates; iState++) {
            logBMap[iState] = this.obsModels[iState].getLogLikelihood(observations);
        }

        return logBMap;
    }



    static private class CostWithIndex implements Comparable<CostWithIndex>{
        final public int idx;

        public CostWithIndex(int idx, double cost) {
            this.idx = idx;
            this.cost = cost;
        }

        final public double cost;


        @Override
        public int compareTo(CostWithIndex o) {
            if (this.cost < o.cost) {
                return 1;
            }

            if (this.cost > o.cost) {
                return -1;
            }

            return 0;
        }
    }


    private double getBIC(double pathCost,int numObs) {
        return 2.0*pathCost + this.numFreeParams*Math.log((double)numObs);
    }

    private double getAIC(double pathCost) {
        return 2.0*pathCost + 2.0*this.numFreeParams;
    }



    private static double [][] clone2d(final double [][] x) {
        double [][] A = new double[x.length][0];
        for (int j = 0; j < x.length; j++) {
            A[j] = x[j].clone();
        }

        return A;
    }


    public HmmDecodedResult decode(final double[][] observations, final Integer[] possibleEndStates, final double minTransitionLikelihood) {

        final int numObs = observations[0].length;

        final double [] scores = new double[numStates];

        final double [][] phi = LogMath.getLogZeroedMatrix(numStates, numObs);
        final int [][] vindices = new int[numStates][numObs];

        double [][] logA = clone2d(this.A); //copy

        //nominal A matrix
        for (int j = 0; j < numStates; j++) {
            for (int i = 0; i < numStates; i++) {
                logA[j][i] = LogMath.eln(logA[j][i] + minTransitionLikelihood);
            }
        }


        final double [][] logbmap = getLogBMap(observations);
/*
        LOGGER.info("logbmap = ");
        for (j = 0; j < numStates; j++) {
            LOGGER.info("{}",logbmap[j]);
        }
*/

        //init
        {
            double maxInit = LogMath.LOGZERO;
            int maxIdx = 0;
            for (int i = 0; i < numStates; i++) {
                phi[i][0] = LogMath.elnproduct(logbmap[i][0], LogMath.eln(initialState[i]));

                if (phi[i][0] > maxInit) {
                    maxInit = phi[i][0];
                    maxIdx = i;
                }
            }

            for (int i = 0; i < numStates; i++) {
                vindices[i][0] = maxIdx;
            }


        }


        for (int t = 1; t < numObs; t++) {

            final double [][] logAThisIndex = clone2d(logA);

            for (int j = 0; j < numStates; j++) {

                final double obscost = logbmap[j][t];

                for (int i = 0; i < numStates; i++) {
                    scores[i] = LogMath.elnproduct(logAThisIndex[i][j], obscost);
                }

                for (int i = 0; i < numStates; i++) {
                    scores[i] = LogMath.elnproduct(scores[i], phi[i][t - 1]);
                }

                final List<CostWithIndex> costsWithIndex = Lists.newArrayList();

                for (int i = 0; i < numStates; i++) {
                    costsWithIndex.add(new CostWithIndex(i, scores[i]));
                }

                Collections.sort(costsWithIndex);


                //check to see if any of the other possible "from" states (i.e. i != j)
                //are below min. duration.  If so, we must force a transition from that state

                final int maxIdx = costsWithIndex.get(0).idx;
                final double maxVal = costsWithIndex.get(0).cost;



                phi[j][t] = maxVal;
                vindices[j][t] = maxIdx;

            }
        }


        final int [] path = new int[numObs];


        //go through each path, and find the least cost one.
        //we do this because we are really not sure about which end-state is the best
        final double [] pathCosts = new double[possibleEndStates.length];

        for (int i = 0; i < possibleEndStates.length; i++) {
            path[numObs - 1] = possibleEndStates[i];

            //#backtrack to get optimal path
            for (int t = numObs - 2; t >= 0; t--) {
                path[t] = vindices[path[t + 1]][t];
            }

            pathCosts[i] = phi[possibleEndStates[i]][phi[0].length - 1];
        }

        double maxScore = pathCosts[0];
        int minIdx = 0;

        for (int i = 1; i < possibleEndStates.length; i++) {
            if (pathCosts[i] > maxScore) {
                minIdx = i;
                maxScore = scores[i];
            }
        }

        //recompute minimum path again
        path[numObs - 1] = possibleEndStates[minIdx];
        //#backtrack to get optimal path
        for (int t = numObs - 2; t >= 0; t--) {
            path[t] = vindices[path[t + 1]][t];

            if (t == 0) {
                int foo = 3;
                foo++;
            }
        }

        final double pathCost = -maxScore;
        final double bic = this.getBIC(pathCost,numObs);
        final double aic = this.getAIC(pathCost);


        final List<Integer> pathObjs = Lists.newArrayList();

        for (int t = 0; t < path.length; t++) {
            pathObjs.add(path[t]);
        }

        return new HmmDecodedResult(ImmutableList.copyOf(pathObjs),bic,aic,pathCost);
    }

    @Override
    public int getNumberOfStates() {
        return numStates;
    }


}
