package com.hello.suripu.core.oauth;

import com.google.common.base.Optional;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.yammer.dropwizard.auth.AuthenticationException;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OAuthAuthenticatorTest {

    private OAuthTokenStore oAuthTokenStore = mock(OAuthTokenStore.class);
    private OAuthAuthenticator oAuthAuthenticator;
    private OAuthScope[] scopes;
    private String token;
    private ClientCredentials credentials;
    private AccessToken accessToken;

    @Before
    public void setUp() {
        oAuthAuthenticator = new OAuthAuthenticator(oAuthTokenStore);
        scopes = new OAuthScope[]{OAuthScope.SENSORS_BASIC};
        token = "fake-token-string";
        credentials = new ClientCredentials(scopes, token);

        accessToken = new AccessToken(
                UUID.randomUUID(), //token
                UUID.randomUUID(), //refreshToken
                DateTime.now().plusSeconds(10).getMillis(), // expires_in
                DateTime.now(), //createdAt,
                123L, // account_id
                999L, // app_id
                scopes // scopes
        );
    }

    @Test
    public void testValidCredentials() throws AuthenticationException {

        when(oAuthTokenStore.getClientDetailsByToken(credentials)).thenReturn(Optional.of(accessToken));
        final Optional<AccessToken> accessTokenOptional = oAuthAuthenticator.authenticate(credentials);
        assertThat(accessTokenOptional.isPresent(), is(true));
    }

    @Test
    public void testInvalidCredentials() throws AuthenticationException {
        when(oAuthTokenStore.getClientDetailsByToken(credentials)).thenReturn(Optional.absent());
        final Optional<AccessToken> accessTokenOptional = oAuthAuthenticator.authenticate(credentials);
        assertThat(accessTokenOptional.isPresent(), is(false));
    }
}
