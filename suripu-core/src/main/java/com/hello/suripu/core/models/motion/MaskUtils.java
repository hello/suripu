package com.hello.suripu.core.models.motion;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by jakepiccolo on 12/16/15.
 */
public class MaskUtils {

    private static final int SECONDS_IN_MINUTE = 60;
    private static final Long USABLE_BITS = 0x0FFFFFFFFFFFFFFFL; // Don't use the high 4 bits, only lower 60

    static Long maskForPreviousMinuteWithOffset(final Long motionMask, final int offsetSeconds) {
        return (motionMask << offsetSeconds) & USABLE_BITS;
    }

    static Long maskForCurrentMinuteWithOffset(final Long motionMask, final int offsetSeconds) {
        return motionMask >> (60 - offsetSeconds);
    }

    static Boolean getBit(final Long mask, final int offsetSeconds) {
        return ((mask >> offsetSeconds) & 1) == 1;
    }

    public static List<Boolean> toBooleans(final Long mask) {
        final List<Boolean> result = Lists.newArrayListWithExpectedSize(SECONDS_IN_MINUTE);
        for (int i = 0; i < SECONDS_IN_MINUTE; i++) {
            final Boolean didMove = getBit(mask, i);
            result.add(didMove);
        }
        return result;
    }

    // For debugging
    static void printBits(final Long x) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 63; i >= 0; i--) {
            stringBuilder.append((x >> i) & 1);
        }
        System.out.println(stringBuilder.toString());
    }
}
