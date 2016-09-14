package com.hello.suripu.coredropwizard.oauth.stores;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.hello.suripu.coredropwizard.oauth.ExternalApplication;
import com.hello.suripu.coredropwizard.oauth.InvalidExternalTokenException;
import com.hello.suripu.coredropwizard.db.ExternalTokenDAO;
import com.hello.suripu.coredropwizard.oauth.ExternalToken;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class PersistentExternalTokenStore implements ExternalOAuthTokenStore<ExternalToken> {

    private final ExternalTokenDAO externalTokenDAO;
    private final ExternalApplicationStore<ExternalApplication> externalApplicationStore;

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentExternalTokenStore.class);

    final LoadingCache<String, Optional<ExternalToken>> cache;

    // This is called by the cache when it doesn't contain the key
    final CacheLoader loader = new CacheLoader<String, Optional<ExternalToken>>() {
        public Optional<ExternalToken> load(final String dirtyToken) {
            LOGGER.debug("{} not in cache, fetching from DB", dirtyToken);
            return fromDB(dirtyToken, false);
        }
    };


    public PersistentExternalTokenStore(
            final ExternalTokenDAO externalTokenDAO,
            ExternalApplicationStore<ExternalApplication> externalApplicationStore) {
        this.externalTokenDAO = externalTokenDAO;
        this.externalApplicationStore = externalApplicationStore;

        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build(loader);
    }

    @Override
    public void storeToken(ExternalToken externalToken) throws InvalidExternalTokenException {
        if(externalToken.hasExpired(DateTime.now(DateTimeZone.UTC))) {
            throw new InvalidExternalTokenException();
        }

        externalTokenDAO.storeExternalToken(externalToken);
    }

    @Override
    public Optional<ExternalToken> getTokenByDeviceId(final String deviceId, final Long appId) {
        return externalTokenDAO.getByDeviceId(deviceId, appId);
    }

    @Override
    public void disable(ExternalToken externalToken) {
        externalTokenDAO.disable(externalToken.accessToken);
    }

    public Optional<ExternalToken> getExternalTokenByToken(final String accessToken, final DateTime now) {
        final Optional<ExternalToken> token = cache.getUnchecked(accessToken);
        if(!token.isPresent()) {
            return Optional.absent();
        }
        if(hasExpired(token.get(), now, false)) {
            return Optional.absent();
        }

        return token;
    }

    @Override
    public void disableByRefreshToken(final String refreshToken) {
        externalTokenDAO.disableByRefreshToken(refreshToken);
    }

    private Optional<ExternalToken> fromDB(final String token, final Boolean fromRefreshToken) {

        Optional<ExternalToken> accessTokenOptional;
        if (fromRefreshToken) {
            accessTokenOptional = externalTokenDAO.getByRefreshToken(token);
        } else {
            accessTokenOptional = externalTokenDAO.getByAccessToken(token);
        }

        if(!accessTokenOptional.isPresent()) {
            LOGGER.warn("warning=token_not_found token={}", token);
            return Optional.absent();
        }

        final ExternalToken externalToken = accessTokenOptional.get();

        return accessTokenOptional;
    }

    private Boolean hasExpired(final ExternalToken externalToken, DateTime now, final Boolean isRefreshToken) {
        final Long expiresIn = (isRefreshToken) ? externalToken.refreshExpiresIn : externalToken.accessExpiresIn;
        long diffInSeconds= (now.getMillis() - externalToken.createdAt.getMillis()) / 1000;
        LOGGER.trace("external_token={} for device_id={}", externalToken.accessToken, externalToken.deviceId);
        LOGGER.trace("Token created at = {}", externalToken.createdAt);
        LOGGER.trace("DiffInSeconds = {}", diffInSeconds);
        if(diffInSeconds > expiresIn) {
            LOGGER.warn("warning=expired_token token={} device_id={} secs_since_expiration={}", externalToken.accessToken, externalToken.deviceId, diffInSeconds);
            return true;
        }

        return false;
    }
}
