package com.hello.suripu.algorithm.hmm;

import java.util.List;

/**
 * Created by benjo on 10/28/15.
 */
public class HiddenMarkovModelFactory {
    public static enum HmmType {
        ORIGINAL,
        LOGMATH
    }


    public static HiddenMarkovModelInterface create (final HmmType type,final int numStates,final List<Double> stm,final List<Double> initialProbs,final HmmPdfInterface [] obsModels,final int numFreeParams) {

        //translate objects into raw types
        final double [][] A = new double[numStates][numStates];

        int k = 0;
        for (int j = 0; j < numStates; j++) {
            for (int i = 0; i < numStates; i++) {
                A[j][i] = stm.get(k);
                k++;
            }
        }

        final double [] initialState = new double[numStates];

        for (int j = 0; j < numStates; j++) {
            initialState[j] = initialProbs.get(j);
        }

        return create(type,numStates,A,initialState,obsModels,numFreeParams);
    }

    public static HiddenMarkovModelInterface create(final HmmType type, final int numStates, final double [][] A,final double [] initialStateProbs, final HmmPdfInterface [] obsModels, final int numFreeParams) {


        switch (type) {

            case LOGMATH:
            {
                return new HiddenMarkovModelLogMath(numStates,A,initialStateProbs,obsModels,numFreeParams);
            }

            case ORIGINAL:
            default:
            {
                return new HiddenMarkovModel(numStates,A,initialStateProbs,obsModels,numFreeParams);

            }


        }

    }

}
