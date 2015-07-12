package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.coredw.configuration.EmailConfiguration;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.passwordreset.PasswordReset;
import com.hello.suripu.core.passwordreset.PasswordResetDB;
import com.hello.suripu.core.passwordreset.PasswordResetRequest;
import com.hello.suripu.core.passwordreset.UpdatePasswordRequest;
import com.hello.suripu.core.translations.English;
import com.hello.suripu.core.util.JsonError;
import com.microtripit.mandrillapp.lutung.MandrillApi;
import com.microtripit.mandrillapp.lutung.model.MandrillApiError;
import com.microtripit.mandrillapp.lutung.view.MandrillMessage;
import com.microtripit.mandrillapp.lutung.view.MandrillMessageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Path("/v1/password_reset")
public class PasswordResetResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountResource.class);

    private final AccountDAO accountDAO;
    private final PasswordResetDB passwordResetDB;
    private final MandrillApi mandrillApi;
    private final EmailConfiguration emailConfiguration;


    private PasswordResetResource(final AccountDAO accountDAO, final PasswordResetDB passwordResetDB, final MandrillApi mandrillApi, final EmailConfiguration emailConfiguration) {
        this.accountDAO = accountDAO;
        this.passwordResetDB = passwordResetDB;
        this.mandrillApi = mandrillApi;
        this.emailConfiguration = emailConfiguration;
    }


    /**
     * Instantiate PasswordResetResource with the proper dependencies
     * @param accountDAO
     * @param passwordResetDB
     * @param emailConfiguration
     * @return
     */
    public static PasswordResetResource create(final AccountDAO accountDAO, final PasswordResetDB passwordResetDB, final EmailConfiguration emailConfiguration) {
        final MandrillApi mandrillApi = new MandrillApi(emailConfiguration.apiKey());
        return new PasswordResetResource(accountDAO, passwordResetDB, mandrillApi, emailConfiguration);
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Scope(OAuthScope.PASSWORD_RESET) final AccessToken accessToken, @Valid final PasswordResetRequest passwordResetRequest) {

        final Optional<Account> accountOptional = accountDAO.getByEmail(passwordResetRequest.email.toLowerCase());
        if(!accountOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(new JsonError(Response.Status.NOT_FOUND.getStatusCode(), "account not found")).build());
        }

        final PasswordReset passwordReset = PasswordReset.create(accountOptional.get());
        passwordResetDB.save(passwordReset);
        final Boolean emailSent = sendEmail(accountOptional.get(), passwordReset);
        if(emailSent) {
            return Response.noContent().build();
        }

        return Response.serverError().build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{uuid}/{state}")
    public Response exists(@Scope(OAuthScope.PASSWORD_RESET) final AccessToken accessToken, @PathParam("uuid") final UUID uuid, @PathParam("state") String state) {
        final Optional<PasswordReset> passwordResetOptional = passwordResetDB.get(uuid);

        if(!passwordResetOptional.isPresent()) {
            LOGGER.warn("No password reset found for uuid = {}", uuid);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if(!passwordResetOptional.get().state.equals(state)) {
            LOGGER.warn("State stored ({}) does not match state sent ({})", passwordResetOptional.get().state, state);
            return Response.status(Response.Status.CONFLICT).build();
        }

        return Response.noContent().build();
    }

    @POST
    @Path("/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePassword(@Scope(OAuthScope.PASSWORD_RESET) final AccessToken accessToken, @Valid final UpdatePasswordRequest passwordUpdate) {

        final Optional<UpdatePasswordRequest> updatePasswordRequestOptional = UpdatePasswordRequest.encrypt(passwordUpdate);
        if(!updatePasswordRequestOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(new JsonError(Response.Status.BAD_REQUEST.getStatusCode(), "password not secure enough")).build());
        }

        final Optional<PasswordReset> passwordResetOptional = passwordResetDB.get(updatePasswordRequestOptional.get().uuid);
        if(!passwordResetOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(new JsonError(Response.Status.NOT_FOUND.getStatusCode(), "not found")).build());
        }
        final PasswordReset passwordReset = passwordResetOptional.get();
        final String password = updatePasswordRequestOptional.get().password;
        final String state = updatePasswordRequestOptional.get().state;
        final Boolean updated = accountDAO.updatePasswordFromResetEmail(passwordReset.accountId, password, state);
        if(updated) {
            LOGGER.warn("Password successfully updated for account: {}", passwordReset.accountId);
            final Boolean deleted = passwordResetDB.delete(passwordReset.uuid, passwordReset.accountId);
            LOGGER.debug("Password Request reset deleted");
        }
        return Response.ok().build();
    }

    /**
     *
     * @param account
     * @param passwordReset
     * @return
     */
    private Boolean sendEmail(final Account account, final PasswordReset passwordReset) {

        final String link = String.format("%s/%s/%s", emailConfiguration.linkHost(), passwordReset.uuid.toString(), passwordReset.state);
        final String htmlMessage = String.format(English.EMAIL_PASSWORD_RESET_HTML_TEMPLATE, account.name, link);

        // Grrr mutable objects
        final MandrillMessage message = new MandrillMessage();
        message.setSubject(English.EMAIL_PASSWORD_RESET_SUBJECT);
        message.setHtml(htmlMessage);
        message.setAutoText(true);
        message.setFromEmail(emailConfiguration.emailFrom());
        message.setFromName(emailConfiguration.nameFrom());


        final MandrillMessage.Recipient recipient = new MandrillMessage.Recipient();
        recipient.setEmail(account.email);
        recipient.setName(account.name);

        final List<MandrillMessage.Recipient> recipients = Lists.newArrayList(recipient);
        message.setTo(recipients);

        final List<String> tags = Lists.newArrayList("password_reset");
        message.setTags(tags);

        try {
            final MandrillMessageStatus[] messageStatusReports = mandrillApi.messages().send(message, false);
            return Boolean.TRUE;
        } catch (MandrillApiError mandrillApiError) {
            LOGGER.error("Failed sending email: {}", mandrillApiError.getMessage());
        } catch (IOException e) {
            LOGGER.error("Failed sending email: {}", e.getMessage());
        }

        return Boolean.FALSE;
    }
}
