package com.hello.suripu.coredw.oauth;

import com.google.common.base.Optional;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.MissingRequiredScopeException;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.yammer.dropwizard.auth.AuthenticationException;
import com.yammer.dropwizard.auth.Authenticator;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuthAuthenticator implements Authenticator<ClientCredentials, AccessToken> {
    private OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> tokenStore;
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthAuthenticator.class);

    public OAuthAuthenticator(OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> tokenStore) {
        super();
        this.tokenStore = tokenStore;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.yammer.dropwizard.auth.Authenticator#authenticate(java.lang.Object)
     */
    @Override
    public Optional<AccessToken> authenticate(ClientCredentials credentials) throws AuthenticationException {


//        final String stuff = bearer;
    /*
     * Hook for the application to enforce that a REST call can only access the
     * data for the student that authorized the client application (which we
     * currently don't do)
     */
        final Optional<AccessToken> token;
        try {
            token = tokenStore.getTokenByClientCredentials(credentials, DateTime.now());
            if(!token.isPresent()) {
                LOGGER.warn("Token {} was not present in OAuthAuthenticator", credentials.tokenOrCode);
            }
            return token;
        } catch (MissingRequiredScopeException e) {
            throw new MissingRequiredScopeAuthenticationException(e);
        }
    }
}
