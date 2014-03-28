package com.hello.suripu.app.resources;


import com.google.common.base.Optional;
import com.hello.suripu.core.Account;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.oauth.*;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

@Path("/oauth2")
public class OAuthResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthResource.class);
    private final OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> tokenStore;
    private final ApplicationStore<Application, ClientDetails> applicationStore;
    private final AccountDAO accountDAO;

    public OAuthResource(
            final OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> tokenStore,
            final ApplicationStore<Application, ClientDetails> applicationStore,
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
            final OAuthScope[] scopes = new OAuthScope[]{
                    OAuthScope.USER_BASIC,
                    OAuthScope.USER_EXTENDED,
                    OAuthScope.SENSORS_BASIC,
                    OAuthScope.SENSORS_EXTENDED
            };

            // Important : when using password flow, we should not send / nor expect the client_secret
            final ClientDetails details = new ClientDetails(
                    grantType.getType(),
                    clientId,
                    redirectUri,
                    scopes,
                    "",
                    code,
                    account.id,
                    clientSecret
            );

            final Optional<Application> applicationOptional = applicationStore.getApplication(details, account.id);
            if(!applicationOptional.isPresent()) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid authorization")
                        .type(MediaType.TEXT_PLAIN_TYPE).build();
            }

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


        // TODO : application store
        // TODO : validate redirect_uri
        // TODO : validate scope
        // TODO : validate state

        final OAuthScope[] scopes = new OAuthScope[1];
        scopes[0] = OAuthScope.USER_BASIC;
        final ClientDetails clientDetails = new ClientDetails(
                GrantTypeParam.GrantType.AUTH_CODE,
                clientId,
                redirectUri,
                scopes,
                state,
                "",
                1L,
                ""
        );

        ClientCredentials credentials;
        try {
            credentials = tokenStore.storeAuthorizationCode(clientDetails);
        } catch (ClientAuthenticationException e) {
            LOGGER.error(e.getMessage());
            return Response.serverError().build();
        }
        final String uri = clientDetails.redirectUri + "?code=" + credentials.tokenOrCode;
        return Response.temporaryRedirect(URI.create(uri)).build();
    }
}
