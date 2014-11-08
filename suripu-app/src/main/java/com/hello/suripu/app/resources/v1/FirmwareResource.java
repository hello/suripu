package com.hello.suripu.app.resources.v1;

import com.hello.suripu.core.firmware.FirmwareFile;
import com.hello.suripu.core.firmware.FirmwareUpdateStore;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

@Deprecated
@Path("/v1/firmware")
public class FirmwareResource {

    private FirmwareUpdateStore firmwareUpdateStore;
    public FirmwareResource(final FirmwareUpdateStore firmwareUpdateStore) {
        this.firmwareUpdateStore = firmwareUpdateStore;
    }

    @POST
    @Path("/{device_id}/{firmware_version}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addDownloadFile(
            @Scope(OAuthScope.ADMINISTRATION_WRITE) AccessToken accessToken,
            @Valid FirmwareFile firmwareFile,
            @PathParam("device_id") String deviceId,
            @PathParam("firmware_version") Integer firmwareVersion) {

        final FirmwareFile updated = FirmwareFile.withS3Info(firmwareFile, "hello-firmware", "sense/" + firmwareFile.s3Key);
        firmwareUpdateStore.insertFile(updated, deviceId, firmwareVersion);
    }

    @DELETE
    @Path("/{device_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void reset( @Scope(OAuthScope.ADMINISTRATION_WRITE) AccessToken accessToken, @PathParam("device_id") String deviceId) {

        firmwareUpdateStore.reset(deviceId);
    }
}
