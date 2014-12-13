package com.hello.suripu.algorithm.core;

/**
 * Created by pangwu on 6/10/14.
 */
public class Segment {
    private long startTimestamp = 0;
    private long endTimestamp = 0;
    private int offsetMillis = 0;

    public Segment() {}

    public Segment(final long startTimestamp, final long endTimestamp, final int offsetMillis) {
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.offsetMillis = offsetMillis;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public int getOffsetMillis() {
        return offsetMillis;
    }

    public void setOffsetMillis(int offsetMillis) {
        this.offsetMillis = offsetMillis;
    }

    public long getDuration(){
        return getEndTimestamp() - getStartTimestamp();
    }

}
