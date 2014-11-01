package com.hello.suripu.service.resources;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.base.Optional;
import com.hello.suripu.api.audio.MatrixProtos;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.util.DeviceIdUtil;
import com.hello.suripu.service.SignedMessage;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Path("/audio")
public class AudioResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioResource.class);

    private final AmazonS3Client s3Client;
    private final String audioBucketName;
    private final DataLogger dataLogger;
    private final boolean debug;

    public AudioResource(final AmazonS3Client s3Client, final String audioBucketName, final DataLogger dataLogger, final boolean debug) {
        this.s3Client = s3Client;
        this.audioBucketName = audioBucketName;
        this.dataLogger = dataLogger;
        this.debug = debug;
    }

    @POST
    @Path("/features")
    public void getAudioFeatures(@Context HttpServletRequest request, byte[] body) {

        final SignedMessage signedMessage = SignedMessage.parse(body);
        MatrixProtos.MatrixClientMessage message;

        try {
            message = MatrixProtos.MatrixClientMessage.parseFrom(signedMessage.body);
        } catch (IOException exception) {
            final String errorMessage = String.format("Failed parsing protobuf: %s", exception.getMessage());
            LOGGER.error(errorMessage);

            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity((debug) ? errorMessage : "bad request")
                    .type(MediaType.TEXT_PLAIN_TYPE).build()
            );
        }

        final Optional<String> mac = DeviceIdUtil.macToStringId(message.getMac().toByteArray());
        if(!mac.isPresent()) {
            LOGGER.warn("No mac address");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        // TODO: Fetch key from Datastore
        final byte[] keyBytes = "1234567891234567".getBytes();
        final Optional<SignedMessage.Error> error = signedMessage.validateWithKey(keyBytes);

        if(error.isPresent()) {
            LOGGER.error(error.get().message);
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity((debug) ? error.get().message : "bad request")
                    .type(MediaType.TEXT_PLAIN_TYPE).build()
            );
        }

        dataLogger.put(mac.get(), signedMessage.body);
    }


    @POST
    @Path("/{device_id}")
    public void getAudio(@Context HttpServletRequest request, byte[] body, @PathParam("device_id") String deviceId) {

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body);
        final String now = DateTime.now().toString();
        final String objectName = String.format("%s_%s", deviceId, now);
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(body.length);
        s3Client.putObject(audioBucketName, objectName, byteArrayInputStream, metadata);
        try {
            byteArrayInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
