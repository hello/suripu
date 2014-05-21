package com.hello.suripu.core.oauth.stores;

import com.google.common.base.Optional;
import com.hello.suripu.core.oauth.ClientAuthenticationException;

/**
 * Token store
 *
 * @param <T>
 *          the type of the OAuth2.0 access token to use
 * @param <I>
 *          the type of the client details (representing an OAuth2.0 client) to
 *          use
 * @param <C>
 *          the type of the OAuth2.0 authorization code to use
 *
 */
public interface OAuthTokenStore<T, I, C> {

    T storeAccessToken(I clientDetails) throws ClientAuthenticationException;

    Optional<T> getClientDetailsByToken(C creds);

    C storeAuthorizationCode(I clientDetails) throws ClientAuthenticationException;

    Optional<I> getClientDetailsByAuthorizationCode(String code);
}
