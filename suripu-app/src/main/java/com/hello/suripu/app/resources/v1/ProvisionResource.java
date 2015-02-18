package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.models.ProvisionRequest;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.provision.PillProvision;
import com.hello.suripu.core.provision.PillProvisionDAO;
import com.hello.suripu.core.util.KeyStoreUtils;
import com.hello.suripu.core.util.SenseProvision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/v1/provision")
public class ProvisionResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceResources.class);

    private final KeyStore senseKeyStore;
    private final KeyStore pillKeyStore;
    private final KeyStoreUtils keyStoreUtils;
    private final PillProvisionDAO pillProvisionDAO;

    public ProvisionResource(final KeyStore senseKeyStore,
                             final KeyStore pillKeyStore,
                             final KeyStoreUtils keyStoreUtils,
                             final PillProvisionDAO pillProvisionDAO) {
        this.senseKeyStore = senseKeyStore;
        this.pillKeyStore = pillKeyStore;
        this.keyStoreUtils = keyStoreUtils;
        this.pillProvisionDAO = pillProvisionDAO;
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

    @POST
    @Path("/batch_pills")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    public void batchPillsProvision(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken, @Valid final List<ProvisionRequest> provisionRequests) {
        for (final ProvisionRequest provisionRequest : provisionRequests) {
            pillKeyStore.put(provisionRequest.deviceId, provisionRequest.publicKey, provisionRequest.metadata);
        }
    }


    @POST
    @Path("{serial_number}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response decrypt(@PathParam("serial_number") final String serialNumber, final byte[] body) throws Exception {

        try{
            final Optional<SenseProvision> sense = keyStoreUtils.decrypt(body);
            if(!sense.isPresent()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("KO").type(MediaType.TEXT_PLAIN).build();
            }

            if(sense.isPresent()) {
                final SenseProvision senseProvision = sense.get();
                senseKeyStore.put(senseProvision.deviceIdHex, senseProvision.aesKeyHex, serialNumber);
                final StringBuilder sb = new StringBuilder();
                sb.append("OK\n");
                sb.append(sense.get().deviceIdHex + "\n");
                return Response.ok().entity(sb.toString()).type(MediaType.TEXT_PLAIN).build();
            }
        } catch (Exception e) {
            LOGGER.error("Exception while provisioning Sense: {}", e.getMessage());
            LOGGER.error("Body was : {}", body);
        }
        return Response.serverError().entity("KO").type(MediaType.TEXT_PLAIN).build();
    }


    @GET
    @Path("/check/p/{serial_number}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response checkPillBySerialNumber(@PathParam("serial_number") final String serialNumber) {
        final Optional<PillProvision> pillProvisionOptional = pillProvisionDAO.getBySN(serialNumber);
        if(!pillProvisionOptional.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).entity("KO not found").build();
        }
        final String message = String.format("OK SN created on %s\n", pillProvisionOptional.get().created.toString());
        return Response.ok().entity(message).build();
    }
}
