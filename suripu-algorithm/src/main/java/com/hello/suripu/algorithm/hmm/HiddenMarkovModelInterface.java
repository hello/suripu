package com.hello.suripu.algorithm.hmm;

/**
 * Created by benjo on 10/28/15.
 */
public interface HiddenMarkovModelInterface {
    HmmDecodedResult decode(double[][] observations, Integer[] possibleEndStates, double minTransitionLikelihood);
    int getNumberOfStates();
}
