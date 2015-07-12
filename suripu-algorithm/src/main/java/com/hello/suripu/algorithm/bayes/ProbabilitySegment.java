package com.hello.suripu.algorithm.bayes;

/**
 * Created by benjo on 6/24/15.
 */
public class ProbabilitySegment implements Comparable<ProbabilitySegment> {
    public final int i1;
    public final int i2;
    public final String tag;

    public ProbabilitySegment(int i1, int i2, String tag) {
        this.i1 = i1;
        this.i2 = i2;
        this.tag = tag;
    }

    public int getDuration() {
        return i2 - i1 + 1;
    }

    @Override
    public int compareTo(ProbabilitySegment o) {
        if (o.i1 > i2) {
            return -1;
        }

        if (o.i2 < i1) {
            return 1;
        }

        return 0;
    }
}
