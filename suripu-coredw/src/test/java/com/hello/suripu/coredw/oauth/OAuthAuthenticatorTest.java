package com.hello.suripu.coredw.oauth;

import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;

import org.joda.time.DateTime;
import org.junit.Before;

import java.util.UUID;

import static org.mockito.Mockito.mock;

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

//    @Test
//    public void testValidCredentials() throws AuthenticationException {
//        final DateTime now = DateTime.now();
//        when(oAuthTokenStore.getClientDetailsByToken(credentials, now)).thenReturn(Optional.of(accessToken));
//        final Optional<AccessToken> accessTokenOptional = oAuthAuthenticator.authenticate(credentials);
//        assertThat(accessTokenOptional.isPresent(), is(true));
//    }
//
//    @Test
//    public void testInvalidCredentials() throws AuthenticationException {
//        final DateTime now = DateTime.now();
//        when(oAuthTokenStore.getClientDetailsByToken(credentials, now)).thenReturn(Optional.absent());
//        final Optional<AccessToken> accessTokenOptional = oAuthAuthenticator.authenticate(credentials);
//        assertThat(accessTokenOptional.isPresent(), is(false));
//    }
}
