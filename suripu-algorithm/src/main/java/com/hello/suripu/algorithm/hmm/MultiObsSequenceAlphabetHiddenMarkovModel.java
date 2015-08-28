package com.hello.suripu.algorithm.hmm;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;

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
    final static double FORBIDDEN_TRANSITION_PENALTY = Double.NEGATIVE_INFINITY;

    public Map<String, double[][]> getLogAlphabetNumerator() {
        return logAlphabetNumerator;
    }

    public double[][] getLogANumerator() {
        return logANumerator;
    }

    public double[] getLogDenominator() {
        return logDenominator;
    }

    public double[] getPi() {
        return pi;
    }

    static public class Result {
        public final int [] path;
        public final double pathScore;

        public Result(int[] path, double pathScore) {
            this.path = path;
            this.pathScore = pathScore;
        }
    }

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



    public Result decodeWithConstraints(final MultiObsSequence meas, final int [] possibleEndStates, final int [] minStateDurations) {
        final Map<String, double [][]>  rawdata = meas.rawmeasurements;
        final Map<Integer,Integer> labels = meas.labels;
        final Multimap<Integer,Transition> forbiddenTransitions = meas.forbiddenTransitions;

        final int numObs = rawdata.get(rawdata.keySet().iterator().next())[0].length;

        final Map<String,double [][]> alphabetProbsMap = getAlphabetMatrices();
        int j,i,t;

        final double [] scores = new double[numStates];

        final double [][] phi = getLogZeroedMatrix(numStates, numObs);
        final int [][] vindices = new int[numStates][numObs];

        double [][] logA = clone2d(getAMatrix()); //copy

        //nominal A matrix
        for (j = 0; j < numStates; j++) {
            for (i = 0; i < numStates; i++) {
                logA[j][i] = LogMath.eln(logA[j][i]);
            }
        }


        final double [][] logbmap = getLogBMap(rawdata,alphabetProbsMap);

        final int [] zeta = new int[numStates]; //this is the count for how long you've been in the same state
        //see the paper "Long-term Activities Segmentation using Viterbi Algorithm with a k-minimum-consecutive-states Constraint"
        //by Enrique Garcia-Ceja, Ramon Brena, 2014

        for (i = 0; i < numStates; i++) {
            zeta[i] = 1;
        }

        //init
        for (i = 0; i < numStates; i++) {
            phi[i][0] = LogMath.elnproduct(logbmap[i][0], LogMath.eln(pi[i]));
        }

        for (t = 1; t < numObs; t++) {

            final double [][] logAThisIndex = getLogAWithForbiddenStates(logA, forbiddenTransitions, t);

            int foo = 3;

            foo++;

            for (j = 0; j < numStates; j++) {

                final double obscost = logbmap[j][t];

                for (i = 0; i < numStates; i++) {
                    scores[i] = LogMath.elnproduct(logAThisIndex[i][j], obscost);
                }

                for (i = 0; i < numStates; i++) {
                    scores[i] = LogMath.elnproduct(scores[i], phi[i][t - 1]);
                }

                final SortedSet<CostWithIndex> costsWithIndex = Sets.newTreeSet();

                for (i = 0; i < numStates; i++) {
                    costsWithIndex.add(new CostWithIndex(i, scores[i]));
                }

                Iterator<CostWithIndex> costIterator = costsWithIndex.iterator();
                int maxidx = costIterator.next().idx;
                double maxval = scores[maxidx];

                //best path is to stay?  increment zeta.
                if (maxidx == j) {
                    zeta[j] += 1;
                }
                else {
                    zeta[j] = 1;
                }

                //if zeta of the state I'm coming FROM is above min durations,
                //I'll let the transition happen.  Otherwise, pick the next best state.

                if (zeta[maxidx] >= minStateDurations[maxidx]) {
                    phi[j][t] = maxval;
                    vindices[j][t] = maxidx;
                }
                else {
                    //next best.... so in theory we should check if this violates the second best state's constraints
                    //TODO check everything
                    maxidx = costIterator.next().idx;
                    maxval = scores[maxidx];

                    phi[j][t] = maxval;
                    vindices[j][t] = maxidx;
                }
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

        return new Result(path,maxScore);
    }

    static private void printMat(double [][] mat) {
        for (int j = 0; j < mat.length; j++) {

            if (j != 0) {
                System.out.print("\n");
            }

            for (int i = 0; i < mat[j].length; i++) {
                if (i != 0) {
                    System.out.print(",");
                }

                System.out.print(String.format("%.2f",mat[j][i]));
            }

        }
    }

    public boolean reestimate(final MultiObsSequence meas, final double priorWeightAsNumberOfSamples) {
        int iterationNumber;
        int iSequence;

        final Map<String, double [][]>  rawdata = meas.rawmeasurements;
        final Map<Integer,Integer> labels = meas.labels;
        final Multimap<Integer,Transition> forbiddenTransitions = meas.forbiddenTransitions;

        if (rawdata.isEmpty()) {
            return false;
        }

        /*  MUST HAVE LABELS OF SOME KIND FOR ESTIMATION -- this is supposed to be for semi supervised, not unsupervised. */
        if (labels.isEmpty()) {
            return false;
        }

        final int numObs = rawdata.get(rawdata.keySet().iterator().next())[0].length;

        //get observation models
        final Map<String,double [][]> alphabetProbsMap = getAlphabetMatrices();

        //get state transition matrix
        final double [][] A = getAMatrix();

        //get log of evaluated observations
        final double [][] logbmap = getLogBMap(rawdata,alphabetProbsMap);

        //compute log of forwards and backwards probs
        final AlphaBetaResult alphaBeta = getAlphaAndBeta(numObs, pi, logbmap, A, numStates,labels);

        //recompute numerator and denominators
        final double [][] logANumerator = getLogANumerator(A,alphaBeta, logbmap, forbiddenTransitions, numObs, numStates);

        final double [] logDenominator = getLogDenominator(alphaBeta, numStates, numObs);

        //go through each measurement available in the raw data
        //find the matching model, and evaluate it
        for (final String key : rawdata.keySet()) {

            if (this.logAlphabetNumerator.get(key) == null) {
                //don't log this -- we might have a model that ignores a particular measurement
                continue;
            }

            //measurements are only 1d for now
            final double [] rawmeas = rawdata.get(key)[0];

            final int alphabetSize = logAlphabetNumerator.get(key)[0].length;
            final double [][] logAlphabetNumerator = getLogAlphabetNumerator(alphaBeta, rawmeas, numStates, numObs, alphabetSize);

            this.logAlphabetNumerator.put(key, LogMath.elnAddMatrix(this.logAlphabetNumerator.get(key), logAlphabetNumerator));
        }

        this.logANumerator = LogMath.elnAddMatrix(this.logANumerator, logANumerator);
        this.logDenominator = LogMath.elnAddVector(this.logDenominator, logDenominator);


        /*
        if (priorWeightAsNumberOfSamples > 0) {
            final double scaleFactor = priorWeightAsNumberOfSamples / (priorWeightAsNumberOfSamples + 1);

            scalePriors(scaleFactor);
        }
        */

        return true;

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


    private static double [][] getLogAWithForbiddenStates(final double [][] logA,final Multimap<Integer,Transition>  forbiddenTransitions, final int t) {

        final double [][] logA2 = clone2d(logA);

        final Collection<Transition> transitionsAtThisTime =  forbiddenTransitions.get(t);

        if (transitionsAtThisTime == null || transitionsAtThisTime.isEmpty()) {
            return logA2;
        }

        for (Iterator<Transition> it = transitionsAtThisTime.iterator(); it.hasNext();) {
            final Transition transition = it.next();

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

    static private AlphaBetaResult getAlphaAndBeta(final int numObs,final double []  pi, final double [][]  logbmap, final double [][]  A,final int numStates,final Map<Integer,Integer> labels) {

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

            for (j = 0; j < numStates; j++) {
                temp = LogMath.LOGZERO;

                for (i = 0; i < numStates; i++) {
                    //alpha[j][t] += alpha[i][t-1]*A[i][j];
                    final double tempval = LogMath.elnproduct(logalpha[i][t - 1], logA[i][j]);
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

            for (i = 0; i < numStates; i++) {
                temp = LogMath.LOGZERO;
                for (j = 0;  j < numStates; j++) {
                    final double tempval  = LogMath.elnproduct(logbmap[j][t + 1], logbeta[j][t + 1]);
                    final double tempval2 = LogMath.elnproduct(tempval, logA[i][j]);
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

    static double [][] getLogANumerator(final double [][] originalA, final AlphaBetaResult alphabeta,final double [][]logbmap,final Multimap<Integer,Transition> forbiddenTransitions,final int numObs, final int numStates) {

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
