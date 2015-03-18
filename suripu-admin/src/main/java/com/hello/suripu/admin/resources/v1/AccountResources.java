package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/v1/account")
public class AccountResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountResources.class);
    private final AccountDAO accountDAO;

    public AccountResources(final AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Account retrieveAccountByEmail(@Scope({OAuthScope.ADMINISTRATION_READ}) final AccessToken accessToken,
                                          @QueryParam("email") final String email) {
        LOGGER.debug("Looking up {}", email);

        final Optional<Account> accountOptional = accountDAO.getByEmail(email);

        if (!accountOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity("Account not found").build());
        }

        else {
            return accountOptional.get();
        }
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/partial")
    public List<Account> retrieveAccountsByEmailPartial(@Scope({OAuthScope.ADMINISTRATION_READ}) final AccessToken accessToken,
                                                        @QueryParam("email") final String emailPartial,
                                                        @QueryParam("name") final String namePartial) {
        if (emailPartial != null) {
            LOGGER.debug("Looking up accounts whose emails contain {}", emailPartial);
            return accountDAO.getByEmailPartial(emailPartial);
        }

        if (namePartial != null) {
            LOGGER.debug("Looking up accounts whose names contain {}", namePartial);
            return accountDAO.getByNamePartial(namePartial);
        }

        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity("Missing email/name partials input").build());
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/recent")
    public List<Account> retrieveRecentlyCreatedAccounts(@Scope({OAuthScope.ADMINISTRATION_READ}) final AccessToken accessToken){
        final List<Account> accounts = accountDAO.getRecent();
        return accounts;
    }
}
