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

    private final String SN_PREFIX = "91000008";

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

        final SignedMessage signedMessage = SignedMessage.parse(body);

        ProvisionProtos.ProvisionRequest provisionRequest;

        try {
            provisionRequest = ProvisionProtos.ProvisionRequest.parseFrom(signedMessage.body);
        } catch (InvalidProtocolBufferException e) {
            LOGGER.error("Invalid protobuf: {}", e.getMessage());
            return plainTextError(Response.Status.BAD_REQUEST, e.getMessage());
        }

        final String deviceId = provisionRequest.getDeviceId();
        final String ipAddress = getIpAddress(request);
        final Boolean isPCH = OTAProcessor.isPCH(ipAddress);
        final Boolean isHelloOffice = OTAProcessor.isHelloOffice(ipAddress);


        final Optional<byte[]> optionalKeyBytes = keyStore.get(deviceId);

        if(!optionalKeyBytes.isPresent()) {
            LOGGER.warn("No key found for device_id = {} from ip={}", deviceId, ipAddress);
            return plainTextError(Response.Status.NOT_FOUND, "");

        }

        final byte[] key = optionalKeyBytes.get();

        if(isPCH || isHelloOffice || KeyStoreDynamoDB.DEFAULT_AES_KEY.equals(key)) {
            LOGGER.warn("Attempting to get keys from ip={} for device_id={}", ipAddress, deviceId);
            final Optional<SignedMessage.Error> optionalError = signedMessage.validateWithKey(key);



            // TODO: remove me
            LOGGER.info("Key = {}", Hex.encodeHexString(key).toUpperCase());
            if(optionalError.isPresent()) {
                LOGGER.error("Failed to validate signature for device_id = {} and key = {}", deviceId, Hex.encodeHexString(key));
                return plainTextError(Response.Status.BAD_REQUEST, optionalError.get().message);
            }

            final String serialNumber = String.format("%s%s", SN_PREFIX, provisionRequest.getSerial());

            final byte[] newKey = new byte[16];
            new Random().nextBytes(newKey );


            final ProvisionProtos.ProvisionResponse provisionResponse = ProvisionProtos.ProvisionResponse.newBuilder()
                    .setKey(ByteString.copyFrom(newKey))
                    .build();

            final Optional<byte[]> optionalSignedResponse = SignedMessage.sign(provisionResponse.toByteArray(), optionalKeyBytes.get());
            if(optionalSignedResponse.isPresent()) {
                if(!KeyStoreDynamoDB.DEFAULT_AES_KEY.equals(key)) {
                    LOGGER.warn("Overriding existing non-default key for device_id = {} from ip = {}", deviceId, ipAddress);
                }
                keyStore.put(deviceId, Hex.encodeHexString(newKey).toUpperCase(), serialNumber);
                LOGGER.info("Persisted new key for device_id = {} with new key={}", deviceId, Hex.encodeHexString(newKey).toUpperCase().substring(0,6));
                return optionalSignedResponse.get();
            }
        }

        LOGGER.error("Got request from ip={} with a valid protobuf but not matching PCH/Hello IP address or not having default key in store", ipAddress);
        return plainTextError(Response.Status.NOT_FOUND, "");
    }
}
