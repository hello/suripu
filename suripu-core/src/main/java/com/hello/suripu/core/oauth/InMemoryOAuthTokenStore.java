package com.hello.suripu.core.oauth;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOAuthTokenStore implements OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> {

    private final ConcurrentHashMap<String, ClientDetails> tokens = new ConcurrentHashMap<String, ClientDetails>();
    private final ConcurrentHashMap<String, ClientDetails> codes = new ConcurrentHashMap<String, ClientDetails>();

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryOAuthTokenStore.class);

    @Override
    public AccessToken storeAccessToken(final ClientDetails clientDetails) throws ClientAuthenticationException {
        // TODO: Generate token here
        final String token = "hello";
        tokens.put(token, clientDetails);
        final AccessToken accessToken = new AccessToken(token);
        return accessToken;
    }

    @Override
    public Optional<ClientDetails> getClientDetailsByCredentials(final ClientCredentials credentials) {


        final ClientDetails clientDetails = tokens.get(credentials.token);
        if(clientDetails == null) {
            LOGGER.warn("{} was not found in our token store", credentials.token);
            return Optional.absent();
        }

        // TODO implement real validation
        if(clientDetails.scopes.length == 0) {
            return Optional.absent();
        }

        return Optional.of(clientDetails);
    }

    @Override
    public ClientCredentials storeAuthorizationCode(final ClientDetails clientDetails) throws ClientAuthenticationException {
        //TODO : generate code here
        final String code = "my new code";
        codes.put(code, clientDetails);
        OAuthScope[] scopes = new OAuthScope[1];
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