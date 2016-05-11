package com.hello.suripu.core.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.isomorphism.util.TokenBucket;
import org.isomorphism.util.TokenBuckets;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by jakepiccolo on 5/10/16.
 */
public class RequestRateLimiter<K> {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RequestRateLimiter.class);

    private final LoadingCache<K, TokenBucket> tokenBucketCache;

    protected RequestRateLimiter(final LoadingCache<K, TokenBucket> tokenBucketCache) {
        this.tokenBucketCache = tokenBucketCache;
    }

    public static <K> RequestRateLimiter<K> create(final int maxSize, final long tokensPerSecond) {
        final LoadingCache<K, TokenBucket> cache = CacheBuilder.newBuilder()
                .maximumSize(maxSize)
                .expireAfterAccess(5L, TimeUnit.MINUTES)
                .build(new CacheLoader<K, TokenBucket>() {
                    @Override
                    public TokenBucket load(K key) throws Exception {
                        return TokenBuckets.builder()
                                .withCapacity(tokensPerSecond)
                                .withFixedIntervalRefillStrategy(tokensPerSecond, 1L, TimeUnit.SECONDS)
                                .build();
                    }
                });
        return new RequestRateLimiter<>(cache);
    }

    public final boolean canProceed(final K key, final int tokens) {
        TokenBucket tokenBucket = null;
        try {
            tokenBucket = tokenBucketCache.get(key);
        } catch (ExecutionException e) {
            LOGGER.error("error=ExecutionException method=canProceed key={} tokens={} exception={}", key, tokens, e);
        }

        if (tokenBucket == null) {
            return true;
        }

        return tokenBucket.tryConsume(tokens);
    }

}
