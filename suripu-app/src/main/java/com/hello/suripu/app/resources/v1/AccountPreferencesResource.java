package com.hello.suripu.app.resources.v1;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.ApiVersion;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.preferences.AccountPreference;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.preferences.PreferenceName;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

@Path("/v1/preferences")
public class AccountPreferencesResource {
    private final AccountPreferencesDAO preferencesDAO;

    public AccountPreferencesResource(final AccountPreferencesDAO preferencesDAO) {
        this.preferencesDAO = preferencesDAO;
    }

    @VisibleForTesting
    static Map<PreferenceName, Boolean> filterEntries(Map<PreferenceName, Boolean> preferences) {
        return Maps.filterEntries(preferences, new Predicate<Map.Entry<PreferenceName, Boolean>>() {
            @Override
            public boolean apply(Map.Entry<PreferenceName, Boolean> entry) {
                final PreferenceName preference = entry.getKey();
                return (preference.availableStarting == ApiVersion.V1);
            }
        });
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<PreferenceName, Boolean> get(@Scope(OAuthScope.PREFERENCES) final AccessToken accessToken) {
        return filterEntries(preferencesDAO.get(accessToken.accountId));
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public AccountPreference put(
            @Scope(OAuthScope.PREFERENCES) final AccessToken accessToken,
            @Valid final AccountPreference accountPreference) {
        return preferencesDAO.put(accessToken.accountId, accountPreference);
    }
}
