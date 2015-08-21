package com.hello.suripu.algorithm.hmm;

/**
 * Created by benjo on 8/20/15.
 */
public class Transition {
    public final int fromState;
    public final int toState;

    public Transition(int fromState, int toState) {
        this.fromState = fromState;
        this.toState = toState;
    }
}