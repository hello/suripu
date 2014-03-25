package com.hello.suripu.core.oauth;

import com.google.common.base.Optional;

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

    Optional<I> getClientDetailsByCredentials(C credentials);

    C storeAuthorizationCode(I clientDetails) throws ClientAuthenticationException;

    Optional<I> getClientDetailsByAuthorizationCode(String code);
}
