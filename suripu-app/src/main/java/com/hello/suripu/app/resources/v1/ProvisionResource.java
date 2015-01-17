package com.hello.suripu.app.resources.v1;

import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.models.ProvisionRequest;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v1/provision")
public class ProvisionResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceResources.class);

    private final KeyStore senseKeyStore;
    private final KeyStore pillKeyStore;

    public ProvisionResource(final KeyStore senseKeyStore,
                             final KeyStore pillKeyStore) {
        this.senseKeyStore = senseKeyStore;
        this.pillKeyStore = pillKeyStore;
    }

    @POST
    @Path("/sense")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    public void senseProvision(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken, @Valid final ProvisionRequest provisionRequest) {
        senseKeyStore.put(provisionRequest.deviceId, provisionRequest.publicKey, provisionRequest.metadata);
    }

    @POST
    @Path("/pill")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    public void pillProvision(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken, @Valid final ProvisionRequest provisionRequest) {
        pillKeyStore.put(provisionRequest.deviceId, provisionRequest.publicKey, provisionRequest.metadata);
    }
}
