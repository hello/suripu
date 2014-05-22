package com.hello.suripu.core.oauth.stores;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.Application;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.GrantTypeParam;
import com.hello.suripu.core.oauth.OAuthScope;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistentAccessTokenStoreTest {

    private AccessTokenDAO accessTokenDAO = mock(AccessTokenDAO.class);
    private ApplicationStore applicationStore = mock(ApplicationStore.class);

    private PersistentAccessTokenStore store;
    private AccessToken accessToken;
    private Application application;

    @Before
    public void setUp() {
        store = new PersistentAccessTokenStore(accessTokenDAO, applicationStore);
        accessToken = new AccessToken.Builder()
                .withAccountId(123L)
                .withAppId(999L)
                .withCreatedAt(DateTime.now(DateTimeZone.UTC))
                .withToken(UUID.randomUUID())
                .withRefreshToken(UUID.randomUUID())
                .withExpiresIn(86400L)
                .withScopes(new OAuthScope[]{})
                .build();

        application = new Application(
                999L,
                "name",
                "client_id",
                "client_secret",
                "http://example.com",
                new OAuthScope[]{OAuthScope.SENSORS_BASIC},
                666L,
                "description",
                false,
                DateTime.now(),
                GrantTypeParam.GrantType.PASSWORD
        );
    }

    @Test
    public void testMissingAccessToken() {
        final UUID uuid = UUID.randomUUID();
        when(accessTokenDAO.getByAccessToken(uuid)).thenReturn(Optional.<AccessToken>absent());

        final ClientCredentials credentials = new ClientCredentials(new OAuthScope[]{}, uuid.toString());
        final Optional<AccessToken> accessTokenOptional = store.getClientDetailsByToken(credentials);
        assertThat(accessTokenOptional.isPresent(), is(false));
    }

    @Test
    public void testPresentAccessTokenButMissingApplication() {
        when(accessTokenDAO.getByAccessToken(accessToken.token)).thenReturn(Optional.of(accessToken));
        when(applicationStore.getApplicationById(accessToken.appId)).thenReturn(Optional.absent());

        final ClientCredentials credentials = new ClientCredentials(new OAuthScope[]{}, accessToken.token.toString());
        final Optional<AccessToken> accessTokenOptional = store.getClientDetailsByToken(credentials);
        assertThat(accessTokenOptional.isPresent(), is(false));
    }

    @Test
    public void testPresentAccessTokenAndPresentApplicationAndWrongScopes() {
        when(accessTokenDAO.getByAccessToken(accessToken.token)).thenReturn(Optional.of(accessToken));
        when(applicationStore.getApplicationById(accessToken.appId)).thenReturn(Optional.of(application));

        final ClientCredentials credentials = new ClientCredentials(new OAuthScope[]{}, accessToken.token.toString());
        final Optional<AccessToken> accessTokenOptional = store.getClientDetailsByToken(credentials);
        assertThat(accessTokenOptional.isPresent(), is(false));
    }

    @Test
    public void testPresentAccessTokenAndPresentApplicationAndValidScopes() {
        when(accessTokenDAO.getByAccessToken(accessToken.token)).thenReturn(Optional.of(accessToken));
        when(applicationStore.getApplicationById(accessToken.appId)).thenReturn(Optional.of(application));

        // scopes match the application scopes
        final ClientCredentials credentials = new ClientCredentials(application.scopes, accessToken.token.toString());
        final Optional<AccessToken> accessTokenOptional = store.getClientDetailsByToken(credentials);
        assertThat(accessTokenOptional.isPresent(), is(true));
    }
}
