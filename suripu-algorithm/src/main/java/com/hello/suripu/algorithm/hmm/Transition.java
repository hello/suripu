package com.hello.suripu.algorithm.hmm;

/**
 * Created by benjo on 8/20/15.
 */
public class Transition {
    public final int fromState;
    public final int toState;
    public final int idx;

    public Transition(int fromState, int toState) {
        this.fromState = fromState;
        this.toState = toState;
        this.idx = -1;
    }

    public Transition(int fromState, int toState, int idx) {
        this.fromState = fromState;
        this.toState = toState;
        this.idx = idx;
    }
}