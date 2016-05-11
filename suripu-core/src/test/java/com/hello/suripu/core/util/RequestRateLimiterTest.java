package com.hello.suripu.core.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jakepiccolo on 5/10/16.
 */
public class RequestRateLimiterTest {

    @Test
    public void testCanProceed() throws Exception {
        final RequestRateLimiter<String> requestRateLimiter = RequestRateLimiter.create(10, 2);
        final String key = "theKey";
        assertThat(requestRateLimiter.canProceed(key, 1), is(true));
        assertThat(requestRateLimiter.canProceed(key, 1), is(true));
        assertThat(requestRateLimiter.canProceed(key, 1), is(false));

        Thread.sleep(1001);
        assertThat(requestRateLimiter.canProceed(key, 1), is(true));
        assertThat(requestRateLimiter.canProceed(key, 1), is(true));
        assertThat(requestRateLimiter.canProceed(key, 1), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCanProceedTooManyTokens() throws Exception {
        final RequestRateLimiter<String> requestRateLimiter = RequestRateLimiter.create(10, 2);
        final String key = "theKey";
        requestRateLimiter.canProceed(key, 3);
    }

}