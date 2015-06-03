package com.hello.suripu.app.resources.v1;


import com.hello.suripu.app.models.AppCheckin;
import com.hello.suripu.app.models.AppCheckinResponse;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v1/app/checkin")
public class AppCheckinResource {

    private final Integer minVersion;

    public AppCheckinResource(final Integer minVersion) {
        this.minVersion= minVersion;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AppCheckinResponse needsUpdate(@Valid final AppCheckin checkin) {

//        if(checkin.platform.equals(AppCheckin.Platform.ANDROID)) {
//            final Boolean needsUpdate = Integer.parseInt(checkin.appVersion) < minVersion;
//            return new AppCheckinResponse(needsUpdate, "You don't need to update", "new app"); // currently returning same app version
//        }

        return new AppCheckinResponse(false, "You don't need to update", checkin.appVersion);
    }
}
