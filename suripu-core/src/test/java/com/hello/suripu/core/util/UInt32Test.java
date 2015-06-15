package com.hello.suripu.core.util;

import org.junit.Test;

import java.nio.ByteOrder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 11/20/14.
 */
public class UInt32Test {
    @Test
    public void testSignedToUnsigned(){
        int signedInt = -2;
        long expected = 0xFFFFFFFEL;
        long actual = UInt32.getValue(signedInt);
        assertThat(actual, is(expected));
        assertThat(actual > 0, is(true));

        signedInt = 2;
        expected = 2;
        actual = UInt32.getValue(signedInt);
        assertThat(actual, is(expected));
    }

    @Test
    public void testARGBToInt(){
        final byte[] red = new byte[]{(byte)0xFF, (byte)0xFE, 0x00, 0x00};
        if(ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)){
            assertThat(PillColorUtil.argbToIntBasedOnSystemEndianess(red), is(0x0000FEFF));
        }else{
            assertThat(PillColorUtil.argbToIntBasedOnSystemEndianess(red), is(0xFFFE0000));
        }
    }
}
