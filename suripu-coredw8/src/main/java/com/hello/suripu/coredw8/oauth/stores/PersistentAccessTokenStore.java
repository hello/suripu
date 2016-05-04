package com.hello.suripu.coredw8.oauth.stores;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.hello.suripu.core.oauth.AccessTokenUtils;
import com.hello.suripu.core.oauth.Application;
import com.hello.suripu.core.oauth.ApplicationRegistration;
import com.hello.suripu.core.oauth.ClientAuthenticationException;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.MissingRequiredScopeException;
import com.hello.suripu.core.oauth.stores.ApplicationStore;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.hello.suripu.coredw8.db.AccessTokenDAO;
import com.hello.suripu.coredw8.oauth.AccessToken;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PersistentAccessTokenStore keeps track of assigned access tokens
 */
public class PersistentAccessTokenStore implements OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> {

    private final AccessTokenDAO accessTokenDAO;
    private final ApplicationStore<Application, ApplicationRegistration> applicationStore;
    private final Long expirationTimeInSeconds;

    private static final Long DEFAULT_EXPIRATION_TIME_IN_SECONDS = 86400L * 365; // 365 days
    private static final Logger LOGGER = LoggerFactory.getLogger(com.hello.suripu.core.oauth.stores.PersistentAccessTokenStore.class);

    final LoadingCache<String, Optional<AccessToken>> cache;

    // This is called by the cache when it doesn't contain the key
    final CacheLoader loader = new CacheLoader<String, Optional<AccessToken>>() {
        public Optional<AccessToken> load(final String dirtyToken) throws MissingRequiredScopeException {
            LOGGER.debug("{} not in cache, fetching from DB", dirtyToken);
            return fromDB(dirtyToken);
        }
    };

    public PersistentAccessTokenStore(final AccessTokenDAO accessTokenDAO, final ApplicationStore<Application, ApplicationRegistration> applicationStore) {
        this(accessTokenDAO, applicationStore, DEFAULT_EXPIRATION_TIME_IN_SECONDS);
    }


    /**
     * Default constructor for PersistentAccessTokenStore
     *
     * @param accessTokenDAO where we store or retrieve access tokens from
     * @param applicationStore
     * @param expirationTimeInSeconds
     */
    public PersistentAccessTokenStore(
            final AccessTokenDAO accessTokenDAO,
            final ApplicationStore<Application, ApplicationRegistration> applicationStore,
            final Long expirationTimeInSeconds) {
        this.accessTokenDAO = accessTokenDAO;
        this.applicationStore = applicationStore;
        this.expirationTimeInSeconds = expirationTimeInSeconds;

        // TODO: get expiration from Config
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build(loader);
    }

    /**
     * Validates client details and stores generated accessToken
     * @param clientDetails
     * @return
     * @throws com.hello.suripu.core.oauth.ClientAuthenticationException
     */
    @Override
    public AccessToken storeAccessToken(final ClientDetails clientDetails) throws ClientAuthenticationException {

        if(!clientDetails.application.isPresent()) {
            LOGGER.error("ClientDetails should have application for storing access token");
            throw new ClientAuthenticationException();
        }

        final AccessToken accessToken = generateAccessToken(
                clientDetails,
                DateTime.now(DateTimeZone.UTC), // this is not sent to the client. We store it to expire tokens
                expirationTimeInSeconds
        );

        accessTokenDAO.storeAccessToken(accessToken);
        return accessToken;
    }


    /**
     * Converts the token to a proper UUID and attempts to retrieve the client details based on the token string
     * @param credentials
     * @return
     */
    @Override
    public Optional<AccessToken> getClientDetailsByToken(final ClientCredentials credentials, final DateTime now) throws MissingRequiredScopeException {
        return getAccessTokenByToken(credentials.tokenOrCode, now);
    }

    public Optional<AccessToken> getAccessTokenByToken(final String dirtyToken, final DateTime now) {
        final Optional<AccessToken> token = cache.getUnchecked(dirtyToken);
        if(!token.isPresent()) {
            return Optional.absent();
        }
        if(hasExpired(token.get(), now)) {
            return Optional.absent();
        }

        return token;
    }


    @Override
    public ClientCredentials storeAuthorizationCode(final ClientDetails clientDetails) throws ClientAuthenticationException {
        return new ClientCredentials(null, "code");
    }

    @Override
    public Optional<ClientDetails> getClientDetailsByAuthorizationCode(final String code) {
        return Optional.absent();
    }

    @Override
    public void disable(final AccessToken accessToken) {
        accessTokenDAO.disable(accessToken.token);
    }


    /**
     * Generates an access token
     * @param clientDetails
     * @param createdAt
     * @param expirationTimeInSeconds
     * @return
     */
    private AccessToken generateAccessToken(final ClientDetails clientDetails, final DateTime createdAt, final Long expirationTimeInSeconds) {
        final UUID accessTokenUUID = UUID.randomUUID();
        final UUID refreshTokenUUID = UUID.randomUUID();

        LOGGER.trace("AccessToken String = {}", accessTokenUUID.toString());
        LOGGER.trace("RefreshToken String = {}", refreshTokenUUID.toString());


        final AccessToken accessToken = new AccessToken.Builder()
                .withToken(accessTokenUUID)
                .withRefreshToken(refreshTokenUUID)
                .withExpiresIn(expirationTimeInSeconds)
                .withCreatedAt(createdAt)
                .withAccountId(clientDetails.accountId)
                .withAppId(clientDetails.application.get().id)
                .withScopes(clientDetails.scopes)
                .build();

        return accessToken;
    }

    private Optional<AccessToken> fromDB(final String dirtyToken) throws MissingRequiredScopeException {
        final Optional<UUID> optionalTokenUUID = AccessTokenUtils.cleanUUID(dirtyToken);
        if(!optionalTokenUUID.isPresent()) {
            LOGGER.warn("Invalid format for token {}", dirtyToken);
            return Optional.absent();
        }

        final UUID tokenUUID = optionalTokenUUID.get();
        final Optional<AccessToken> accessTokenOptional = accessTokenDAO.getByAccessToken(tokenUUID);

        if(!accessTokenOptional.isPresent()) {
            LOGGER.warn("{} was not found in accessTokenDAO.getByAccessToken() (UUID)", tokenUUID);
            return Optional.absent();
        }

        final AccessToken accessToken = accessTokenOptional.get();
        final Optional<Long> optionalAppIdFromToken = AccessTokenUtils.extractAppIdFromToken(dirtyToken);
        if(!optionalAppIdFromToken.isPresent()) {
            LOGGER.warn("Invalid appId format for token {}", dirtyToken);
            return Optional.absent();
        }

        final Long appIdFromToken = optionalAppIdFromToken.get();

        if(!appIdFromToken.equals(accessToken.appId)) {
            LOGGER.warn("AppId from token is different from appId retrieved from DB ({} vs {})", appIdFromToken, accessToken.appId);
            return Optional.absent();
        }

        return accessTokenOptional;
    }

    private Boolean hasExpired(final AccessToken accessToken, DateTime now) {
        long diffInSeconds= (now.getMillis() - accessToken.createdAt.getMillis()) / 1000;
        LOGGER.trace("Token = {} for account_id = {}", accessToken.serializeAccessToken(), accessToken.accountId);
        LOGGER.trace("Token created at = {}", accessToken.createdAt);
        LOGGER.trace("DiffInSeconds = {}", diffInSeconds);
        if(diffInSeconds > accessToken.expiresIn) {
            LOGGER.warn("Token {} has expired {} seconds ago (accountId = {})", accessToken.serializeAccessToken(), diffInSeconds, accessToken.accountId);
            return true;
        }

        return false;
    }
}
