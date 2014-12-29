package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.core.db.KeyStore;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/check")
public class CheckResource {

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
        if (senseId == null) {
            LOGGER.error("Request doesn't contain the required header {}", HelloHttpHeader.SENSE_ID);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("incorrect headers").type(MediaType.TEXT_PLAIN_TYPE).build());
        }

        final Optional<byte[]> keyBytes = senseKeyStore.get(senseId);
        if(!keyBytes.isPresent()) {
            LOGGER.warn("Could not find keys for senseId = {}", senseId);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("not found").type(MediaType.TEXT_PLAIN_TYPE).build());
        }

        final SignedMessage signedMessage = SignedMessage.parse(body);
        final Optional<SignedMessage.Error> error = signedMessage.validateWithKey(keyBytes.get());
        if (error.isPresent()) {
            LOGGER.warn("Could not verify signature for senseId = {} with key = {}", senseId, Hex.encodeHexString(keyBytes.get()));
            throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).entity("").type(MediaType.TEXT_PLAIN_TYPE).build());
        }

        final Optional<byte[]> signedResponse = SignedMessage.sign(signedMessage.body, keyBytes.get());
        if(!signedResponse.isPresent()) {
            LOGGER.warn("Could not sign response for senseId = {} with key = {}", senseId, Hex.encodeHexString(keyBytes.get()));
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("server error").type(MediaType.TEXT_PLAIN_TYPE).build());
        }

        return signedResponse.get();
    }
}
