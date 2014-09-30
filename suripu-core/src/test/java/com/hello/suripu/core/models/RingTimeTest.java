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
                0);
    }

    @Test
    public void testRegularRingTime(){
        final DateTime ring = DateTime.now();
        final RingTime regularRingTime = new RingTime(ring.getMillis(), ring.getMillis(), 0);
        assertThat(regularRingTime.isRegular(), is(true));
        assertThat(regularRingTime.isSmart(), is(false));
        assertThat(regularRingTime.isEmpty(), is(false));
    }

    @Test
    public void testSmartRingTime(){
        final DateTime ring = DateTime.now();
        final RingTime smartRingTime = new RingTime(ring.minusMinutes(1).getMillis(), ring.getMillis(), 0);
        assertThat(smartRingTime.isRegular(), is(false));
        assertThat(smartRingTime.isSmart(), is(true));
        assertThat(smartRingTime.isEmpty(), is(false));
    }
}
