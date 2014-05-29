package com.hello.suripu.app.resources;


import com.google.common.base.Optional;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.Application;
import com.hello.suripu.core.oauth.ApplicationRegistration;
import com.hello.suripu.core.oauth.ClientAuthenticationException;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.GrantTypeParam;
import com.hello.suripu.core.oauth.stores.ApplicationStore;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/oauth2")
public class OAuthResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthResource.class);
    private final OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> tokenStore;
    private final ApplicationStore applicationStore;
    private final AccountDAO accountDAO;

    public OAuthResource(
            final OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> tokenStore,
            final ApplicationStore<Application, ApplicationRegistration> applicationStore,
            final AccountDAO accountDAO) {

        this.tokenStore = tokenStore;
        this.applicationStore = applicationStore;
        this.accountDAO = accountDAO;
    }

    @POST
    @Path("/token")
    @Timed
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response accessToken(
                @FormParam("grant_type") GrantTypeParam grantType,
                @FormParam("code") String code,
                @FormParam("redirect_uri") String redirectUri,
                @FormParam("client_id") String clientId,
                @FormParam("client_secret") String clientSecret,
                @FormParam("username") String username,
                @FormParam("password") String password) {

        if(grantType == null) {
            LOGGER.error("GrantType is null");
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid authorization")
                    .type(MediaType.TEXT_PLAIN_TYPE).build();
        }
        if(grantType.getType().equals(GrantTypeParam.GrantType.PASSWORD)) {
            if(username == null || password == null) {
                LOGGER.error("username or password is null");
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid authorization")
                        .type(MediaType.TEXT_PLAIN_TYPE).build();
            }


            final Optional<Account> accountOptional = accountDAO.exists(username, password);
            if(!accountOptional.isPresent()) {
                LOGGER.error("Account wasn't found", username, password);
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid authorization")
                        .type(MediaType.TEXT_PLAIN_TYPE).build();
            }

            final Account account = accountOptional.get();


            // FIXME: this is confusing, are we checking for application, or for installed application for this user
            // FIXME: if that's what we are doing, how did they get a token in the first place?
            // TODO: BE SMARTER
            final Optional<Application> applicationOptional = applicationStore.getApplicationByClientId(clientId);
            if(!applicationOptional.isPresent()) {
                LOGGER.error("application wasn't found for clientId : {}", clientId);
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid authorization")
                        .type(MediaType.TEXT_PLAIN_TYPE).build();
            }

            if(!applicationOptional.get().grantType.equals(grantType.getType())) {
                LOGGER.error("Grant types don't match : {} and {}", applicationOptional.get().grantType, grantType.getType());
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid authorization")
                        .type(MediaType.TEXT_PLAIN_TYPE).build();
            }

            // Important : when using password flow, we should not send / nor expect the client_secret
            final ClientDetails details = new ClientDetails(
                    grantType.getType(),
                    clientId,
                    redirectUri,
                    applicationOptional.get().scopes,
                    "", // state
                    code,
                    account.id,
                    clientSecret
            );

            details.setApp(applicationOptional.get());
            AccessToken accessToken = null;

            try {
                accessToken = tokenStore.storeAccessToken(details);
            } catch (ClientAuthenticationException clientAuthenticationException) {
                LOGGER.error(clientAuthenticationException.getMessage());
                return Response.serverError().build();
            }

            LOGGER.debug("{}", accessToken);
            return Response.ok().entity(accessToken).build();
        }

        // We only support password grant at the moment
        return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid authorization")
            .type(MediaType.TEXT_PLAIN_TYPE).build();
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
}
