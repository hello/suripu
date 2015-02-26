package com.hello.suripu.algorithm.hmm;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.List;

/**
 * Created by benjo on 2/21/15.
 *
 *  serialize/deserialize
 *  viterbi decoding
 *
 */
public class HiddenMarkovModel {
    static private final double MIN_NORMALIZING = 1e-6;
    final int numStates;
    double [][] A;
    double [] initialState;
    HmmPdfInterface [] obsModels;


    public static HiddenMarkovModel createPoissonOnlyModel(double [][] A, double [] initialStateProbs, double [][] poissonMeans) {
        final int numStates = A.length;
        final int numObsModelsPerState = poissonMeans[0].length;

        HmmPdfInterface [] models = new HmmPdfInterface[numStates];

        for (int j = 0; j < numStates; j++) {
            PdfComposite c = new PdfComposite();
            for (int i = 0; i < numObsModelsPerState; i++) {
                c.addPdf(new PoissonPdf(poissonMeans[j][i],i));
            }

            models[j] = c;

        }

        return new HiddenMarkovModel(numStates,A,initialStateProbs,models);
    }

    //not currently used... but maybe soon
    private class AlphaResult {
        public double [][] alpha;
        public double [] c;
    }

    //ctor
    private HiddenMarkovModel(int numStates, double [][] A,double [] initialStateProbs, HmmPdfInterface [] obsModels) {
        this.numStates = numStates;
        this.A = A;
        this.initialState = initialStateProbs;
        this.obsModels = obsModels;
    }

    public HiddenMarkovModel(final int numStates,List<Double> stm,List<Double> initialProbs,HmmPdfInterface [] obsModels) {

        this.numStates = numStates;

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


    double [][] mapB(final double [][] observations) {
        double [][] bmap = new double[this.numStates][];

        for (int iState = 0; iState < this.numStates; iState++) {
            bmap[iState] = this.obsModels[iState].getLikelihood(observations);
        }

        return bmap;
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


    public int [] getViterbiPath(final double [][] observations) {
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

        int numObs = observations[0].length;
        double [][] bmap = this.mapB(observations);

        double [][] phi = new double[this.numStates][numObs];
        int [][] viterbiIndices = new int[this.numStates][numObs];

        for (int i = 0; i < this.numStates; i++) {
            viterbiIndices[i][0] = 0;
        }

        // init
        for (int i = 0; i < this.numStates; i++) {
            phi[i][0] = -Math.log(this.initialState[i]+1e-15) - Math.log(bmap[i][0] + 1e-15);
        }


        //do viterbi
        double [] cost = new double[this.numStates];

        double obscost;
        for (int t = 1; t < numObs; t++) {
            for (int j = 0; j < this.numStates; j++) {
                //#"j" mean THIS (the jth) hidden state
                obscost = -Math.log(bmap[j][t] + 1e-15);
                for (int i = 0; i < this.numStates; i++) {
                   cost[i] = -Math.log(this.A[i][j] + 1e-15) + obscost;
                }


                for (int i = 0; i < this.numStates; i++) {
                    cost[i] += phi[i][t-1];
                }

                int minidx = findMinIndexOfDoubleArray(cost);
                double minval = cost[minidx];

                phi[j][t] = minval;

                viterbiIndices[j][t] = minidx;

            }
        }

        int [] path = new int[numObs];
        //#let's just say you wind up in state zero at the end?
        //otherwise, we have to test each possible ending, bleh.
        path[numObs-1] = 0;

        //#backtrack to get optimal path
        for (int t = numObs - 2; t >= 0; t--) {
            path[t] = viterbiIndices[path[t+1]][t];
        }


        return path;

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
