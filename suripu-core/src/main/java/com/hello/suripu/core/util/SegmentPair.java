package com.hello.suripu.core.util;

/**
 * Created by benjo on 5/25/15.
 */
public class SegmentPair {
    public SegmentPair(final int i1, final int i2) {
        this.i1 = i1;
        this.i2 = i2;
    }

    public int compare(final Integer idx) {
        if (idx < i1) {
            return -1;
        }

        if (idx > i2) {
            return 1;
        }

        return 0;
    }

    public final int i1;
    public final int i2;
}
