package com.hello.suripu.service.resources;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.Ordering;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.service.models.FirmwareUpdate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path(("/download"))
public class DownloadResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadResource.class);

    final private AmazonS3Client amazonS3Client;
    final private String bucketName;

    public DownloadResource(final AmazonS3Client amazonS3Client, final String bucketName) {
        this.amazonS3Client = amazonS3Client;
        this.bucketName = bucketName;

    }
    @Path("/{type}/manifest")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<FirmwareUpdate> getManifest(@Scope(OAuthScope.SENSORS_BASIC) AccessToken accessToken, @PathParam("type") String type) {

        final ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.withBucketName(bucketName);
        listObjectsRequest.withPrefix("stable");

        final ObjectListing objectListing = amazonS3Client.listObjects(listObjectsRequest);

        final Map<String, String> metadata = new HashMap<>();
        final List<String> files = new ArrayList<>();

        for(final S3ObjectSummary summary: objectListing.getObjectSummaries()) {
            if(summary.getKey().contains(".hex")) {
                files.add(summary.getKey());
                metadata.put(summary.getKey(), summary.getLastModified().toString());
            }
        }

        final Date expiration = DateTime.now().plusHours(1).toDate();

        final List<String> sorted = Ordering.natural().sortedCopy(files);
        final List<FirmwareUpdate> updates = new ArrayList<>();
        for(final String sortedKey : sorted) {
            final GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, sortedKey);
            generatePresignedUrlRequest.setMethod(HttpMethod.GET); // Default.
            generatePresignedUrlRequest.setExpiration(expiration);

            final URL s = amazonS3Client.generatePresignedUrl(generatePresignedUrlRequest);

            updates.add(new FirmwareUpdate(sortedKey, s.toExternalForm(), metadata.get(sortedKey)));
            LOGGER.debug("Generated url for key = {}", sortedKey);
        }
        return updates;
    }


    @Path("/latest/manifest")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<FirmwareUpdate> getManifestStable(@Scope(OAuthScope.SENSORS_BASIC) AccessToken accessToken) {

        final ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.withBucketName(bucketName);
        listObjectsRequest.withPrefix("latest");

        final ObjectListing objectListing = amazonS3Client.listObjects(listObjectsRequest);

        final Map<String, String> metadata = new HashMap<>();
        final List<String> files = new ArrayList<>();

        for(final S3ObjectSummary summary: objectListing.getObjectSummaries()) {
            if(summary.getKey().contains(".hex")) {
                files.add(summary.getKey());
                metadata.put(summary.getKey(), summary.getLastModified().toString());
            }
        }

        final Date expiration = DateTime.now().plusHours(1).toDate();

        final List<String> sorted = Ordering.natural().sortedCopy(files);
        final List<FirmwareUpdate> updates = new ArrayList<>();
        for(final String sortedKey : sorted) {
            final GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, sortedKey);
            generatePresignedUrlRequest.setMethod(HttpMethod.GET); // Default.
            generatePresignedUrlRequest.setExpiration(expiration);

            final URL s = amazonS3Client.generatePresignedUrl(generatePresignedUrlRequest);

            updates.add(new FirmwareUpdate(sortedKey, s.toExternalForm(), metadata.get(sortedKey)));
            LOGGER.debug("Generated url for key = {}", sortedKey);
        }
        return updates;
    }
}
