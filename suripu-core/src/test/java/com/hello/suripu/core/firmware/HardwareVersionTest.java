package com.hello.suripu.core.firmware;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class HardwareVersionTest {

    @Test
    public void testEqual(){
        assertThat(HardwareVersion.SENSE_ONE, equalTo(HardwareVersion.SENSE_ONE));
        assertThat(HardwareVersion.fromInt(1), equalTo(HardwareVersion.SENSE_ONE));
        assertThat(HardwareVersion.fromInt(4), equalTo(HardwareVersion.SENSE_ONE_FIVE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongVersion(){
        HardwareVersion.fromInt(99);
    }
}
