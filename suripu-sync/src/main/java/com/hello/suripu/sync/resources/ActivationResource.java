package com.hello.suripu.sync.resources;

import com.google.common.base.Optional;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.core.crypto.CryptoHelper;
import com.hello.suripu.core.db.PublicKeyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/auth")
public class ActivationResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivationResource.class);

    private final PublicKeyStore publicKeyStore;

    public ActivationResource(final PublicKeyStore publicKeyStore) {
        this.publicKeyStore = publicKeyStore;
    }


    @POST
    public Response auth(@Valid final InputProtos.ActivationRequest activationRequest) {

        LOGGER.debug("Device id for activation = {}", activationRequest.getDeviceId());
        final Optional<byte[]> optionalPublicKeyBase64Encoded = publicKeyStore.get(activationRequest.getDeviceId());
        if(!optionalPublicKeyBase64Encoded.isPresent()) {
            LOGGER.warn("Error validating device_id = {}. Could not find public key", activationRequest.getDeviceId());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        final boolean validSignature = CryptoHelper.validate(
                activationRequest.toByteArray(),
                activationRequest.getSignature().toByteArray(),
                optionalPublicKeyBase64Encoded.get()
        );

        if(!validSignature) {
            LOGGER.warn("Error validating device_id = {}. Signature doesn't match {}", activationRequest.getDeviceId(), activationRequest.getSignature());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        final byte[] encrypted = activationRequest.toByteArray();
        return Response.status(Response.Status.FORBIDDEN).entity(encrypted).build();
    }
}
