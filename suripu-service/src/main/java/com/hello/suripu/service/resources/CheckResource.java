package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.HelloHttpHeader;
import com.hello.suripu.service.SignedMessage;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/check")
public class CheckResource extends BaseResource {

    private final static Logger LOGGER = LoggerFactory.getLogger(CheckResource.class);

    private final KeyStore senseKeyStore;

    @Context
    HttpServletRequest request;

    public CheckResource(final KeyStore senseKeyStore) {
        this.senseKeyStore = senseKeyStore;
    }

    @POST
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] check(final byte[] body) {
        final String senseId = request.getHeader(HelloHttpHeader.SENSE_ID);
        final String ipAddress = (request.getHeader("X-Forwarded-For") == null) ? request.getRemoteAddr() : request.getHeader("X-Forwarded-For");
        if (senseId == null) {
            LOGGER.error("CHECK_KEY FAILED. Request doesn't contain the required header {}. IP: {}", HelloHttpHeader.SENSE_ID, ipAddress);
            return plainTextError(Response.Status.BAD_REQUEST, "http 400");
        }

        if (senseId.equals(KeyStoreDynamoDB.DEFAULT_FACTORY_DEVICE_ID)) {
            LOGGER.warn("CHECK_KEY FAILED. DeviceId = {}. Failing check key. IP: {}", senseId, ipAddress);
            return plainTextError(Response.Status.BAD_REQUEST, "http 400");
        }


        Optional<byte[]> keyBytes;
        try {
            keyBytes = senseKeyStore.getStrict(senseId);
        } catch (Exception e) {
            LOGGER.error("CHECK_KEY FAILED. Failed to connect to senseKeyStore: {}. IP: {}", e.getMessage(), ipAddress);
            return plainTextError(Response.Status.INTERNAL_SERVER_ERROR, "http 500");
        }

        if (!keyBytes.isPresent()) {
            LOGGER.warn("CHECK_KEY FAILED. Could not find keys for senseId = {}. IP: {}", senseId, ipAddress);
            // DO not leak that the device id exists or not. return forbidden
            return plainTextError(Response.Status.FORBIDDEN, "");
        }


        SignedMessage signedMessage;
        try {
            signedMessage = SignedMessage.parse(body);
        } catch (RuntimeException e) {
            LOGGER.error("CHECK_KEY FAILED. Failed parsing request body for sense {}. IP: {}", senseId, ipAddress);
            return plainTextError(Response.Status.BAD_REQUEST, "");
        }

        final Optional<SignedMessage.Error> error = signedMessage.validateWithKey(keyBytes.get());
        if (error.isPresent()) {
            LOGGER.warn("CHECK_KEY FAILED. Could not verify signature for senseId = {} with key = {}. IP: {}", senseId, Hex.encodeHexString(keyBytes.get()), ipAddress);
            return plainTextError(Response.Status.FORBIDDEN, "");
        }

        final Optional<byte[]> signedResponse = SignedMessage.sign(signedMessage.body, keyBytes.get());
        if (!signedResponse.isPresent()) {
            LOGGER.warn("CHECK_KEY FAILED. Could not sign response for senseId = {} with key = {}. IP: {}", senseId, Hex.encodeHexString(keyBytes.get()), ipAddress);
            return plainTextError(Response.Status.INTERNAL_SERVER_ERROR, "");
        }

        LOGGER.info("CHECK_KEY SUCCESS for senseId = {}. IP: {}", senseId, ipAddress);
        return signedResponse.get();
    }
}
