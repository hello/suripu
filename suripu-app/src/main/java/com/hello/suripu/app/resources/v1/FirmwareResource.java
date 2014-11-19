package com.hello.suripu.app.resources.v1;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.hello.suripu.core.firmware.FirmwareFile;
import com.hello.suripu.core.firmware.FirmwareUpdateStore;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Deprecated
@Path("/v1/firmware")
public class FirmwareResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirmwareResource.class);

    private final FirmwareUpdateStore firmwareUpdateStore;
    private final AmazonS3 amazonS3;
    private final String bucketName;

    public FirmwareResource(final FirmwareUpdateStore firmwareUpdateStore, final String bucketName,
                            final AmazonS3 amazonS3) {
        this.firmwareUpdateStore = firmwareUpdateStore;
        this.bucketName = bucketName;
        this.amazonS3 = amazonS3;
    }

    @POST
    @Path("/{device_id}/{firmware_version}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addFirmwareFile(
            @Scope(OAuthScope.ADMINISTRATION_WRITE) AccessToken accessToken,
            @Valid final FirmwareFile firmwareFile,
            @PathParam("device_id") final String deviceId,
            @PathParam("firmware_version") final Integer firmwareVersion) {

        final FirmwareFile updated = FirmwareFile.withS3Info(firmwareFile, "hello-firmware", "sense" + firmwareFile.s3Key);
        firmwareUpdateStore.insertFile(updated, deviceId, firmwareVersion);
    }

    @DELETE
    @Path("/{device_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void reset( @Scope(OAuthScope.ADMINISTRATION_WRITE) AccessToken accessToken, @PathParam("device_id") final String deviceId) {

        firmwareUpdateStore.reset(deviceId);
    }

    @PUT
    @Path("/{device_id}/{firmware_version}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addFirmwareFiles(
            @Scope(OAuthScope.ADMINISTRATION_WRITE) AccessToken accessToken,
            @PathParam("device_id") final String deviceId,
            @PathParam("firmware_version") final Integer firmwareVersion,
            final List<FirmwareFile> files) {

        for(final FirmwareFile firmwareFile : files) {

            if(firmwareFile.resetApplicationProcessor) {
                final S3Object object = amazonS3.getObject(bucketName, firmwareFile.s3Key);
                final S3ObjectInputStream s3ObjectInputStream = object.getObjectContent();
                try {
                    final String sha1 = DigestUtils.sha1Hex(s3ObjectInputStream);
                    final FirmwareFile updated = FirmwareFile.withS3InfoAndSha1(firmwareFile, bucketName, firmwareFile.s3Key, sha1);
                    firmwareUpdateStore.insertFile(updated, deviceId, firmwareVersion);
                    continue;
                } catch (IOException e) {
                    LOGGER.error("Failed computing sha1 for {}", firmwareFile.s3Key);
                    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
                }
            }

            final FirmwareFile updated = FirmwareFile.withS3Info(firmwareFile, bucketName, firmwareFile.s3Key);
            firmwareUpdateStore.insertFile(updated, deviceId, firmwareVersion);

        }
    }


    @GET
    @Path("/{device_id}/{firmware_version}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FirmwareFile> getFirmwareFiles(
            @Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
            @PathParam("device_id") final String deviceId,
            @PathParam("firmware_version") final Integer firmwareVersion) {

        return firmwareUpdateStore.getFirmwareFiles(deviceId,firmwareVersion);
    }


    @GET
    @Path("source/{s3_key}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getKeysFromBucket(
            @Scope(OAuthScope.ADMINISTRATION_READ) AccessToken accessToken,
            @PathParam("s3_key") final String s3key) {


        final ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.withBucketName(bucketName); // TODO: Move this to Firmware store
        listObjectsRequest.withPrefix("sense/" + s3key);

        final ObjectListing objectListing = amazonS3.listObjects(listObjectsRequest);
        final List<String> files = new ArrayList<>();

        for(final S3ObjectSummary summary: objectListing.getObjectSummaries()) {
            files.add(summary.getKey());
        }

        return files;
    }
}
