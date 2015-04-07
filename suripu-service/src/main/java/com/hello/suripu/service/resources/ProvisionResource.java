package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.api.provision.ProvisionProtos;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.processors.OTAProcessor;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.service.SignedMessage;
import com.librato.rollout.RolloutClient;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Random;

@Path("/provision")
public class ProvisionResource extends BaseResource {

    @Inject
    RolloutClient featureFlipper;

    private final KeyStore keyStore;

    @Context
    HttpServletRequest request;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionResource.class);


    public ProvisionResource(final KeyStore senseKeyStore) {
        this.keyStore = senseKeyStore;
    }


    @POST
    @Path("/keys")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] provision(final byte[] body) {

        final String ipAddress = getIpAddress(request);
        final Boolean isPCH = OTAProcessor.isPCH(ipAddress);
        final Boolean isHelloOffice = OTAProcessor.isHelloOffice(ipAddress);

        if(!isPCH || !isHelloOffice) {
            LOGGER.warn("Attempting to get keys from ip={}", ipAddress);
            return plainTextError(Response.Status.NOT_FOUND, "not found");
        }

        ProvisionProtos.ProvisionRequest provisionRequest;

        try {
            provisionRequest = ProvisionProtos.ProvisionRequest.parseFrom(body);
        } catch (InvalidProtocolBufferException e) {
            LOGGER.error("Invalid protobuf: {}", e.getMessage());
            return plainTextError(Response.Status.BAD_REQUEST, e.getMessage());
        }

        final String deviceId = provisionRequest.getDeviceId();
        Optional<byte[]> optionalKeyBytes = keyStore.get(deviceId);

        if(!optionalKeyBytes.isPresent()) {
            optionalKeyBytes = Optional.of(KeyStoreDynamoDB.DEFAULT_AES_KEY);
        }

        final SignedMessage signedMessage = SignedMessage.parse(body);
        final Optional<SignedMessage.Error> optionalError = signedMessage.validateWithKey(optionalKeyBytes.get());

        if(optionalError.isPresent()) {
            return plainTextError(Response.Status.BAD_REQUEST, optionalError.get().message);
        }

        final String serialNumber = provisionRequest.getSerial();

        final byte[] key = new byte[16];
        new Random().nextBytes(key);
        keyStore.put(deviceId, Hex.encodeHexString(key).toUpperCase(), serialNumber);


        final ProvisionProtos.ProvisionResponse provisionResponse = ProvisionProtos.ProvisionResponse.newBuilder()
                .setKey(ByteString.copyFrom(key))
                .build();

        final Optional<byte[]> optionalSignedResponse = SignedMessage.sign(provisionResponse.toByteArray(), optionalKeyBytes.get());
        if(optionalSignedResponse.isPresent()) {
            return optionalSignedResponse.get();
        }

        return plainTextError(Response.Status.INTERNAL_SERVER_ERROR, "Failed to sign response");
    }


}
