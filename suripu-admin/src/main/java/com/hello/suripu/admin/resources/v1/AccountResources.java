package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.admin.Util;
import com.hello.suripu.admin.models.PasswordResetAdmin;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.passwordreset.PasswordReset;
import com.hello.suripu.core.passwordreset.PasswordResetDB;
import com.hello.suripu.core.util.JsonError;
import com.hello.suripu.core.util.PasswordUtil;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/v1/account")
public class AccountResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountResources.class);
    private final Integer DEFAULT_RECENT_USERS_LIMIT = 100;
    private final Integer MAX_RECENT_USERS_LIMIT = 500;

    private final AccountDAO accountDAO;
    private final PasswordResetDB passwordResetDB;
    final DeviceDAO deviceDAO;

    public AccountResources(final AccountDAO accountDAO, final PasswordResetDB passwordResetDB, final DeviceDAO deviceDAO) {
        this.accountDAO = accountDAO;
        this.passwordResetDB = passwordResetDB;
        this.deviceDAO = deviceDAO;
    }


    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Account retrieveAccountByEmailOrId(@Scope({OAuthScope.ADMINISTRATION_READ}) final AccessToken accessToken,
                                              @QueryParam("email") final String email,
                                              @QueryParam("id") final Long id) {

        if (email == null &&  id == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity("Missing query params, please specify email or id").build());
        }

        else if (email != null) {
            LOGGER.debug("Looking account up by email {}", email);
            final Optional<Account> accountByEmailOptional = accountDAO.getByEmail(email.toLowerCase());
            if (!accountByEmailOptional.isPresent()) {
                throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Account not found").build());
            }
            return accountByEmailOptional.get();
        }
        else {
            LOGGER.debug("Looking up account by id {}", id);
            final Optional<Account> accountByIdOptional = accountDAO.getById(id);
            if (!accountByIdOptional.isPresent()) {
                throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Account not found").build());
            }
            return accountByIdOptional.get();
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
    public List<Account> retrieveRecentlyCreatedAccounts(@Scope({OAuthScope.ADMINISTRATION_READ}) final AccessToken accessToken,
                                                         @QueryParam("limit") final Integer limit){
        if (limit == null) {
            return accountDAO.getRecent(DEFAULT_RECENT_USERS_LIMIT);
        }
        return accountDAO.getRecent(Math.min(limit, MAX_RECENT_USERS_LIMIT));
    }


    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/update_password")
    public Response passwordUpdate(@Scope({OAuthScope.ADMINISTRATION_WRITE, OAuthScope.PASSWORD_RESET}) final AccessToken accessToken, final PasswordResetAdmin passwordResetAdmin) {

        LOGGER.debug("Admin {} attempts to set passsword for email {}", accessToken.accountId, passwordResetAdmin.email);
        final Optional<Account> accountOptional = accountDAO.getByEmail(passwordResetAdmin.email.toLowerCase());
        if(!accountOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(new JsonError(Response.Status.NOT_FOUND.getStatusCode(), "account not found")).build());
        }

        final PasswordReset passwordReset = PasswordReset.create(accountOptional.get());
        passwordResetDB.save(passwordReset);

        if (passwordResetAdmin.password.length() < 6){
            throw new WebApplicationException(Response.status(Response.Status.NOT_ACCEPTABLE).entity("Password length should be greater than 6").build());
        }

        final Boolean updated = accountDAO.updatePasswordFromResetEmail(passwordReset.accountId, PasswordUtil.encrypt(passwordResetAdmin.password), passwordReset.state);

        if(updated) {
            return Response.noContent().build();
        }

        return Response.serverError().build();
    }


    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{email}/partner")
    public Account retrievePartnerAccount(@Scope({OAuthScope.ADMINISTRATION_READ}) final AccessToken accessToken,
                                          @PathParam("email") final String email){

        final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);

        if (!accountIdOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(404, "Account not found")).build());
        }

        final Optional<Long> partnerAccountIdOptional = deviceDAO.getPartnerAccountId(accountIdOptional.get());

        if (!partnerAccountIdOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(404, "Partner account not found")).build());
        }

        return accountDAO.getById(partnerAccountIdOptional.get()).get();
    }
}
