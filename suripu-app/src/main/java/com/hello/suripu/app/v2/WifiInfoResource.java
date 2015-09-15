package com.hello.suripu.app.v2;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.WifiInfoDAO;
import com.hello.suripu.core.models.DeviceAccountPair;
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
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v2/wifi_info")
public class WifiInfoResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(WifiInfoResource.class);
    public static final String WIFI_INFO_HASH_KEY = "wifi_info";

    private final WifiInfoDAO wifiInfoDAO;
    private final DeviceDAO deviceDAO;

    public WifiInfoResource(final WifiInfoDAO wifiInfoDAO, final DeviceDAO deviceDAO) {
        this.wifiInfoDAO = wifiInfoDAO;
        this.deviceDAO = deviceDAO;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public WifiInfo retrieveWifiInfo(@Scope(OAuthScope.WIFI_INFO_READ) final AccessToken accessToken){

        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accessToken.accountId);

        if (deviceAccountPairOptional.isPresent()) {
            final String senseId = deviceAccountPairOptional.get().externalDeviceId;
            final Optional<WifiInfo> wifiInfoOptional = wifiInfoDAO.get(senseId);
            if (wifiInfoOptional.isPresent()) {
                return wifiInfoOptional.get();
            }
        }
        throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(new JsonError(404, "Not found")).build());
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateWifiInfo(@Scope(OAuthScope.WIFI_INFO_WRITE) final AccessToken accessToken,
                                   final WifiInfo wifiInfo){
        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accessToken.accountId);

        if (deviceAccountPairOptional.isPresent()) {
            final String mostRecentlyPairedSenseId = deviceAccountPairOptional.get().externalDeviceId;
            if (mostRecentlyPairedSenseId.equals(wifiInfo.senseId)) {
                if (wifiInfoDAO.put(wifiInfo)) {
                    return Response.noContent().build();
                }
            }
            else {
                LOGGER.debug("Account {} attempted to update wifi info for not sense {} but the most recently paired sense is {}",
                        accessToken.accountId, wifiInfo.senseId, mostRecentlyPairedSenseId);
            }
        }
        throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new JsonError(500, "Internal server error")).build());
    }
}
