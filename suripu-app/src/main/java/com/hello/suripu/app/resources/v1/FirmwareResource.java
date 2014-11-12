package com.hello.suripu.app.resources.v1;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.core.firmware.FirmwareFile;
import com.hello.suripu.core.firmware.FirmwareUpdateStore;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Deprecated
@Path("/v1/firmware")
public class FirmwareResource {

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
            final FirmwareFile updated = FirmwareFile.withS3Info(firmwareFile, "hello-firmware", "sense/" + firmwareFile.s3Key);
            firmwareUpdateStore.insertFile(updated, deviceId, firmwareVersion);
        }
    }


    @GET
    @Path("/{device_id}/{firmware_version}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FirmwareFile> getFirmwareFiles(
            @Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
            @PathParam("device_id") final String deviceId,
            @PathParam("firmare_version") final Integer firmwareVersion) {

        return firmwareUpdateStore.getFirmwareFiles(deviceId,firmwareVersion);
    }


    @GET
    @Path("source/{s3_key}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getKeysFromBucket(
            @Scope(OAuthScope.ADMINISTRATION_READ) AccessToken accessToken,
            @PathParam("s3_key") final String s3key) {


        final ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.withBucketName(bucketName);
        listObjectsRequest.withPrefix("sense/" + s3key);

        final ObjectListing objectListing = amazonS3.listObjects(listObjectsRequest);
        final List<String> files = new ArrayList<>();

        for(final S3ObjectSummary summary: objectListing.getObjectSummaries()) {
            files.add(summary.getKey());
        }

        return files;
    }
}
