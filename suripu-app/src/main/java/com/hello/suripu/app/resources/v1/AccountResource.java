package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountLocationDAO;
import com.hello.suripu.core.db.util.MatcherPatternsDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.PasswordUpdate;
import com.hello.suripu.core.models.Registration;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.JsonError;
import com.yammer.metrics.annotation.Timed;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.regex.Matcher;

@Path("/v1/account")
public class AccountResource extends BaseResource {

    @Context
    HttpServletRequest request;

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountResource.class);
    private final AccountDAO accountDAO;
    private final AccountLocationDAO accountLocationDAO;

    public AccountResource(final AccountDAO accountDAO, final AccountLocationDAO accountLocationDAO) {
        this.accountDAO = accountDAO;
        this.accountLocationDAO = accountLocationDAO;
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Account getAccount(@Scope({OAuthScope.USER_EXTENDED}) final AccessToken accessToken) {

        LOGGER.debug("debug=get-account access_token={}", accessToken);
        final Optional<Account> account = accountDAO.getById(accessToken.accountId);
        if(!account.isPresent()) {
            LOGGER.warn("warning=account-not-present account_id={}", accessToken.accountId);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        LOGGER.info("info=last-modified last_modified={}", account.get().lastModified);
        return account.get();
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Account register(
            @Valid final Registration registration,
            @QueryParam("sig") final String signature) {

        LOGGER.info("info=attempt-to-register-account email={}", registration.email);
        final Optional<Registration.RegistrationError> error = Registration.validate(registration);
        if(error.isPresent()) {
            LOGGER.error("error=registration-failed registration_fail_reason={}.", error.get());
            throw new WebApplicationException(Response.status(400).entity(new JsonError(400, error.get().toString())).build());
        }

        // Overriding email address for kaytlin
        final Registration securedRegistration = Registration.secureAndNormalize(registration);
        LOGGER.info("info=email-after-encryption-and-normalizing email={}", securedRegistration.email);

        try {
            final Account account = accountDAO.register(securedRegistration);
            return account;
        } catch (UnableToExecuteStatementException exception) {

            final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(exception.getMessage());

            if(matcher.find()) {
                LOGGER.warn("warning=register-account-with-existing-email email={}", registration.email);
                throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
                        .entity(new JsonError(409, "Account already exists.")).build());
            }

            LOGGER.error("error=non-unique-email-exception email={}", registration.email);
            LOGGER.error(exception.getMessage());
        }

        throw new WebApplicationException(Response.serverError().build());
    }

    @PUT
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Account modify(
            @Scope({OAuthScope.USER_EXTENDED}) final AccessToken accessToken,
            @Valid final Account account) {

        LOGGER.warn("warning=last-modified account={} last_modified={}", accessToken.accountId, account.lastModified);

        final Optional<Account> optionalAccount = accountDAO.update(account, accessToken.accountId);


        if(!optionalAccount.isPresent()) {
            LOGGER.warn("warning=last-modified-condition-did-not-match-DB-data account_id={} diff_last_modified={}",
                    accessToken.accountId, account.lastModified);
            final JsonError error = new JsonError(Response.Status.PRECONDITION_FAILED.getStatusCode(), "pre condition failed");
            throw new WebApplicationException(Response.status(Response.Status.PRECONDITION_FAILED)
                    .entity(error).build());
        }

        // save location if exists
        if (account.hasLocation()) {
            final String ip = getIpAddress(request);
            try {
                LOGGER.debug("debug=insert-account-location account_id={} latitude={} longitude={} ip_addr={}",
                        accessToken.accountId, account.latitude, account.longitude, ip);
                accountLocationDAO.insertNewAccountLatLongIP(accessToken.accountId, ip, account.latitude, account.longitude);
            } catch (UnableToExecuteStatementException exception) {
                LOGGER.error("error=fail-to-insert-account-location account_id={} latitude={} longitude={} ip_addr={}",
                        accessToken.accountId, account.latitude, account.longitude, ip);
            }
        }

        return optionalAccount.get();
    }

    @POST
    @Timed
    @Path("/password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void password(
            @Scope({OAuthScope.USER_EXTENDED}) final AccessToken accessToken,
            @Valid final PasswordUpdate passwordUpdate) {

        final Optional<Registration.RegistrationError> error = Registration.validatePassword(passwordUpdate.newPassword);
        if(error.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(
                    new JsonError(Response.Status.BAD_REQUEST.getStatusCode(), error.get().toString())).build());
        }

        final PasswordUpdate encrypted = PasswordUpdate.encrypt(passwordUpdate);
        if(!accountDAO.updatePassword(accessToken.accountId, encrypted)) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        };
        // TODO: remove all tokens for this user
    }

    @POST
    @Timed
    @Path("/email")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Account updateEmail(
            @Scope({OAuthScope.USER_EXTENDED}) final AccessToken accessToken,
            @Valid final Account account) {
        LOGGER.info("info=new-email-input email={}", account.email);
        final Account accountWithId = Account.normalizeWithId(account, accessToken.accountId);
        LOGGER.info("info=new-email-after-normalizing email={}", accountWithId.email);
        final Optional<Registration.RegistrationError> error = Registration.validateEmail(accountWithId.email);
        if(error.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(
                    new JsonError(Response.Status.BAD_REQUEST.getStatusCode(), error.get().toString())).build());
        }

        final Optional<Account> accountOptional = accountDAO.updateEmail(accountWithId);
        if(!accountOptional.isPresent()) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        }

        return accountOptional.get();
    }
}
