package com.hello.suripu.app.resources;


import com.google.common.base.Optional;
import com.hello.suripu.core.oauth.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/oauth")
public class OAuthResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthResource.class);
    private final OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> tokenStore;

    public OAuthResource(OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> tokenStore) {
        this.tokenStore = tokenStore;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public AccessToken accessToken(
                @FormParam("grant_type") GrantTypeParam grantType,
                @FormParam("code") String code,
                @FormParam("redirect_uri") String redirectUri,
                @FormParam("client_id") String clientId,
                @FormParam("client_secret") String clientSecret) {

            Optional<ClientDetails> opt = tokenStore.getClientDetailsByAuthorizationCode(code);
            if (!opt.isPresent()) {
                throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity("Invalid authorization")
                        .type(MediaType.TEXT_PLAIN_TYPE).build());
            }

            ClientDetails clientDetails = opt.get();
            LOGGER.debug(String.format("Handing out access token for client %s with secret %s", clientId, clientSecret));
            try {
                return tokenStore.storeAccessToken(clientDetails);
            } catch (ClientAuthenticationException exception) {
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Server Error").build());
            }
    }

    @POST
    @Path("/stuff")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public AccessToken passwordGrant() {
        return new AccessToken("123456789");
    }
}
