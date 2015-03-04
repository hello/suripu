package com.hello.suripu.core.models;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 3/4/15.
 */
public class CachedTimelinesTest {
    @Test
    public void testShouldInvalidateDifferentVersion(){
        final long expireMillis = DateTime.now().getMillis();
        final CachedTimelines cachedTimelines = CachedTimelines.create(new ArrayList<Timeline>(), new Integer(1).toString(), expireMillis);
        assertThat(cachedTimelines.shouldInvalidate(new Integer(2).toString(),
                DateTime.now().withTimeAtStartOfDay(),
                DateTime.now().withTimeAtStartOfDay(),
                5),
                is(true));
    }

    @Test
    public void shouldNotInvalidateTargetDateTooOld(){
        final long expireMillis = DateTime.now().getMillis();
        final CachedTimelines cachedTimelines = CachedTimelines.create(new ArrayList<Timeline>(), new Integer(1).toString(), expireMillis);
        assertThat(cachedTimelines.shouldInvalidate(new Integer(2).toString(),
                DateTime.now().withTimeAtStartOfDay().minusDays(20),
                DateTime.now(), 5),
                is(false));
    }

    @Test
    public void shouldNotInvalidateExpiredButTargetDateTooOld(){
        final long expireMillis = DateTime.now().plusMinutes(1).getMillis();
        final CachedTimelines cachedTimelines = CachedTimelines.create(new ArrayList<Timeline>(), new Integer(1).toString(), expireMillis);
        assertThat(cachedTimelines.shouldInvalidate(new Integer(2).toString(),
                DateTime.now().withTimeAtStartOfDay().minusDays(20),
                DateTime.now().plusMinutes(6),
                5),
                is(false));
    }

    @Test
    public void shouldInvalidateExpired(){
        final long expireMillis = DateTime.now().getMillis();
        final CachedTimelines cachedTimelines = CachedTimelines.create(new ArrayList<Timeline>(), new Integer(1).toString(), expireMillis);
        assertThat(cachedTimelines.shouldInvalidate(new Integer(1).toString(),
                DateTime.now().withTimeAtStartOfDay(),
                DateTime.now().plusMinutes(1),
                5),
                is(true));
    }

    @Test
    public void shouldNotInvalidateNotExpired(){
        final long expireMillis = DateTime.now().plusMinutes(1).getMillis();
        final CachedTimelines cachedTimelines = CachedTimelines.create(new ArrayList<Timeline>(), new Integer(1).toString(), expireMillis);
        assertThat(cachedTimelines.shouldInvalidate(new Integer(1).toString(),
                        DateTime.now().withTimeAtStartOfDay(),
                        DateTime.now(),
                        5),
                is(false));
    }
}
