package com.hello.suripu.app.resources.v1;


import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.notifications.NotificationSubscriptionDAOWrapper;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.Application;
import com.hello.suripu.core.oauth.ApplicationRegistration;
import com.hello.suripu.core.oauth.ClientAuthenticationException;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.GrantType;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.oauth.stores.ApplicationStore;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.hello.suripu.core.util.PasswordUtil;
import com.hello.suripu.coredw.oauth.GrantTypeParam;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/oauth2")
public class OAuthResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthResource.class);
    private final OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> tokenStore;
    private final ApplicationStore applicationStore;
    private final AccountDAO accountDAO;
    private final NotificationSubscriptionDAOWrapper notificationSubscriptionDAOWrapper;

    public OAuthResource(
            final OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> tokenStore,
            final ApplicationStore<Application, ApplicationRegistration> applicationStore,
            final AccountDAO accountDAO,
            final NotificationSubscriptionDAOWrapper notificationSubscriptionDAOWrapper) {

        this.tokenStore = tokenStore;
        this.applicationStore = applicationStore;
        this.accountDAO = accountDAO;
        this.notificationSubscriptionDAOWrapper = notificationSubscriptionDAOWrapper;
    }

    @POST
    @Path("/token")
    @Timed
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public AccessToken accessToken(
                @FormParam("grant_type") GrantTypeParam grantType,
                @FormParam("code") String code,
                @FormParam("redirect_uri") String redirectUri,
                @FormParam("client_id") String clientId,
                @FormParam("client_secret") String clientSecret,
                @FormParam("username") String username,
                @FormParam("password") String password) {

        if(grantType == null) {
            LOGGER.error("GrantType is null");
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        if(!grantType.getType().equals(GrantType.PASSWORD)) {
            // We only support password grant at the moment
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        if(username == null || password == null || username.isEmpty() || password.isEmpty()) {
            LOGGER.error("username or password is null or empty.");
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        // FIXME: this is confusing, are we checking for application, or for installed application for this user
        // FIXME: if that's what we are doing, how did they get a token in the first place?
        // TODO: BE SMARTER
        final Optional<Application> applicationOptional = applicationStore.getApplicationByClientId(clientId);
        if(!applicationOptional.isPresent()) {
            LOGGER.error("application wasn't found for clientId : {}", clientId);
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        final Application application = applicationOptional.get();
        if (!application.hasScope(OAuthScope.AUTH)) {
            LOGGER.error("application does not have proper scope : {}", clientId);
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        if(!application.grantType.equals(grantType.getType())) {
            LOGGER.error("Grant types don't match : {} and {}", applicationOptional.get().grantType, grantType.getType());
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        final String normalizedUsername = username.toLowerCase();
        LOGGER.debug("normalized username {}", normalizedUsername);
        final Optional<Account> accountOptional = accountDAO.exists(normalizedUsername, password);
        if(!accountOptional.isPresent()) {
            LOGGER.error("Account wasn't found: {}, {}", normalizedUsername, PasswordUtil.obfuscate(password));
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        final Account account = accountOptional.get();

        // Important : when using password flow, we should not send / nor expect the client_secret
        final ClientDetails details = new ClientDetails(
                grantType.getType(),
                clientId,
                redirectUri,
                applicationOptional.get().scopes,
                "", // state
                code,
                account.id.get(),
                clientSecret
        );

        details.setApp(applicationOptional.get());
        AccessToken accessToken = null;

        try {
            accessToken = tokenStore.storeAccessToken(details);
        } catch (ClientAuthenticationException clientAuthenticationException) {
            LOGGER.error(clientAuthenticationException.getMessage());
            throw new WebApplicationException(Response.serverError().build());
        }

        LOGGER.debug("AccessToken {}", accessToken);
        LOGGER.debug("email {}", username);
        return accessToken;
    }

    @GET
    @Path("/authorize")
    @Timed
    public Response code(
            @QueryParam("client_id") String clientId,
            @QueryParam("redirect_uri") String redirectUri,
            @QueryParam("scope") String scope,
            @QueryParam("state") String state) {


        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("service unavailable").build();

//        // TODO : application store
//        // TODO : validate redirect_uri
//        // TODO : validate scope
//        // TODO : validate state
//
//        final OAuthScope[] scopes = new OAuthScope[1];
//        scopes[0] = OAuthScope.USER_BASIC;
//        final ClientDetails clientDetails = new ClientDetails(
//                GrantTypeParam.GrantType.AUTH_CODE,
//                clientId,
//                redirectUri,
//                scopes,
//                state,
//                "",
//                1L,
//                ""
//        );
//
//        ClientCredentials credentials;
//        try {
//            credentials = tokenStore.storeAuthorizationCode(clientDetails);
//        } catch (ClientAuthenticationException e) {
//            LOGGER.error(e.getMessage());
//            return Response.serverError().build();
//        }
//        final String uri = clientDetails.redirectUri + "?code=" + credentials.tokenOrCode;
//        return Response.temporaryRedirect(URI.create(uri)).build();
    }

    @DELETE
    @Path("/token")
    @Timed
    public void delete(@Scope({OAuthScope.AUTH}) final AccessToken accessToken) {
        tokenStore.disable(accessToken);
        LOGGER.debug("AccessToken {} deleted", accessToken);
        if(accessToken.hasScope(OAuthScope.PUSH_NOTIFICATIONS)) {
            LOGGER.debug("AccessToken {} has PUSH_NOTIFICATIONS_SCOPE");
            notificationSubscriptionDAOWrapper.unsubscribe(accessToken.serializeAccessToken());
            LOGGER.debug("Unsubscribed from push notifications");
        }
    }
}
