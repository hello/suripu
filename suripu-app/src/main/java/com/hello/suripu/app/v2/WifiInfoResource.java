package com.hello.suripu.app.v2;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.WifiInfoDAO;
import com.hello.suripu.core.models.WifiInfo;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.util.JsonError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v2/wifi_info")
public class WifiInfoResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(WifiInfoResource.class);
    public static final String WIFI_INFO_HASH_KEY = "wifi_info";

    private final WifiInfoDAO wifiInfoDAO;

    public WifiInfoResource(final WifiInfoDAO wifiInfoDAO) {
        this.wifiInfoDAO = wifiInfoDAO;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{sense_id}")
    public WifiInfo retrieveWifiInfo(@Scope(OAuthScope.WIFI_INFO_READ) final AccessToken accessToken,
                                     @PathParam("sense_id") final String senseId ){

        final Optional<WifiInfo> wifiInfoOptional = wifiInfoDAO.get(senseId);

        if (wifiInfoOptional.isPresent()) {
            return wifiInfoOptional.get();
        }
        throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                .entity(new JsonError(Response.Status.NOT_FOUND.getStatusCode(),
                        String.format("No wifi info for %s", senseId))).build());
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{sense_id}")
    public Response updateWifiInfo(@Scope(OAuthScope.WIFI_INFO_WRITE) final AccessToken accessToken,
                                   @PathParam("sense_id") final String senseId,
                                   final WifiInfo wifiInfo){

        if (wifiInfoDAO.put(wifiInfo)) {
            return Response.noContent().build();
        }
        throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                        String.format("No wifi info for %s", senseId))).build());
    }
}
