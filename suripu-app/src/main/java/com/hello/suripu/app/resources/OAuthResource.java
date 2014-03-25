package com.hello.suripu.app.resources;


import com.google.common.base.Optional;
import com.hello.suripu.core.oauth.*;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

@Path("/oauth")
public class OAuthResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthResource.class);
    private final OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> tokenStore;

    public OAuthResource(OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> tokenStore) {
        this.tokenStore = tokenStore;
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response accessToken(
                @FormParam("grant_type") GrantTypeParam grantType,
                @FormParam("code") String code,
                @FormParam("redirect_uri") String redirectUri,
                @FormParam("client_id") String clientId,
                @FormParam("client_secret") String clientSecret,
                @FormParam("username") String username,
                @FormParam("password") String password) {

            final Optional<ClientDetails> opt = tokenStore.getClientDetailsByAuthorizationCode(code);

            if (!opt.isPresent()) {
                throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity("Invalid authorization")
                        .type(MediaType.TEXT_PLAIN_TYPE).build());
            }

            final ClientDetails clientDetails = opt.get();
            LOGGER.debug(String.format("Handing out access token for client %s with secret %s", clientId, clientSecret));
            try {
                return Response.ok().entity(tokenStore.storeAccessToken(clientDetails)).build();
            } catch (ClientAuthenticationException exception) {
                LOGGER.error(exception.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Server Error").build();
            }
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
                "responseType",
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
