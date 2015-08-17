package com.hello.suripu.algorithm.hmm;

import com.google.common.collect.Multimap;

import java.util.Map;

/**
 * Created by benjo on 8/17/15.
 */
public class MultiObsSequence {

    public static class Transition {
        public final int fromState;
        public final int toState;

        public Transition(int fromState, int toState) {
            this.fromState = fromState;
            this.toState = toState;
        }
    }

    public MultiObsSequence(Map<String, double[][]> rawmeasurements, Map<Integer, Integer> labels, Multimap<Integer, Transition> forbiddenTransitions) {
        this.rawmeasurements = rawmeasurements;
        this.labels = labels;
        this.forbiddenTransitions = forbiddenTransitions;
    }

    public final Map<String,double [][]> rawmeasurements;
    public final Map<Integer,Integer> labels;
    public final Multimap<Integer,Transition> forbiddenTransitions;

}
