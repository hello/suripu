package com.hello.suripu.core.oauth;

import com.google.common.base.Optional;
import com.yammer.dropwizard.auth.AuthenticationException;
import com.yammer.dropwizard.auth.Authenticator;

public class OAuthAuthenticator implements Authenticator<ClientCredentials, ClientDetails>{
    private OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> tokenStore;

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
    public Optional<ClientDetails> authenticate(ClientCredentials credentials) throws AuthenticationException {


//        final String stuff = bearer;
    /*
     * Hook for the application to enforce that a REST call can only access the
     * data for the student that authorized the client application (which we
     * currently don't do)
     */
        return tokenStore.getClientDetailsByCredentials(credentials);
    }
}
