package com.hello.suripu.core.oauth;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryOAuthTokenStore implements OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> {

    private final ConcurrentHashMap<String, ClientDetails> tokens = new ConcurrentHashMap<String, ClientDetails>();
    private final ConcurrentHashMap<String, ClientDetails> codes = new ConcurrentHashMap<String, ClientDetails>();

    private final AtomicLong currentId = new AtomicLong();

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryOAuthTokenStore.class);

    @Override
    public AccessToken storeAccessToken(final ClientDetails clientDetails) throws ClientAuthenticationException {
        // TODO: Generate token here
        final String token = String.valueOf(currentId.incrementAndGet());

        LOGGER.debug("Generated token for {} = {}", clientDetails.accountId, token);
        tokens.put(token, clientDetails);
        final AccessToken accessToken = new AccessToken(token);
        return accessToken;
    }

    @Override
    public Optional<ClientDetails> getClientDetailsByCredentials(final ClientCredentials credentials) {

        final ClientDetails clientDetails = tokens.get(credentials.tokenOrCode);
        if(clientDetails == null) {
            LOGGER.warn("{} was not found in our token store", credentials.tokenOrCode);
            return Optional.absent();
        }

        final Set<OAuthScope> requiredScopes = Sets.newHashSet(credentials.scopes);
        final Set<OAuthScope> grantedScopes = Sets.newHashSet(clientDetails.scopes);

        // Compute intersection of granted scopes to required scopes
        if(Sets.intersection(grantedScopes, requiredScopes).size() == 0) {
            LOGGER.debug("{}", requiredScopes);
            LOGGER.debug("{}", grantedScopes);
            LOGGER.warn("Scopes don't match", credentials.tokenOrCode);
            return Optional.absent();
        }

        return Optional.of(clientDetails);
    }

    @Override
    public ClientCredentials storeAuthorizationCode(final ClientDetails clientDetails) throws ClientAuthenticationException {
        // TODO : generate code here
        final String code = "my new code";
        codes.put(code, clientDetails);
        final OAuthScope[] scopes = new OAuthScope[1];
        scopes[0] = OAuthScope.USER_BASIC;
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


}