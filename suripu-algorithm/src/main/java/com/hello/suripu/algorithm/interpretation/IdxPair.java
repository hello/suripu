package com.hello.suripu.algorithm.interpretation;

/**
 * Created by benjo on 4/12/16.
 */
public class IdxPair {
    final public int i1;
    final public int i2;

    public IdxPair(final int i1,final int i2) {
        this.i1 = i1;
        this.i2 = i2;
    }

    public int getDistance(final IdxPair pair) {
        int distance = 0;
        if (pair.i1 > i2) {
            distance = pair.i1 - i2;
        }
        else if (pair.i2 < i1) {
            distance = i1 - pair.i2;
        }
        return distance;
    }

    public int duration() {
        return i2 - i1;
    }

    public IdxPair merge(final IdxPair pair) {
        return new IdxPair(i1,pair.i2);
    }
}
