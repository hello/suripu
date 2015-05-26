package com.hello.suripu.core.util;

import java.util.List;

/**
 * Created by benjo on 5/25/15.
 */
public class SegmentPairWithGaps {
    public SegmentPairWithGaps(SegmentPair bounds, List<SegmentPair> gaps) {
        this.bounds = bounds;
        this.gaps = gaps;
    }

    public boolean isInsideOf(final SegmentPairWithGaps p) {
        if (bounds.i1 >= p.bounds.i1 && bounds.i2 <= p.bounds.i2) {
            return true;
        }
        else {
            return false;
        }
    }
    public final SegmentPair bounds;
    public final List<SegmentPair> gaps;
}