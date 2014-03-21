package com.hello.suripu.app.resources;

import com.google.common.base.Optional;
import com.hello.suripu.core.Account;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.dropwizard.auth.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/account")
public class AccountResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountResource.class);
    private final AccountDAO accountDAO;

    public AccountResource(AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Account getAccount(
            @Scope({OAuthScope.USER_BASIC}) ClientDetails clientDetails) {

        LOGGER.warn("getAccount triggered");
        final Optional<Account> account = accountDAO.getById(clientDetails.accountId);
        if(!account.isPresent()) {
            throw new WebApplicationException();
        }
        return account.get();
    }
}
