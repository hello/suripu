package com.hello.suripu.algorithm.sleep;

import com.hello.suripu.algorithm.core.AmplitudeData;

/**
 * Created by pangwu on 12/14/14.
 */
public class InternalScore implements Comparable<InternalScore> {
    public final AmplitudeData data;
    public final double score;

    public InternalScore(final AmplitudeData data, final double score){
        this.data = data;
        this.score = score;
    }

    @Override
    public int compareTo(final InternalScore o) {
        return Double.compare(this.score, o.score);
    }
}
