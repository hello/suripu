package com.hello.suripu.algorithm.hmm;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 *  DANGER this class is mutable!
 */


public class MultiObsSequenceAlphabetHiddenMarkovModel {
    Map<String,double [][]> logAlphabetNumerator;
    double [][] logANumerator;
    double [] logDenominator;
    final double [] pi;
    final int numStates;

    final static double MIN_A = 1e-6;
    final static double MIN_PROB = 1e-6;
    final static double FORBIDDEN_TRANSITION_PENALTY = -100;

    private static double [][] clone2d(final double [][] x) {
        double [][] A = new double[x.length][0];
        for (int j = 0; j < x.length; j++) {
            A[j] = x[j].clone();
        }

        return A;
    }

    private static double [][]  getZeroedMatrix(final int m, final int n) {
        final double [][] x = new double[m][n];

        for (int j = 0; j < m; j++) {
            Arrays.fill(x[j], 0.0);
        }

        return x;
    }

    private static double [][]  getLogZeroedMatrix(final int m, final int n) {
        final double [][] x = new double[m][n];

        for (int j = 0; j < m; j++) {
            Arrays.fill(x[j], LogMath.LOGZERO);
        }

        return x;
    }



    public MultiObsSequenceAlphabetHiddenMarkovModel(Map<String, double[][]> logAlphabetNumerator, double[][] logANumerator, double[] logDenominator, final double [] pi) {
        this.numStates = logANumerator.length;
        this.logAlphabetNumerator = logAlphabetNumerator;
        this.logANumerator = logANumerator;
        this.logDenominator = logDenominator;
        this.pi = pi;
    }

    double [][] getAMatrix()  {
        //finalruct transition matrix
        double [][] A = new double[logANumerator.length][0];
        for (int j = 0; j < logANumerator.length; j++) {
            A[j] = logANumerator[j].clone();
        }

        for (int iState = 0; iState < numStates; iState++) {
            for (int j = 0; j < numStates; j++) {
                A[iState][j] = LogMath.eexp(LogMath.elnproduct(logANumerator[iState][j], -logDenominator[iState]));

                if (A[iState][j] < MIN_A && A[iState][j] != 0.0) {
                    A[iState][j] = MIN_A;
                }
            }

            double thesum = 0.0;
            for (int j = 0; j < numStates; j++) {
                thesum += A[iState][j];
            }

            for (int j = 0; j < numStates; j++) {
                A[iState][j] /= thesum;
            }

        }

        return A;
    }

    Map<String,double [][]> getAlphabetMatrices() {

        final Map<String,double [][]> alphabetProbsMap = Maps.newHashMap();

        for (final String key : logAlphabetNumerator.keySet()) {

            final double [][] numerator = logAlphabetNumerator.get(key);

            if (numerator == null) {
                //yeah, fail silently
                continue;
            }

            final int alphabetSize = numerator[0].length;

            //finalruct alphabet probs
            double [][] alphabetProbs = clone2d(numerator);
            for (int iState = 0; iState < numStates; iState++) {
                for (int iAlphabet = 0; iAlphabet < alphabetSize; iAlphabet++) {
                    final double value = LogMath.eexp(LogMath.elnproduct(numerator[iState][iAlphabet], -logDenominator[iState]));

                    alphabetProbs[iState][iAlphabet] = value;

                    if (alphabetProbs[iState][iAlphabet] < MIN_PROB) {
                        alphabetProbs[iState][iAlphabet] = MIN_PROB;
                    }
                }

                double thesum = 0.0;
                for (int iAlphabet = 0; iAlphabet < alphabetSize; iAlphabet++) {
                    thesum += alphabetProbs[iState][iAlphabet];
                }

                for (int iAlphabet = 0; iAlphabet < alphabetSize; iAlphabet++) {
                    alphabetProbs[iState][iAlphabet] /= thesum;
                }

            }

            alphabetProbsMap.put(key, alphabetProbs);
        }

        return alphabetProbsMap;

    }

    private double [][] getLogBMap(final Map<String,double [][]>  rawdataMap, final Map<String,double [][]> alphabetProbsMap)  {

        if (rawdataMap.isEmpty()) {
            return new double[numStates][0];
        }



        final int numObs = rawdataMap.get(rawdataMap.keySet().iterator().next())[0].length;

        final double [][] logbmap = getZeroedMatrix(numStates, numObs);
        for (final String key : alphabetProbsMap.keySet()) {

            final double [][] alphabetProbs = alphabetProbsMap.get(key);

            final double [][] rawdata = rawdataMap.get(key);

            if (rawdata == null) {
                //TODO log this as error
                continue;
            }


            //get logbmap
            for (int iState = 0; iState < numStates; iState++) {
                for (int t = 0; t < numObs; t++) {
                    final int idx = (int)rawdata[0][t];

                    assert(idx >= 0 && idx < alphabetProbs[0].length);

                    logbmap[iState][t] = LogMath.elnproduct(logbmap[iState][t], LogMath.eln(alphabetProbs[iState][idx]));
                }
            }
        }

        return logbmap;
    }

    void reestimate(final MultiObsSequence meas, final int priorWeightAsNumberOfSamples) {
        int iterationNumber;
        int iSequence;

        final Map<String, double [][]>  rawdata = meas.rawmeasurements;
        final Map<Integer,Integer> labels = meas.labels;
        final Multimap<Integer,MultiObsSequence.Transition> forbiddenTransitions = meas.forbiddenTransitions;

        if (rawdata.isEmpty()) {
            return;
        }

        /*  MUST HAVE LABELS OF SOME KIND FOR ESTIMATION -- this is supposed to be for semi supervised, not unsupervised. */
        if (labels.isEmpty()) {
            return;
        }



        final int numObs = rawdata.get(rawdata.keySet().iterator().next())[0].length;

        final Map<String,double [][]> alphabetProbsMap = getAlphabetMatrices();

        final double [][] A = getAMatrix();

        final double [][] logbmap = getLogBMap(rawdata,alphabetProbsMap);

        final AlphaBetaResult alphaBeta = getAlphaAndBeta(numObs, pi, logbmap, A, numStates,labels,forbiddenTransitions);

        final double [][] logANumerator = getLogANumerator(A,alphaBeta, logbmap, forbiddenTransitions, numObs, numStates);

        final double [] logDenominator = getLogDenominator(alphaBeta, numStates, numObs);

        for (final String key : rawdata.keySet()) {

            if (this.logAlphabetNumerator.get(key) == null) {
                continue;
            }

            final double [] rawmeas = rawdata.get(key)[0];

            final int alphabetSize = logAlphabetNumerator.get(key)[0].length;
            final double [][] logAlphabetNumerator = getLogAlphabetNumerator(alphaBeta, rawmeas, numStates, numObs, alphabetSize);


            this.logAlphabetNumerator.put(key, LogMath.elnAddMatrix(this.logAlphabetNumerator.get(key), logAlphabetNumerator));
        }

        this.logANumerator = LogMath.elnAddMatrix(this.logANumerator, logANumerator);
        this.logDenominator = LogMath.elnAddVector(this.logDenominator, logDenominator);

        if (priorWeightAsNumberOfSamples > 0) {
            final double scaleFactor = (double)priorWeightAsNumberOfSamples / (priorWeightAsNumberOfSamples + 1);

            scalePriors(scaleFactor);
        }

    }

    
    private static class AlphaBetaResult {

        final double [][] logalpha;
        final double [][] logbeta;
        final double modelLikelihood;


        public AlphaBetaResult(double[][] logalpha, double[][] logbeta, double modelLikelihood) {
            this.logalpha = logalpha;
            this.logbeta = logbeta;
            this.modelLikelihood = modelLikelihood;
        }
    }


    private static double [][] getLogAWithForbiddenStates(final double [][] logA,final Multimap<Integer,MultiObsSequence.Transition>  forbiddenTransitions, final int t) {

        double [][] logA2 = clone2d(logA);


        Collection<MultiObsSequence.Transition> transitionsAtThisTime =  forbiddenTransitions.get(t);

        for (Iterator<MultiObsSequence.Transition> it = transitionsAtThisTime.iterator(); it.hasNext();) {
            final MultiObsSequence.Transition transition = it.next();

            logA2[transition.fromState][transition.toState] = FORBIDDEN_TRANSITION_PENALTY;
        }

        return logA2;

    }

    private  static int getLabel(final int t, final Map<Integer,Integer> labels) {
        final Integer label = labels.get(t);

        if (label == null) {
            return -1;
        }

        return (int)label;
    }

    static private AlphaBetaResult getAlphaAndBeta(final int numObs,final double []  pi, final double [][]  logbmap, final double [][]  A,final int numStates,final Map<Integer,Integer> labels, final Multimap<Integer,MultiObsSequence.Transition>  forbiddenTransitions) {

    /*
     Calculates 'alpha' the forward variable.
     
     The alpha variable is a numpy array indexed by time, then state (NxT).
     alpha[i][t] = the probability of being in state 'i' after observing the
     first t symbols.
     
     forbidden transitions means at that time index a transition is unavailable
     labels mean that you have to be in that time
     */
        int t,j,i;
        double [][] logalpha = getLogZeroedMatrix(numStates,numObs);
        double [][] logbeta = getLogZeroedMatrix(numStates,numObs);
        double temp;
        double [][] logA = clone2d(A); //copy

        //nominal A matrix
        for (j = 0; j < numStates; j++) {
            for (i = 0; i < numStates; i++) {
                logA[j][i] = LogMath.eln(logA[j][i]);
            }
        }


        //init stage - alpha_1(x) = pi(x)b_x(O1)

        for (j = 0; j < numStates; j++) {
            logalpha[j][0] = LogMath.elnproduct(LogMath.eln(pi[j]), logbmap[j][0]);
        }


        for (t = 1; t < numObs; t++) {

            double [][] logAThisTimeStep = getLogAWithForbiddenStates(logA,forbiddenTransitions,t);

            for (j = 0; j < numStates; j++) {
                temp = LogMath.LOGZERO;

                for (i = 0; i < numStates; i++) {
                    //alpha[j][t] += alpha[i][t-1]*A[i][j];
                    final double tempval = LogMath.elnproduct(logalpha[i][t - 1], logAThisTimeStep[i][j]);
                    temp = LogMath.elnsum(temp, tempval);
                }

                //alpha[j][t] *= bmap[j][t];
                logalpha[j][t] = LogMath.elnproduct(temp, logbmap[j][t]);
            }


            int label = getLabel(t,labels);
            if (label >= 0) {
                for (j = 0; j < numStates; j++) {
                    if (j != label) {
                        logalpha[j][t] = LogMath.LOGZERO;
                    }
                }
            }


        }
    
    /*
     Calculates 'beta' the backward variable.
     
     The beta variable is a numpy array indexed by time, then state (NxT).
     beta[i][t] = the probability of being in state 'i' and then observing the
     symbols from t+1 to the end (T).
     */


        // init stage
        for (int s = 0; s < numStates; s++) {
            logbeta[s][numObs - 1] = 0.0;
        }



        for (t = numObs - 2; t >= 0; t--) {

            double [][] logAThisTimeStep = getLogAWithForbiddenStates(logA,forbiddenTransitions,t);

            for (i = 0; i < numStates; i++) {
                temp = LogMath.LOGZERO;
                for (j = 0;  j < numStates; j++) {
                    final double tempval  = LogMath.elnproduct(logbmap[j][t + 1], logbeta[j][t + 1]);
                    final double tempval2 = LogMath.elnproduct(tempval, logAThisTimeStep[i][j]);
                    temp = LogMath.elnsum(temp, tempval2);
                    //beta[i][t] += A[i][j]*bmap[j][t+1] * beta[j][t+1];
                }

                logbeta[i][t] = temp;
            }


            final int label = getLabel(t,labels);
            if (label >= 0) {
                for (j = 0; j < numStates; j++) {
                    if (j != label) {
                        logbeta[j][t] = LogMath.LOGZERO;
                    }
                }
            }

        }

        temp = LogMath.LOGZERO;
        for (i = 0; i < numStates; i++) {
            temp = LogMath.elnsum(temp, logalpha[i][numObs - 1]);
        }


        return new AlphaBetaResult(logalpha,logbeta,temp);




    }

    static double [][] getLogANumerator(final double [][] originalA, final AlphaBetaResult alphabeta,final double [][]logbmap,final Multimap<Integer,MultiObsSequence.Transition> forbiddenTransitions,final int numObs, final int numStates) {

        int i,j,t;
        final double [][] logANumerator = getLogZeroedMatrix(numStates, numStates);

        final double [][]logalpha = alphabeta.logalpha;
        final double [][]logbeta = alphabeta.logbeta;

        double [][] logA = clone2d(originalA); //copy

        //nominal A matrix
        for (j = 0; j < numStates; j++) {
            for (i = 0; i < numStates; i++) {
                logA[j][i] = LogMath.eln(logA[j][i]);
            }
        }

        for (i = 0; i < numStates; i++) {
            for (j = 0; j < numStates; j++) {
                double numer = LogMath.LOGZERO;

                for (t = 0; t < numObs - 1; t++) {
                    double [][] logAThisTimeStep = getLogAWithForbiddenStates(logA,forbiddenTransitions,t);

                    final double tempval1 = LogMath.elnproduct(logalpha[i][t], logAThisTimeStep[i][j]);
                    final double tempval2 = LogMath.elnproduct(logbmap[j][t + 1], logbeta[j][t + 1]);
                    final double tempval3 = LogMath.elnproduct(tempval1, tempval2);

                    numer = LogMath.elnsum(numer, tempval3);
                }

                if (originalA[i][j] == 0.0) {
                    logANumerator[i][j] = LogMath.LOGZERO;
                }
                else {
                    logANumerator[i][j] = numer;
                }
            }
        }



        for (i = 0; i < numStates; i++) {
            for (j = 0; j < numStates; j++) {
                logANumerator[j][i] = LogMath.elnproduct(logANumerator[j][i], -alphabeta.modelLikelihood);
            }
        }


        return logANumerator;

    }

    static double [][] getLogAlphabetNumerator(final AlphaBetaResult alphabeta, final double [] rawdata, final int numStates, final int numObs, final int alphabetSize ) {

        int iState,iAlphabet,t;

        double [][] logAlphabetNumerator = getLogZeroedMatrix(numStates, alphabetSize);

        final double [][] logalpha = alphabeta.logalpha;
        final double [][] logbeta = alphabeta.logbeta;

        for (iState = 0; iState < numStates; iState++) {

            for (t = 0; t < numObs; t++) {
                final int idx = (int)rawdata[t];

                assert(idx >= 0 && idx < alphabetSize);

                logAlphabetNumerator[iState][idx] = LogMath.elnsum(logAlphabetNumerator[iState][idx], LogMath.elnproduct(logalpha[iState][t], logbeta[iState][t]));
            }

        }


        for (iState = 0; iState < numStates; iState++) {
            for (iAlphabet = 0; iAlphabet < alphabetSize; iAlphabet++) {
                logAlphabetNumerator[iState][iAlphabet] = LogMath.elnproduct(logAlphabetNumerator[iState][iAlphabet], -alphabeta.modelLikelihood);
            }
        }

        return logAlphabetNumerator;


    }

    static double [] getLogDenominator(final AlphaBetaResult alphabeta, final int numStates, final int numObs) {

        int iState,t;

        final double [] logDenominator = new double[numStates];
        Arrays.fill(logDenominator,LogMath.LOGZERO);

        final double [][] logalpha = alphabeta.logalpha;
        final double [][] logbeta = alphabeta.logbeta;

        for (iState = 0; iState < numStates; iState++) {
            for (t = 0; t < numObs; t++) {
                final double tempval = LogMath.elnproduct(logalpha[iState][t], logbeta[iState][t]);
                logDenominator[iState] = LogMath.elnsum(logDenominator[iState], tempval); //sum
            }

            logDenominator[iState] = LogMath.elnproduct(logDenominator[iState], -alphabeta.modelLikelihood);
        }

        return logDenominator;
    }


    public void scalePriors(final double scaleFactor) {

        final double logScaleFactor = LogMath.eln(scaleFactor);

        for (final String key : logAlphabetNumerator.keySet()) {
            logAlphabetNumerator.put(key, LogMath.elnMatrixScalarProduct(logAlphabetNumerator.get(key),logScaleFactor));
        }

        logANumerator = LogMath.elnMatrixScalarProduct(logANumerator,logScaleFactor);
        logDenominator = LogMath.elnVectorScalarProduct(logDenominator,logScaleFactor);
    }

}
