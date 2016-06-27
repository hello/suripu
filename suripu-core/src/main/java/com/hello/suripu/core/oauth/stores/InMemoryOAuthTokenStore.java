package com.hello.suripu.core.oauth.stores;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.hello.suripu.core.oauth.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryOAuthTokenStore implements OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> {

    private final ConcurrentHashMap<String, AccessToken> tokens = new ConcurrentHashMap<String, AccessToken>();
    private final ConcurrentHashMap<String, ClientDetails> codes = new ConcurrentHashMap<String, ClientDetails>();

    private final AtomicLong currentId = new AtomicLong();

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryOAuthTokenStore.class);

    @Override
    public AccessToken storeAccessToken(final ClientDetails clientDetails, final GrantType grantType) throws ClientAuthenticationException {

        if(!clientDetails.application.isPresent()) {
            LOGGER.error("Application was not present");
            throw new ClientAuthenticationException();
        }
        // TODO: Generate token here
        final String token = String.valueOf(currentId.incrementAndGet());

        LOGGER.debug("Generated token for {} = {}", clientDetails.accountId, token);
        AccessToken accessToken = new AccessToken(
                UUID.randomUUID(),
                UUID.randomUUID(),
                300L,
                8640000L,
                DateTime.now(DateTimeZone.UTC),
                clientDetails.accountId,
                clientDetails.application.get().id,
                clientDetails.scopes
        );

        tokens.put(token, accessToken);
        return accessToken;
    }

    @Override
    public Optional<AccessToken> getTokenByClientCredentials(final ClientCredentials credentials, final DateTime now) throws MissingRequiredScopeException {

        final AccessToken accessToken = tokens.get(credentials.tokenOrCode);
        if(accessToken == null) {
            LOGGER.warn("{} was not found in our token store", credentials.tokenOrCode);
            return Optional.absent();
        }

        final Set<OAuthScope> requiredScopes = Sets.newHashSet(credentials.scopes);
        final Set<OAuthScope> grantedScopes = Sets.newHashSet(accessToken.scopes);

        // Make sure we have all the permissions
        if(!grantedScopes.containsAll(requiredScopes)) {
            LOGGER.debug("{}", requiredScopes);
            LOGGER.debug("{}", grantedScopes);
            LOGGER.warn("Scopes don't match", credentials.tokenOrCode);
            throw new MissingRequiredScopeException();
        }

        return Optional.of(accessToken);
    }

    @Override
    public Optional<ClientDetails> getClientDetailsByRefreshToken(String token, DateTime now) throws MissingRequiredScopeException {
        return Optional.absent();
    }

    @Override
    public ClientCredentials storeAuthorizationCode(final ClientDetails clientDetails) throws ClientAuthenticationException {
        // TODO : generate code here
        final String code = "my new code";
        codes.put(code, clientDetails);

        // TODO : read scopes from application table!
        final OAuthScope[] scopes = new OAuthScope[]{OAuthScope.USER_BASIC};
        final ClientCredentials creds = new ClientCredentials(scopes, code);
        return creds;
    }

    @Override
    public Optional<ClientDetails> getClientDetailsByAuthorizationCode(final String code) {
        final ClientDetails clientDetails = codes.get(code);
        if(clientDetails == null) {
            return Optional.absent();
        }
        return Optional.of(clientDetails);
    }

    @Override
    public void disable(AccessToken accessToken) {
        tokens.remove(accessToken.token);
    }

    @Override
    public void disableByRefreshToken(String token) {
        tokens.remove(token);
    }

    @Override
    public void disableAuthCode(UUID authCodeUUID) {

    }
}