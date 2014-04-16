package com.hello.suripu.core.oauth;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.AccessTokenDAO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;

public class PersistentAccessTokenStore implements OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials>{

    private final AccessTokenDAO accessTokenDAO;
    private final ApplicationStore<Application, ApplicationRegistration> applicationStore;
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentAccessTokenStore.class);

    public PersistentAccessTokenStore(final AccessTokenDAO accessTokenDAO, final ApplicationStore<Application, ApplicationRegistration> applicationStore) {
        this.accessTokenDAO = accessTokenDAO;
        this.applicationStore = applicationStore;
    }

    @Override
    public AccessToken storeAccessToken(final ClientDetails clientDetails) throws ClientAuthenticationException {

        if(!clientDetails.application.isPresent()) {
            LOGGER.error("ClientDetails should have application for storing access token");
            throw new ClientAuthenticationException();
        }

        final UUID accessTokenUUID = UUID.randomUUID();
        final UUID refreshTokenUUID = UUID.randomUUID();

        LOGGER.debug("AccessToken String = {}", accessTokenUUID.toString());
        LOGGER.debug("RefreshToken String = {}", refreshTokenUUID.toString());


        final AccessToken accessToken = new AccessToken(
                accessTokenUUID,
                refreshTokenUUID,
                86400L * 90, // 90 days
                DateTime.now(DateTimeZone.UTC),
                clientDetails.accountId,
                clientDetails.application.get().id,
                new OAuthScope[]{OAuthScope.SENSORS_BASIC}
        );

        accessTokenDAO.storeAccessToken(accessToken);
        return accessToken;
    }

    @Override
    public Optional<AccessToken> getClientDetailsByToken(final ClientCredentials credentials) {

        // TODO: make sure this is efficient
        final String uuidWithHyphens =  credentials.tokenOrCode.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5" );
        LOGGER.debug("Before transformation : {}", credentials.tokenOrCode);
        LOGGER.debug("After transformation : {}", uuidWithHyphens);
        final Optional<AccessToken> accessTokenOptional = accessTokenDAO.getByAccessToken(UUID.fromString(uuidWithHyphens));

        if(!accessTokenOptional.isPresent()) {
            return Optional.absent();
        }

        Optional<Application> applicationOptional = applicationStore.getApplicationById(accessTokenOptional.get().appId);

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
}
