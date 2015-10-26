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
public class HiddenMarkovModel {
    static private final double MIN_NORMALIZING = 1e-6;
    public final int numStates;
    final int numFreeParams;
    double [][] A;
    double [] initialState;
    HmmPdfInterface [] obsModels;
    private static final Logger LOGGER = LoggerFactory.getLogger(HiddenMarkovModel.class);

    //not currently used... but maybe soon
    private class AlphaResult {
        public double [][] alpha;
        public double [] c;
    }

    //ctor
    public HiddenMarkovModel(final int numStates, final double [][] A,final double [] initialStateProbs, final HmmPdfInterface [] obsModels, final int numFreeParams) {
        this.numStates = numStates;
        this.A = A;
        this.initialState = initialStateProbs;
        this.obsModels = obsModels;
        this.numFreeParams = numFreeParams; //whatever, not important here since this is only used for test now

        for (int i = 0; i < numStates; i++) {
            LOGGER.info("{}",A[i]);
        }
        LOGGER.info("--------");
    }

    public HiddenMarkovModel(final int numStates,final List<Double> stm,final List<Double> initialProbs,final HmmPdfInterface [] obsModels,final int numFreeParams) {

        this.numStates = numStates;
        this.numFreeParams = numFreeParams; //used for BIC / AIC calculation

        //turn state transition matrix into something we like
        this.A = new double[numStates][numStates];

        int k = 0;
        for (int j = 0; j < numStates; j++) {
            for (int i = 0; i < numStates; i++) {
                this.A[j][i] = stm.get(k);
                k++;
            }
        }

        this.initialState = new double[numStates];
        for (int j = 0; j < numStates; j++) {
            this.initialState[j] = initialProbs.get(j);
        }

        this.obsModels = obsModels;
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

    public HmmDecodedResult decode(final double[][] observations, final Integer[] possibleEndStates) {

        final int numObs = observations[0].length;
        final int [] minStateDurations = new int[numStates];
        Arrays.fill(minStateDurations, 1);

        int j,i,t;

        final double [] scores = new double[numStates];

        final double [][] phi = LogMath.getLogZeroedMatrix(numStates, numObs);
        final int [][] vindices = new int[numStates][numObs];

        double [][] logA = clone2d(this.A); //copy

        //nominal A matrix
        for (j = 0; j < numStates; j++) {
            for (i = 0; i < numStates; i++) {
                logA[j][i] = LogMath.eln(logA[j][i]);
            }
        }


        final double [][] logbmap = getLogBMap(observations);
/*
        LOGGER.info("logbmap = ");
        for (j = 0; j < numStates; j++) {
            LOGGER.info("{}",logbmap[j]);
        }
*/
        final int [] zeta = new int[numStates]; //this is the count for how long you've been in the same state
        //see the paper "Long-term Activities Segmentation using Viterbi Algorithm with a k-minimum-consecutive-states Constraint"
        //by Enrique Garcia-Ceja, Ramon Brena, 2014 WHICH IS WRONG.  I.e. not guaranteed optimal given constraints.  This implementation si bettter.

        for (i = 0; i < numStates; i++) {
            zeta[i] = 1;
        }

        //init
        for (i = 0; i < numStates; i++) {
            phi[i][0] = LogMath.elnproduct(logbmap[i][0], LogMath.eln(initialState[i]));
        }

        for (t = 1; t < numObs; t++) {

            final double [][] logAThisIndex = clone2d(logA);

            for (j = 0; j < numStates; j++) {

                final double obscost = logbmap[j][t];

                for (i = 0; i < numStates; i++) {
                    scores[i] = LogMath.elnproduct(logAThisIndex[i][j], obscost);
                }

                for (i = 0; i < numStates; i++) {
                    scores[i] = LogMath.elnproduct(scores[i], phi[i][t - 1]);
                }

                final List<CostWithIndex> costsWithIndex = Lists.newArrayList();

                for (i = 0; i < numStates; i++) {
                    costsWithIndex.add(new CostWithIndex(i, scores[i]));
                }

                Collections.sort(costsWithIndex);


                //check to see if any of the other possible "from" states (i.e. i != j)
                //are below min. duration.  If so, we must force a transition from that state

                int maxIdx = costsWithIndex.get(0).idx;
                double maxVal = costsWithIndex.get(0).cost;

                for (i = 0; i < numStates; i++) {
                    //not possible, quit
                    final int idx = costsWithIndex.get(i).idx;
                    final double cost = costsWithIndex.get(i).cost;

                    if (cost == Double.NEGATIVE_INFINITY) {
                        break;
                    }

                    if (zeta[idx] < minStateDurations[idx] && idx != j) {
                        maxIdx = idx;
                        maxVal = cost;
                        break;
                    }
                }

                //best path is to stay?  increment zeta.
                if (maxIdx == j) {
                    zeta[j] += 1;
                }
                else {
                    zeta[j] = 1;
                }

                //if zeta of the state I'm coming FROM is above min durations,
                //I'll let the transition happen.  Otherwise, pick the next best state.

                if (maxVal == Double.NEGATIVE_INFINITY) {
                    maxIdx = j;
                }

                phi[j][t] = maxVal;
                vindices[j][t] = maxIdx;

            }
        }


        final int [] path = new int[numObs];


        //go through each path, and find the least cost one.
        //we do this because we are really not sure about which end-state is the best
        final double [] pathCosts = new double[possibleEndStates.length];

        for (i = 0; i < possibleEndStates.length; i++) {
            path[numObs - 1] = possibleEndStates[i];

            //#backtrack to get optimal path
            for (t = numObs - 2; t >= 0; t--) {
                path[t] = vindices[path[t + 1]][t];
            }

            pathCosts[i] = phi[possibleEndStates[i]][phi[0].length - 1];
        }

        double maxScore = pathCosts[0];
        int minIdx = 0;
        for (i = 1; i < possibleEndStates.length; i++) {
            if (pathCosts[i] > maxScore) {
                minIdx = i;
                maxScore = scores[i];
            }
        }

        //recompute minimum path again
        path[numObs - 1] = possibleEndStates[minIdx];
        //#backtrack to get optimal path
        for (t = numObs - 2; t >= 0; t--) {
            path[t] = vindices[path[t + 1]][t];
        }

        final double pathCost = -maxScore;
        final double bic = this.getBIC(pathCost,numObs);
        final double aic = this.getAIC(pathCost);


        final List<Integer> pathObjs = Lists.newArrayList();

        for (t = 0; t < path.length; t++) {
            pathObjs.add(path[t]);
        }

        return new HmmDecodedResult(ImmutableList.copyOf(pathObjs),bic,aic,pathCost);
    }




    //actually this is useless until we do training
    private AlphaResult calcAlpha(int numObs,final double [][] bmap) {

        /*
        Calculates 'alpha' the forward variable.

                The alpha variable is a numpy array indexed by time, then state (TxN).
                alpha[t][i] = the probability of being in state 'i' after observing the
        first t symbols.
        */
        double [][] alpha = new  double[numObs][this.numStates];
        double [] c = new double[numObs];


        // init stage - alpha_1(x) = pi(x)b_x(O1)
        for (int iState = 0; iState < this.numStates; iState++) {
            alpha[0][iState] = this.initialState[iState]*bmap[iState][0];
        }

        // induction
        for (int t = 1; t < numObs; t++) {
            for (int j = 0; j < this.numStates; j++) {
                for (int i = 0; i < this.numStates; i++) {
                    alpha[t][j] += alpha[t - 1][i] * this.A[i][j];
                }
            }

            double sum = 0.0;

            for (int j = 0; j < this.numStates; j++) {
                sum += alpha[t][j];
            }

            if (sum < MIN_NORMALIZING) {
                sum = MIN_NORMALIZING;
            }

            c[t] = sum;

            for (int j = 0; j < this.numStates; j++) {
                alpha[t][j] /= sum;
            }

        }


        AlphaResult res = new AlphaResult();

        res.alpha = alpha;
        res.c = c;

        return res;

    }

    //actually this is useless until we do training in java
    double [][] calcBeta(int numObs,double [] c,final double [][] bmap) {
       /*
        Calculates 'beta' the backward variable.

                The beta variable is a numpy array indexed by time, then state (TxN).
                beta[t][i] = the probability of being in state 'i' and then observing the
        symbols from t+1 to the end (T).
        */

        double [][] beta = new double[numObs][this.numStates];

        for (int s = 0 ; s < this.numStates; s++) {
            beta[numObs-1][s] = 1.0;
        }

        for (int t = numObs - 2; t >= 0; t--) {
            for (int i = 0; i < this.numStates; i++) {
                for (int j = 0; j < this.numStates; j++) {
                    beta[t][i] += this.A[i][j]*bmap[j][t+1]*beta[t+1][j];
                }
            }

            for (int i = 0; i < this.numStates; i++) {
                beta[t][i] /= c[t];
            }
        }

        return beta;
    }

}
