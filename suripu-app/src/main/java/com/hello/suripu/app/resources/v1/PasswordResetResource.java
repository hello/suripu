package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.passwordreset.PasswordResetDB;
import com.hello.suripu.core.passwordreset.PasswordResetRequest;
import com.hello.suripu.core.passwordreset.PasswordReset;
import com.hello.suripu.core.passwordreset.UpdatePasswordRequest;
import com.hello.suripu.core.util.JsonError;
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
import java.util.UUID;

@Path("/v1/password_reset")
public class PasswordResetResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountResource.class);

    private final AccountDAO accountDAO;
    private final PasswordResetDB passwordResetDB;

    public PasswordResetResource(final AccountDAO accountDAO, final PasswordResetDB passwordResetDB) {
        this.accountDAO = accountDAO;
        this.passwordResetDB = passwordResetDB;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public PasswordReset create(@Scope(OAuthScope.PASSWORD_RESET) final AccessToken accessToken, @Valid final PasswordResetRequest passwordResetRequest) {

        final Optional<Account> accountOptional = accountDAO.getByEmail(passwordResetRequest.email);
        if(!accountOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(new JsonError(Response.Status.NOT_FOUND.getStatusCode(), "account not found")).build());
        }

        final PasswordReset passwordReset = PasswordReset.create(accountOptional.get());
        passwordResetDB.save(passwordReset);
        return passwordReset;
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

        return Response.status(Response.Status.NO_CONTENT).build();
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
        accountDAO.updatePasswordFromResetEmail(passwordReset.accountId, password, state);

        return Response.ok().build();
    }
}
