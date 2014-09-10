package com.hello.suripu.service.resources;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.audio.MatrixProtos;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.models.DeviceAccountPair;
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
import java.util.List;

@Path("/audio")
public class AudioResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioResource.class);

    private final AmazonS3Client s3Client;
    private final String audioBucketName;
    private final DataLogger dataLogger;
    private final DeviceDAO deviceDAO;

    public AudioResource(final AmazonS3Client s3Client, final String audioBucketName, final DataLogger dataLogger, final DeviceDAO deviceDAO) {
        this.s3Client = s3Client;
        this.audioBucketName = audioBucketName;
        this.dataLogger = dataLogger;
        this.deviceDAO = deviceDAO;
    }

    @POST
    @Path("/features")
    public void getAudioFeatures(@Context HttpServletRequest request, byte[] body) {
        try {

            final MatrixProtos.MatrixClientMessage message = MatrixProtos.MatrixClientMessage.parseFrom(body);

            LOGGER.debug("Received features from mac = {} at {}", Hex.encodeHex(message.getMac().toByteArray()), message.getUnixTime());
            LOGGER.debug("Source = {}", message.getMatrixPayload().getSource());
            LOGGER.debug("iData = {}", message.getMatrixPayload().getIdataList());

            final String deviceName = new String(Hex.encodeHex(message.getMac().toByteArray()));
            final List<DeviceAccountPair> pairs = deviceDAO.getAccountIdsForDeviceId(deviceName);

            LOGGER.debug("Found {} pairs for device name = {}", pairs.size(), deviceName);

            for(final DeviceAccountPair pair : pairs) {
                dataLogger.put(pair.internalDeviceId.toString(), body);
            }
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
