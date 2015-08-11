package com.hello.suripu.app.v2;

import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.preferences.AccountPreference;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("/v2/account/preferences")
public class AccountPreferencesResource {
    private final AccountPreferencesDAO preferencesDAO;

    public AccountPreferencesResource(final AccountPreferencesDAO preferencesDAO) {
        this.preferencesDAO = preferencesDAO;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<AccountPreference.EnabledPreference, Boolean> get(@Scope(OAuthScope.PREFERENCES) final AccessToken accessToken) {
        return preferencesDAO.get(accessToken.accountId);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<AccountPreference.EnabledPreference, Boolean> put(
            @Scope(OAuthScope.PREFERENCES) final AccessToken accessToken,
            @Valid final Map<AccountPreference.EnabledPreference, Boolean> accountPreference) {
        return preferencesDAO.putAll(accessToken.accountId, accountPreference);
    }
}
