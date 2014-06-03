package com.hello.suripu.app.resources;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Registration;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/account")
public class AccountResource {

    private static final Pattern PG_UNIQ_PATTERN = Pattern.compile("ERROR: duplicate key value violates unique constraint \"(\\w+)\"");
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountResource.class);
    private final AccountDAO accountDAO;

    public AccountResource(final AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
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
    public Response register(
            @Valid final Registration registration,
            @Scope({OAuthScope.ADMINISTRATION_WRITE}) final AccessToken accessToken) {

        LOGGER.info("Attempting to register account with email: {}", registration.email);

        final Registration securedRegistration = Registration.encryptPassword(registration);
        try {
            final Account account = accountDAO.register(securedRegistration);
            return Response.ok().entity(account).build();
        } catch (UnableToExecuteStatementException exception) {

            final Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());

            if(matcher.find()) {
                return Response.status(409).entity("").type(MediaType.TEXT_PLAIN_TYPE).build();
            }

            LOGGER.error(exception.getMessage());
        }

        return Response.serverError().entity("").type(MediaType.TEXT_PLAIN_TYPE).build();
    }
}
