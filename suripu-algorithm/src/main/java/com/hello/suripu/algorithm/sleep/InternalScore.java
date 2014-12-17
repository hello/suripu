package com.hello.suripu.algorithm.sleep;

/**
 * Created by pangwu on 12/14/14.
 */
public class InternalScore implements Comparable<InternalScore> {
    public final long timestamp;
    public final double score;

    public InternalScore(final long timestamp, final double score){
        this.timestamp = timestamp;
        this.score = score;
    }

    @Override
    public int compareTo(final InternalScore o) {
        return Double.compare(this.score, o.score);
    }
}
