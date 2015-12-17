package com.hello.suripu.core.models.motion;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by jakepiccolo on 12/16/15.
 */
public class MaskUtilsTest {

    @Test
    public void testMaskForPreviousMinuteWithOffset() {
        assertThat(MaskUtils.maskForPreviousMinuteWithOffset(0L, 0), is(0L));
        assertThat(MaskUtils.maskForPreviousMinuteWithOffset(15L, 0), is(15L));
        assertThat(MaskUtils.maskForPreviousMinuteWithOffset(8L, 1), is(16L));
        assertThat(MaskUtils.maskForPreviousMinuteWithOffset(15L << 59, 0), is(1L << 59));
        assertThat(MaskUtils.maskForPreviousMinuteWithOffset(15L << 57, 3), is(0L));
    }

    @Test
    public void testMaskForCurrentMinuteWithOffset() {
        assertThat(MaskUtils.maskForCurrentMinuteWithOffset(1L, 0), is(0L));
        assertThat(MaskUtils.maskForCurrentMinuteWithOffset(15L << 57, 3), is(15L));
        assertThat(MaskUtils.maskForCurrentMinuteWithOffset(15L, 59), is(7L));
    }

    @Test
    public void testGetBit() {
        assertThat(MaskUtils.getBit(1L, 0), is(true));
        assertThat(MaskUtils.getBit(1L, 1), is(false));
        assertThat(MaskUtils.getBit(2L, 1), is(true));
        assertThat(MaskUtils.getBit(2L, 0), is(false));
        assertThat(MaskUtils.getBit(0x0FL, 0), is(true));
        assertThat(MaskUtils.getBit(0x0FL, 1), is(true));
        assertThat(MaskUtils.getBit(0x0FL, 2), is(true));
        assertThat(MaskUtils.getBit(0x0FL, 3), is(true));
        assertThat(MaskUtils.getBit(0x0FL, 4), is(false));
    }

    @Test
    public void testToBooleans() {
        for (int i = 0; i < 8; i++) {
            assertThat(MaskUtils.toBooleans(0xFFL).get(i), is(true));
        }
        for (int i = 8; i < 60; i++) {
            assertThat(MaskUtils.toBooleans(0xFFL).get(i), is(false));
        }
    }
}