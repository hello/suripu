package com.hello.suripu.core.util;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 11/20/14.
 */
public class UInt32Test {
    @Test
    public void testSignedToUnsigned(){
        int signedInt = -2;
        long expected = 0xFFFFFFFEl;
        long actual = UInt32.getValue(signedInt);
        assertThat(actual, is(expected));
        assertThat(actual > 0, is(true));

        signedInt = 2;
        expected = 2;
        actual = UInt32.getValue(signedInt);
        assertThat(actual, is(expected));
    }
}
