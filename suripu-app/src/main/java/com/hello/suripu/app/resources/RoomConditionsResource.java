package com.hello.suripu.app.resources;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.models.CurrentRoomState;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/room")
public class RoomConditionsResource {

    private final DeviceDataDAO deviceDataDAO;

    public RoomConditionsResource(final DeviceDataDAO deviceDataDAO) {
        this.deviceDataDAO = deviceDataDAO;
    }


    @GET
    @Path("/current")
    @Produces(MediaType.APPLICATION_JSON)
    public CurrentRoomState current(@Scope({OAuthScope.SENSORS_BASIC}) final AccessToken token) {
        final Optional<DeviceData> data = deviceDataDAO.getMostRecent(token.accountId);
        if(!data.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        final CurrentRoomState roomState = CurrentRoomState.fromDeviceData(data.get());
        return roomState;
    }

}
