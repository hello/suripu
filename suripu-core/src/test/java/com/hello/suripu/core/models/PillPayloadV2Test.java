package com.hello.suripu.core.models;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by jakepiccolo on 11/16/15.
 */
public class PillPayloadV2Test {

    @Test
    public void testCreateWithMotionMask() throws Exception {
        final Long cosTheta = 1L;
        final Long motionMask = 0xFFL;
        final Long maxAmp = 1L;
        final TrackerMotion.PillPayloadV2 pillPayloadV2 = TrackerMotion.PillPayloadV2.createWithMotionMask(maxAmp, motionMask, cosTheta);
        assertThat(pillPayloadV2.cosTheta.get(), is(cosTheta));
        assertThat(pillPayloadV2.maxAmplitude, is(maxAmp));
        assertThat(pillPayloadV2.motionMask.get(), is(motionMask));
        assertThat(pillPayloadV2.onDurationInSeconds, is(8L));

        assertThat(TrackerMotion.PillPayloadV2.createWithMotionMask(maxAmp, 0xD8L, cosTheta).onDurationInSeconds,
                is(4L));
    }
}