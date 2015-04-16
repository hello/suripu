package com.hello.suripu.algorithm.sleep;

import com.hello.suripu.algorithm.core.Segment;

/**
 * Created by pangwu on 4/14/15.
 */
public class VotingSegment extends Segment {
    public final double vote;
    public VotingSegment(final long startTimestampMillis, final long endTimestampMillis, final int offsetMillis, final double vote){
        super(startTimestampMillis, endTimestampMillis, offsetMillis);
        this.vote = vote;
    }
}
