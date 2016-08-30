package com.hello.suripu.coredropwizard.oauth.stores;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.hello.suripu.coredropwizard.oauth.AccessToken;
import com.hello.suripu.core.oauth.AccessTokenUtils;
import com.hello.suripu.core.oauth.Application;
import com.hello.suripu.core.oauth.ApplicationRegistration;
import com.hello.suripu.core.oauth.ClientAuthenticationException;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.GrantType;
import com.hello.suripu.core.oauth.MissingRequiredScopeException;
import com.hello.suripu.core.oauth.stores.ApplicationStore;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.hello.suripu.coredropwizard.db.AccessTokenDAO;
import com.hello.suripu.coredropwizard.db.AuthorizationCodeDAO;
import com.hello.suripu.coredropwizard.oauth.AuthorizationCode;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * PersistentAccessTokenStore keeps track of assigned access tokens
 */
public class PersistentAccessTokenStore implements OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> {

    private final AccessTokenDAO accessTokenDAO;
    private final ApplicationStore<Application, ApplicationRegistration> applicationStore;
    private final AuthorizationCodeDAO authCodeDAO;
    private final Long expirationTimeInSeconds;

    private static final Long REFRESH_EXPIRATION_TIME_IN_SECONDS = 86400L * 365; // 365 days
    private static final Long ACCESS_EXPIRATION_TIME_IN_SECONDS = 86400L; // 1 day
    private static final Long PASSWORD_GRANT_ACCESS_EXPIRATION_TIME_IN_SECONDS = 86400L * 365; // 365 days
    private static final Logger LOGGER = LoggerFactory.getLogger(com.hello.suripu.core.oauth.stores.PersistentAccessTokenStore.class);

    final LoadingCache<String, Optional<AccessToken>> cache;

    // This is called by the cache when it doesn't contain the key
    final CacheLoader loader = new CacheLoader<String, Optional<AccessToken>>() {
        public Optional<AccessToken> load(final String dirtyToken) throws MissingRequiredScopeException {
            LOGGER.debug("{} not in cache, fetching from DB", dirtyToken);
            return fromDB(dirtyToken, false);
        }
    };

    public PersistentAccessTokenStore(final AccessTokenDAO accessTokenDAO, final ApplicationStore<Application, ApplicationRegistration> applicationStore, final AuthorizationCodeDAO authCodeDAO) {
        this(accessTokenDAO, applicationStore,  authCodeDAO, ACCESS_EXPIRATION_TIME_IN_SECONDS);
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
            final AuthorizationCodeDAO authCodeDAO,
            final Long expirationTimeInSeconds) {
        this.accessTokenDAO = accessTokenDAO;
        this.applicationStore = applicationStore;
        this.authCodeDAO = authCodeDAO;
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

        final Long expiration = (clientDetails.responseType.equals(GrantType.PASSWORD)) ? PASSWORD_GRANT_ACCESS_EXPIRATION_TIME_IN_SECONDS : expirationTimeInSeconds;

        final AccessToken accessToken = generateAccessToken(
            clientDetails,
            DateTime.now(DateTimeZone.UTC), // this is not sent to the client. We store it to expire tokens
            expiration
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
    public Optional<AccessToken> getTokenByClientCredentials(final ClientCredentials credentials, final DateTime now) throws MissingRequiredScopeException {
        return getAccessTokenByToken(credentials.tokenOrCode, now);
    }

    @Override
    public Optional<ClientDetails> getClientDetailsByRefreshToken(final String token, final DateTime now) throws MissingRequiredScopeException {
        final Optional<AccessToken> optionalRefreshToken = fromDB(token, true);
        if(!optionalRefreshToken.isPresent()) {
            return Optional.absent();
        }
        final AccessToken refreshToken = optionalRefreshToken.get();
        if(hasExpired(refreshToken, now, true)) {
            return Optional.absent();
        }

        final Optional<Long> appId = AccessTokenUtils.extractAppIdFromToken(token);
        if (!appId.isPresent()) {
            LOGGER.warn("warning=invalid_token_format refresh_token={}", token);
            return Optional.absent();
        }

        final Optional<Application> optionalApp = applicationStore.getApplicationById(appId.get());
        if (!optionalApp.isPresent()) {
            LOGGER.error("error=invalid-refresh-token-app refresh_token={}", token);
            return Optional.absent();
        }

        final Application app = optionalApp.get();

        final ClientDetails clientDetails = new ClientDetails(
            GrantType.REFRESH_TOKEN,
            app.clientId,
            app.redirectURI,
            refreshToken.scopes,
            "",
            "",
            refreshToken.accountId,
            app.clientSecret
        );
        clientDetails.setApp(app);
        return Optional.of(clientDetails);
    }

    public Optional<AccessToken> getAccessTokenByToken(final String dirtyToken, final DateTime now) throws MissingRequiredScopeException {
        final Optional<AccessToken> token = cache.getUnchecked(dirtyToken);
        if(!token.isPresent()) {
            return Optional.absent();
        }
        if(hasExpired(token.get(), now, false)) {
            return Optional.absent();
        }

        return token;
    }


    @Override
    public ClientCredentials storeAuthorizationCode(final ClientDetails clientDetails) throws ClientAuthenticationException {

        final AuthorizationCode authCode = new AuthorizationCode.Builder()
            .withAuthCode(UUID.randomUUID())
            .withCreatedAt(DateTime.now(DateTimeZone.UTC))
            .withExpiresIn(600L) //10 mins is the max an auth code should be viable
            .withAccountId(clientDetails.accountId)
            .withAppId(clientDetails.application.get().id)
            .withScopes(clientDetails.scopes)
            .build();

        authCodeDAO.storeAuthCode(authCode);
        return new ClientCredentials(authCode.scopes, authCode.serializeAuthCode());
    }

    @Override
    public Optional<ClientDetails> getClientDetailsByAuthorizationCode(final String code) {
        final Optional<UUID> optionalAuthCodeUUID = AccessTokenUtils.cleanUUID(code);
        if(!optionalAuthCodeUUID.isPresent()) {
            LOGGER.warn("warning=invalid_code_format auth_code={}", code);
            return Optional.absent();
        }

        final Optional<Long> appId = AccessTokenUtils.extractAppIdFromToken(code);
        if (!appId.isPresent()) {
            LOGGER.warn("warning=invalid_code_format auth_code={}", code);
            return Optional.absent();
        }

        final Optional<Application> optionalApp = applicationStore.getApplicationById(appId.get());
        if (!optionalApp.isPresent()) {
            LOGGER.error("error=invalid-auth-code-app auth_code={}", code);
            return Optional.absent();
        }

        final Application app = optionalApp.get();

        final UUID authCodeUUID = optionalAuthCodeUUID.get();
        final Optional<AuthorizationCode> optionalAuthCode = authCodeDAO.getByAuthCode(authCodeUUID);
        if(!optionalAuthCode.isPresent()) {
            LOGGER.error("error=get-auth-code-failure auth_code={}", code);
            return Optional.absent();
        }
        final AuthorizationCode authCode = optionalAuthCode.get();
        if(authCode.hasExpired(DateTime.now(DateTimeZone.UTC))) {
            LOGGER.warn("warning=expired-auth-code auth_code={} created_at={}", code, authCode.createdAt);
            return Optional.absent();
        }

        final ClientDetails clientDetails = new ClientDetails(
            GrantType.AUTHORIZATION_CODE,
            app.clientId,
            app.redirectURI,
            authCode.scopes,
            "",
            authCodeUUID.toString(),
            authCode.accountId,
            app.clientSecret
        );
        clientDetails.setApp(app);
        disableAuthCode(authCodeUUID);

        return Optional.of(clientDetails);
    }

    @Override
    public void disableAuthCode(UUID authCodeUUID) {
        authCodeDAO.disableByAuthCode(authCodeUUID);
    }

    @Override
    public void disable(final AccessToken accessToken) {
        accessTokenDAO.disable(accessToken.token);
    }

    @Override
    public void disableByRefreshToken(final String dirtyToken) {
        final Optional<UUID> optionalToken = AccessTokenUtils.cleanUUID(dirtyToken);
        if(!optionalToken.isPresent()) {
            LOGGER.error("error=invalid_refresh_token token={}", dirtyToken);
        }
        accessTokenDAO.disableByRefreshToken(optionalToken.get());
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
                .withRefreshExpiresIn(REFRESH_EXPIRATION_TIME_IN_SECONDS)
                .withCreatedAt(createdAt)
                .withAccountId(clientDetails.accountId)
                .withAppId(clientDetails.application.get().id)
                .withScopes(clientDetails.scopes)
                .build();

        return accessToken;
    }

    private Optional<AccessToken> fromDB(final String dirtyToken, final Boolean fromRefreshToken) throws MissingRequiredScopeException {
        final Optional<UUID> optionalTokenUUID = AccessTokenUtils.cleanUUID(dirtyToken);
        if(!optionalTokenUUID.isPresent()) {
            LOGGER.warn("warning=invalid_token_format token={}", dirtyToken);
            return Optional.absent();
        }

        final UUID tokenUUID = optionalTokenUUID.get();
        Optional<AccessToken> accessTokenOptional;
        if (fromRefreshToken) {
            accessTokenOptional = accessTokenDAO.getByRefreshToken(tokenUUID);
        } else {
            accessTokenOptional = accessTokenDAO.getByAccessToken(tokenUUID);
        }

        if(!accessTokenOptional.isPresent()) {
            LOGGER.warn("warning=token_not_found token={}", tokenUUID);
            return Optional.absent();
        }

        final AccessToken accessToken = accessTokenOptional.get();
        final Optional<Long> optionalAppIdFromToken = AccessTokenUtils.extractAppIdFromToken(dirtyToken);
        if(!optionalAppIdFromToken.isPresent()) {
            LOGGER.warn("warning=invalid_format_app_id token={}", dirtyToken);
            return Optional.absent();
        }

        final Long appIdFromToken = optionalAppIdFromToken.get();

        if(!appIdFromToken.equals(accessToken.appId)) {
            LOGGER.warn("warning=app_id_mismatch token_app_id={} db_app_id={}", appIdFromToken, accessToken.appId);
            return Optional.absent();
        }

        return accessTokenOptional;
    }

    private Boolean hasExpired(final AccessToken accessToken, DateTime now, final Boolean isRefreshToken) {
        final Long expiresIn = (isRefreshToken) ? accessToken.refreshExpiresIn : accessToken.expiresIn;
        long diffInSeconds= (now.getMillis() - accessToken.createdAt.getMillis()) / 1000;
        LOGGER.trace("Token = {} for account_id = {}", accessToken.serializeAccessToken(), accessToken.accountId);
        LOGGER.trace("Token created at = {}", accessToken.createdAt);
        LOGGER.trace("DiffInSeconds = {}", diffInSeconds);
        if(diffInSeconds > expiresIn) {
            LOGGER.warn("warning=expired_token token={} account_id={} secs_since_expiration={}", accessToken.serializeAccessToken(), accessToken.accountId, diffInSeconds);
            return true;
        }

        return false;
    }
}
