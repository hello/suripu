package com.hello.suripu.admin.resources.v1;


import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.Application;
import com.hello.suripu.core.oauth.ApplicationRegistration;
import com.hello.suripu.core.oauth.ClientAuthenticationException;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.ImplicitTokenRequest;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.oauth.stores.ApplicationStore;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.hello.suripu.core.util.HelloHttpHeader;
import com.hello.suripu.core.util.JsonError;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/token")
public class TokenResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenResources.class);
    private final OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> tokenStore;
    private final ApplicationStore applicationStore;
    private final AccountDAO accountDAO;

    @Context
    HttpServletRequest request;
    public TokenResources(final OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> tokenStore,
                          final ApplicationStore<Application, ApplicationRegistration> applicationStore,
                          final AccountDAO accountDAO) {

        this.tokenStore = tokenStore;
        this.applicationStore = applicationStore;
        this.accountDAO = accountDAO;
    }



    @POST
    @Path("/implicit")
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AccessToken accessToken(@Scope({OAuthScope.IMPLICIT_TOKEN}) final AccessToken accessToken,
                                   final ImplicitTokenRequest implicitTokenRequest) {
        LOGGER.debug("Raw implicit token request {}", implicitTokenRequest);
        String requesterEmail = this.request.getHeader(HelloHttpHeader.ADMIN);

        if (requesterEmail == null || !requesterEmail.endsWith("@sayhello.com")) {
            LOGGER.error("Unauthorized attempt to generate token from email {}", requesterEmail);
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        final Optional<Account> accountOptional = accountDAO.getByEmail(implicitTokenRequest.email);
        if (!accountOptional.isPresent()) {
            LOGGER.error("Account not found for email {}", implicitTokenRequest.email);
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        final Long accountId = accountOptional.get().id.get();

        LOGGER.info("Admin {} attempts to generate token on behalf of account {} - email {}", requesterEmail, accountId, implicitTokenRequest.email);


        final Optional<Application> applicationOptional = applicationStore.getApplicationByClientId(implicitTokenRequest.clientId);
        if (!applicationOptional.isPresent()) {
            LOGGER.error("application wasn't found for clientId : {}", implicitTokenRequest.clientId);
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        final Application application = applicationOptional.get();
        if (!application.hasScope(OAuthScope.AUTH)) {
            LOGGER.error("application does not have proper scope : {}", implicitTokenRequest.clientId);
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        final ClientDetails details = ClientDetails.createWithImplicitGrantType(application, accountId);

        details.setApp(application);
        AccessToken implicitToken;
        try {
            implicitToken = tokenStore.storeAccessToken(details);
        } catch (ClientAuthenticationException e) {
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                            String.format("Failed to generate token on behalf of account {} because {}", implicitTokenRequest.email, e.getMessage()))).build());
        }

        LOGGER.debug("Admin {} created implicit token {} for account {}", requesterEmail, implicitToken.toString(), implicitTokenRequest.email);
        return implicitToken;

    }
}
