package com.hello.suripu.service.resources;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.audio.MatrixProtos;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Path("/audio")
public class AudioResource {

    private final AmazonS3Client s3Client;
    private final String audioBucketName;

    public AudioResource(final AmazonS3Client s3Client, final String audioBucketName) {
        this.s3Client = s3Client;
        this.audioBucketName = audioBucketName;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioResource.class);


    @POST
    @Path("/features")
    public void getAudioFeatures(@Context HttpServletRequest request, byte[] body) {
        try {
            final MatrixProtos.MatrixClientMessage message = MatrixProtos.MatrixClientMessage.parseFrom(body);
            LOGGER.debug("Received features from mac = {} at {}", Hex.encodeHex(message.getMac().toByteArray()), message.getUnixTime());
            LOGGER.debug("Source = {}", message.getMatrixPayload().getSource());
            LOGGER.debug("iData = {}", message.getMatrixPayload().getIdataList());
        } catch (InvalidProtocolBufferException e) {
            LOGGER.error("Failed parsing protobuf: {}", e.getMessage());
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("invalid protobuf").build());
        }
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
