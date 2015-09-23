package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.api.ble.SenseCommandProtos.MorpheusCommand;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.HelloHttpHeader;
import com.hello.suripu.service.registration.PillRegistration;
import com.hello.suripu.service.registration.SenseRegistration;
import com.yammer.metrics.annotation.Timed;
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


/**
 * Created by pangwu on 10/10/14.
 */
@Path("/register")
public class RegisterResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterResource.class);
    private final SenseRegistration senseRegistration;
    private final PillRegistration pillRegistration;
    private final Boolean debug;

    @Context
    HttpServletRequest request;


    public RegisterResource(final SenseRegistration senseRegistration,
                            final PillRegistration pillRegistration,
                            final Boolean debug){
        this.debug = debug;
        this.senseRegistration = senseRegistration;
        this.pillRegistration = pillRegistration;
    }

    @POST
    @Path("/morpheus")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Deprecated
    public byte[] registerMorpheus(final byte[] body)  {
        final String senseIdFromHeader = getSenseIdFromHeader();
        if(senseIdFromHeader != null){
            LOGGER.info("Sense Id from http header {}", senseIdFromHeader);
        }

        try {
            final Optional<MorpheusCommand> commandOptional = senseRegistration.attemptToPair(body);
            final MorpheusCommand command = commandOptional.get();
//            builder.clearAccountId();
            if(senseIdFromHeader != null && senseIdFromHeader.equals(KeyStoreDynamoDB.DEFAULT_FACTORY_DEVICE_ID)){
//                senseKeyStore.put(command.getDeviceId(), Hex.encodeHexString(KeyStoreDynamoDB.DEFAULT_AES_KEY));
                LOGGER.error("Key for device {} has been automatically generated", command.getDeviceId());
            }

            return senseRegistration.signAndSend(command.getDeviceId(), command);
        } catch (Exception e) {

        }
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    @POST
    @Path("/sense")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Timed
    public byte[] registerSense(final byte[] body) {
        final String senseIdFromHeader = getSenseIdFromHeader();
        if(senseIdFromHeader != null){
            LOGGER.info("Sense Id from http header {}", senseIdFromHeader);
        }

        try {
            final Optional<MorpheusCommand> commandOptional = senseRegistration.attemptToPair(body);
            if(!commandOptional.isPresent()) {
                BaseResource.throwPlainTextError(Response.Status.BAD_REQUEST, "");
            }
            if(senseIdFromHeader != null){
                return senseRegistration.signAndSend(senseIdFromHeader, commandOptional.get());
            }

            return senseRegistration.signAndSend(senseIdFromHeader, commandOptional.get());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
//        throwPlainTextError(Response.Status.BAD_REQUEST, "");
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    @POST
    @Path("/pill")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Timed
    public byte[] registerPill(final byte[] body) {
        final String senseIdFromHeader = getSenseIdFromHeader();

        try {
            final Optional<MorpheusCommand> commandOptional = pillRegistration.attemptToPair(body, senseIdFromHeader, request.getHeader("X-Forwarded-For"));
            if(!commandOptional.isPresent()) {
                BaseResource.throwPlainTextError(Response.Status.BAD_REQUEST, "");
            }

            final MorpheusCommand command = commandOptional.get();
            return pillRegistration.signAndSend(senseIdFromHeader, command);
        } catch (Exception e) {
          LOGGER.error("Failed pairing pill: {}", e.getMessage());
        }

        throw new WebApplicationException(Response.Status.BAD_REQUEST); // TODO : make this plainText
    }

    public String getSenseIdFromHeader() {
        return this.request.getHeader(HelloHttpHeader.SENSE_ID);
    }
}
