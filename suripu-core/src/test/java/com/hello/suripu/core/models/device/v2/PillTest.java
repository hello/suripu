package com.hello.suripu.core.models.device.v2;

import com.hello.suripu.core.models.Device;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by jakepiccolo on 1/12/16.
 */
public class PillTest {

    @Test
    public void testFromDeviceColor() throws Exception {
        final Pill.Color redColor = Pill.Color.fromDeviceColor(Device.Color.RED);
        assertThat(redColor, is(Pill.Color.RED));

        final Pill.Color aquaColor = Pill.Color.fromDeviceColor(Device.Color.AQUA);
        assertThat(aquaColor, is(Pill.Color.AQUA));
    }
}