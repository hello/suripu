package com.hello.suripu.algorithm.hmm;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by benjo on 2/21/15.
 *
 *  serialize/deserialize
 *  viterbi decoding
 *
 */
public class HiddenMarkovModel implements HiddenMarkovModelInterface {
    static public final double MIN_LIKELIHOOD = 1e-15;

    final int numStates;
    final int numFreeParams;
    double [][] A;
    double [] initialState;
    HmmPdfInterface [] obsModels;

    //not currently used... but maybe soon
    private class AlphaResult {
        public double [][] alpha;
        public double [] c;
    }

    //ctor
    protected HiddenMarkovModel(final int numStates, final double [][] A,final double [] initialStateProbs, final HmmPdfInterface [] obsModels, final int numFreeParams) {
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

    static int findMinIndexOfDoubleArray(double [] array) {
        double theMinimum = array[0];
        int minIdx = 0;
        for (int i = 1; i < array.length; i++) {
            final double val = array[i];
            if (val < theMinimum) {
                theMinimum = val;
                minIdx = i;
            }
        }

        return minIdx;
    }

    protected static double getPathCost(final int [] path, final double [][] phi) {
        double cost = 0.0;
        for (int t = 0; t < path.length; t++) {
            cost += phi[path[t]][t];
        }

        return cost;
    }




    private double getBIC(double pathCost,int numObs) {
        return 2.0*pathCost + this.numFreeParams*Math.log((double)numObs);
    }

    private double getAIC(double pathCost) {
        return 2.0*pathCost + 2.0*this.numFreeParams;
    }
    public HmmDecodedResult decode(final double[][] observations, final Integer[] possibleEndStates) {
        return decode(observations,possibleEndStates,MIN_LIKELIHOOD);
    }

    public HmmDecodedResult decode(final double[][] observations, final Integer[] possibleEndStates, final double minLikelihood) {
        /*

        returns optimal path given observations and state transition matrix "A"

        Find the best state sequence (path) using viterbi algorithm - a method of dynamic programming,
        very similar to the forward-backward algorithm, with the added step of maximization and eventual
        backtracing.

                delta[t][i] = max(P[q1..qt=i,O1...Ot|model] - the path ending in Si and until time t,
        that generates the highest probability.

                phi[t][i] = argmin(delta[t-1][i]*aij) - the index of the maximizing state in time (t-1),
                i.e: the previous state.
        */


        // similar to the forward-backward algorithm, we need to make sure that we're using fresh data for the given observations.

        final int numObs = observations[0].length;
        final double [][] logBMap = this.getLogBMap(observations);

        final double [][] phi = new double[this.numStates][numObs];
        final int [][] viterbiIndices = new int[this.numStates][numObs];


        // init
        for (int i = 0; i < this.numStates; i++) {
            phi[i][0] = -Math.log(this.initialState[i]+ minLikelihood) - logBMap[i][0];
        }

        //find minimum cost
        {
            double themin = Double.POSITIVE_INFINITY;
            int minidx = 0;
            for (int i = 0; i < this.numStates; i++) {
                if (phi[i][0] < themin) {
                    themin = phi[i][0];
                    minidx = i;
                }
            }



            //assign minimum cost index to first Viterbi state
            for (int i = 0; i < this.numStates; i++) {
                viterbiIndices[i][0] = minidx;
            }
        }


        //do viterbi
        final double [] cost = new double[this.numStates];

        for (int t = 1; t < numObs; t++) {
            for (int j = 0; j < this.numStates; j++) {
                //#"j" mean THIS (the jth) hidden state
                final double obscost = -logBMap[j][t];
                for (int i = 0; i < this.numStates; i++) {
                   cost[i] = -Math.log(this.A[i][j] + minLikelihood) + obscost;
                }

                for (int i = 0; i < this.numStates; i++) {
                    cost[i] += phi[i][t-1];
                }

                final int minidx = findMinIndexOfDoubleArray(cost);
                final double minval = cost[minidx];

                phi[j][t] = minval;

                viterbiIndices[j][t] = minidx;

            }
        }

        final int [] path = new int[numObs];


        //go through each path, and find the least cost one.
        //we do this because we are really not sure about which end-state is the best
        final double [] costs = new double[possibleEndStates.length];

        for (int i = 0; i < possibleEndStates.length; i++) {
            path[numObs - 1] = possibleEndStates[i];

            //#backtrack to get optimal path
            for (int t = numObs - 2; t >= 0; t--) {
                path[t] = viterbiIndices[path[t + 1]][t];
            }

            costs[i] = getPathCost(path,phi);
        }

        double minCost = costs[0];
        int minIdx = 0;
        for (int i = 1; i < possibleEndStates.length; i++) {
            if (costs[i] < minCost) {
                minIdx = i;
                minCost = costs[i];
            }
        }

        //recompute minimum path again
        path[numObs - 1] = possibleEndStates[minIdx];
        //#backtrack to get optimal path
        for (int t = numObs - 2; t >= 0; t--) {
            path[t] = viterbiIndices[path[t + 1]][t];
        }

        //convert from primitive to collection
        final ArrayList<Integer> bestPath = new ArrayList<>();

        for (int i = 0; i < numObs; i++) {
            bestPath.add(path[i]);
        }


        final double bic = this.getBIC(minCost, numObs);
        final double aic = this.getAIC(minCost);


        return new HmmDecodedResult(ImmutableList.copyOf(bestPath),bic,aic,minCost);

    }

    @Override
    public int getNumberOfStates() {
        return numStates;
    }


}
