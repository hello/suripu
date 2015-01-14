package com.hello.suripu.admin.resources.v1;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/admin/v1/account")
public class AccountResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountResource.class);

    private final AccountDAO accountDAO;

    public AccountResource(final AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Account getAccount() {
        return null;
    }

    @GET
    @Timed
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public ImmutableList<Account> getAccountByName(@Scope({OAuthScope.ADMINISTRATION_READ}) final AccessToken accessToken,
                                          @PathParam("name") final String name) {
        System.out.println(name);

        final ImmutableList<Account> accounts = accountDAO.getAccountsByNameHint(name);

        return accounts;
    }
    @GET
    @Timed
    @Path("/email/{email}")
    @Produces(MediaType.APPLICATION_JSON)
    public ImmutableList<Account> getAccountByEmail(@Scope({OAuthScope.ADMINISTRATION_READ}) final AccessToken accessToken,
                                                   @PathParam("email") final String email) {
        System.out.println(email);

        final ImmutableList<Account> accounts = accountDAO.getAccountsByEmailHint(email);

        return accounts;
    }
}
