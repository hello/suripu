package com.hello.suripu.algorithm.hmm;

import com.google.common.base.Objects;

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

    @Override
    public Transition clone()  {
        return new Transition(fromState,toState,idx);
    }

    @Override
    public String toString() {

        if (idx >= 0) {
            return Objects.toStringHelper(this)
                    .add("from",fromState)
                    .add("to",toState)
                    .add("idx",idx).toString();
        }
        else {
            return Objects.toStringHelper(this)
                    .add("from",fromState)
                    .add("to",toState).toString();
        }
    }
}