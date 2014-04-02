package com.hello.suripu.app.resources;

import com.google.common.base.Optional;
import com.hello.suripu.core.Account;
import com.hello.suripu.core.Gender;
import com.hello.suripu.core.Registration;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.ClientAuthenticationException;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.GrantTypeParam;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.OAuthTokenStore;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/account")
public class AccountResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountResource.class);
    private final AccountDAO accountDAO;
    private final OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> tokenStore;

    public AccountResource(final AccountDAO accountDAO, final OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> tokenStore) {
        this.accountDAO = accountDAO;
        this.tokenStore = tokenStore;
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Account getAccount(
            @Scope({OAuthScope.USER_EXTENDED}) final AccessToken accessToken) {

        LOGGER.debug("{}", accessToken);
        final Optional<Account> account = accountDAO.getById(accessToken.accountId);
        if(!account.isPresent()) {
            LOGGER.warn("Account not present");
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return account.get();
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AccessToken register(@Valid final Registration registration) {
        LOGGER.info("{}", registration.gender);

        final Registration securedRegistration = Registration.encryptPassword(registration);
        final Account account = accountDAO.register(securedRegistration);

        final OAuthScope[] scopes = new OAuthScope[]{OAuthScope.USER_BASIC, OAuthScope.USER_EXTENDED};

        try {
            final AccessToken token = tokenStore.storeAccessToken(
                    new ClientDetails(GrantTypeParam.GrantType.AUTH_CODE,
                    "clientId",
                    "redirectUri",
                    scopes,
                    "state",
                    "code",
                    1L,
                    "secret"));
            return token;

        } catch (ClientAuthenticationException e) {
            throw new WebApplicationException(Response.serverError().build());
        }
    }

    @GET
    @Path("/registration")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Registration fakeRegistration() {
        return new Registration("tim", "bart", "tim@sayhello.com", "123456789", Gender.OTHER, 167.0f, 72.0f, 32, "America/Los_Angeles");
    }
}
