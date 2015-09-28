package com.hello.suripu.core.oauth.stores;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.AccessTokenUtils;
import com.hello.suripu.core.oauth.Application;
import com.hello.suripu.core.oauth.ApplicationRegistration;
import com.hello.suripu.core.oauth.ClientAuthenticationException;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.MissingRequiredScopeException;
import com.hello.suripu.core.oauth.OAuthScope;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * PersistentAccessTokenStore keeps track of assigned access tokens
 */
public class PersistentAccessTokenStore implements OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials>{

    private final AccessTokenDAO accessTokenDAO;
    private final ApplicationStore<Application, ApplicationRegistration> applicationStore;
    private final Long expirationTimeInSeconds;

    private static final Long DEFAULT_EXPIRATION_TIME_IN_SECONDS = 86400L * 365; // 365 days
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentAccessTokenStore.class);

    final LoadingCache<ClientCredentials, Optional<AccessToken>> cache;

    // This is called by the cache when it doesn't contain the key
    final CacheLoader loader = new CacheLoader<ClientCredentials, Optional<AccessToken>>() {
        public Optional<AccessToken> load(final ClientCredentials clientCredentials) throws MissingRequiredScopeException {
            LOGGER.debug("{} not in cache, fetching from DB", clientCredentials.tokenOrCode);
            return fromDB(clientCredentials);
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

    private Optional<AccessToken> getToken(final ClientCredentials credentials) throws MissingRequiredScopeException {
        try {
            return cache.getUnchecked(credentials);
        } catch (UncheckedExecutionException e) {
            if (e.getCause().getClass() == MissingRequiredScopeException.class) {
                throw new MissingRequiredScopeException();
            }
            throw e;
        }
    }

    /**
     * Converts the token to a proper UUID and attempts to retrieve the client details based on the token string
     * @param credentials
     * @return
     */
    @Override
    public Optional<AccessToken> getClientDetailsByToken(final ClientCredentials credentials, final DateTime now) throws MissingRequiredScopeException {
        final Optional<AccessToken> token = getToken(credentials);

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

    public boolean hasRequiredScopes(OAuthScope[] granted, OAuthScope[] required) {
        if(granted.length == 0 || required.length == 0) {
            LOGGER.warn("Empty scopes is definitely not valid");
            return false;
        }

        final Set<OAuthScope> requiredScopes = Sets.newHashSet(required);
        final Set<OAuthScope> grantedScopes = Sets.newHashSet(granted);

        // Make sure we have all the permissions
        boolean valid = grantedScopes.containsAll(requiredScopes);
        if(!valid) {
            LOGGER.warn("Required: {}, granted: {}", requiredScopes, grantedScopes);
        }

        return valid;
    }

    private Optional<AccessToken> fromDB(final ClientCredentials credentials) throws MissingRequiredScopeException {
        final Optional<UUID> optionalTokenUUID = AccessTokenUtils.cleanUUID(credentials.tokenOrCode);
        if(!optionalTokenUUID.isPresent()) {
            LOGGER.warn("Invalid format for token {}", credentials.tokenOrCode);
            return Optional.absent();
        }

        final UUID tokenUUID = optionalTokenUUID.get();
        final Optional<AccessToken> accessTokenOptional = accessTokenDAO.getByAccessToken(tokenUUID);

        if(!accessTokenOptional.isPresent()) {
            LOGGER.warn("{} was not found in accessTokenDAO.getByAccessToken() (UUID)", tokenUUID);
            return Optional.absent();
        }

        final AccessToken accessToken = accessTokenOptional.get();
        final Optional<Long> optionalAppIdFromToken = AccessTokenUtils.extractAppIdFromToken(credentials.tokenOrCode);
        if(!optionalAppIdFromToken.isPresent()) {
            LOGGER.warn("Invalid appId format for token {}", credentials.tokenOrCode);
            return Optional.absent();
        }

        final Long appIdFromToken = optionalAppIdFromToken.get();

        if(!appIdFromToken.equals(accessToken.appId)) {
            LOGGER.warn("AppId from token is different from appId retrieved from DB ({} vs {})", appIdFromToken, accessToken.appId);
            return Optional.absent();
        }

        final Optional<Application> applicationOptional = applicationStore.getApplicationById(accessToken.appId);

        if(!applicationOptional.isPresent()) {
            LOGGER.warn("No application with id = {} as specified by token {}", accessToken.appId, credentials.tokenOrCode);
            return Optional.absent();
        }

        boolean validScopes = hasRequiredScopes(applicationOptional.get().scopes, credentials.scopes);
        if(!validScopes) {
            LOGGER.warn("Scopes don't match for {}", credentials.tokenOrCode);
            throw new MissingRequiredScopeException();
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
