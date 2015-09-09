package com.hello.suripu.coredw8.oauth;

import com.google.common.base.Optional;
import com.hello.suripu.coredw8.oauth.stores.PersistentAccessTokenStore;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
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

        final Optional<AccessToken> token = tokenStore.getAccessTokenByToken(submittedToken);

        if(!token.isPresent()) {
            LOGGER.warn("Token {} was not present in OAuthAuthenticator", submittedToken);
        }
        return token;
    }
}
