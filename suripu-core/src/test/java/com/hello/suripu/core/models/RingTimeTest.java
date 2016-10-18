package com.hello.suripu.core.models;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 9/23/14.
 */
public class RingTimeTest {

    @Test
    public void testEmpty(){
        final RingTime empty = RingTime.createEmpty();
        assertThat(empty.isEmpty(), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid(){
        final RingTime invalidRingTime = new RingTime(DateTime.now().plusMinutes(1).getMillis(),
                DateTime.now().getMillis(),
                0, true);
    }
}
