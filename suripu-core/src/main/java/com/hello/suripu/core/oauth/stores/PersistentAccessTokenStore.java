package com.hello.suripu.core.oauth.stores;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.Application;
import com.hello.suripu.core.oauth.ApplicationRegistration;
import com.hello.suripu.core.oauth.ClientAuthenticationException;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.OAuthScope;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;

/**
 * PersistentAccessTokenStore keeps track of assigned access tokens
 */
public class PersistentAccessTokenStore implements OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials>{

    private final AccessTokenDAO accessTokenDAO;
    private final ApplicationStore<Application, ApplicationRegistration> applicationStore;
    private final Long expirationTimeInSeconds;

    private static final Long DEFAULT_EXPIRATION_TIME_IN_SECONDS = 86400L * 90; // 90 days
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentAccessTokenStore.class);

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
     * Converts the token to a proper UUID and attempts to retrieve the clientdetails based on the token string
     * @param credentials
     * @return
     */
    @Override
    public Optional<AccessToken> getClientDetailsByToken(final ClientCredentials credentials) {

        // TODO: make sure this is efficient
        final String uuidWithHyphens =  credentials.tokenOrCode.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5" );
        final Optional<AccessToken> accessTokenOptional = accessTokenDAO.getByAccessToken(UUID.fromString(uuidWithHyphens));

        if(!accessTokenOptional.isPresent()) {
            return Optional.absent();
        }

        final Optional<Application> applicationOptional = applicationStore.getApplicationById(accessTokenOptional.get().appId);

        if(!applicationOptional.isPresent()) {
            return Optional.absent();
        }

        final Set<OAuthScope> requiredScopes = Sets.newHashSet(credentials.scopes);
        final Set<OAuthScope> grantedScopes = Sets.newHashSet(applicationOptional.get().scopes);

        // Make sure we have all the permissions
        if(!grantedScopes.containsAll(requiredScopes)) {
            LOGGER.debug("Required: {}", requiredScopes);
            LOGGER.debug("Granted: {}", grantedScopes);
            LOGGER.warn("Scopes don't match for {}", credentials.tokenOrCode);
            return Optional.absent();
        }

        return accessTokenOptional;
    }


    @Override
    public ClientCredentials storeAuthorizationCode(final ClientDetails clientDetails) throws ClientAuthenticationException {
        return new ClientCredentials(null, "code");
    }

    @Override
    public Optional<ClientDetails> getClientDetailsByAuthorizationCode(final String code) {
        return Optional.absent();
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

        LOGGER.debug("AccessToken String = {}", accessTokenUUID.toString());
        LOGGER.debug("RefreshToken String = {}", refreshTokenUUID.toString());


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
}
