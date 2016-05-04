package com.hello.suripu.coredw8.oauth;

import com.google.common.base.Optional;

import com.hello.suripu.core.oauth.MissingRequiredScopeException;
import com.hello.suripu.coredw8.oauth.stores.PersistentAccessTokenStore;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuthAuthenticator implements Authenticator<String, AccessToken> {

    private PersistentAccessTokenStore tokenStore;
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthAuthenticator.class);

    public OAuthAuthenticator(PersistentAccessTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public Optional<AccessToken> authenticate(String submittedToken) throws AuthenticationException {

        try {
            final Optional<AccessToken> token = tokenStore.getAccessTokenByToken(submittedToken, DateTime.now());
            if(!token.isPresent()) {
                LOGGER.warn("warning=token_not_present token={}", submittedToken);
            }
            return token;
        } catch (MissingRequiredScopeException e) {
            throw new MissingRequiredScopeAuthenticationException(e);
        }
    }
}
