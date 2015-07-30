package com.hello.suripu.service.resources;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.service.models.FirmwareUpdate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.Date;
import java.util.List;

@Path(("/download"))
public class DownloadResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadResource.class);

    final private AmazonS3Client amazonS3Client;
    final private String bucketName;

    public DownloadResource(final AmazonS3Client amazonS3Client, final String bucketName) {
        this.amazonS3Client = amazonS3Client;
        this.bucketName = bucketName;
    }


    private static boolean isValidType(final String type) {
        return (type.equals("pill") || type.equals("morpheus"));
    }

    private List<FirmwareUpdate> createFirmwareUpdatesFromListing(final FluentIterable<S3ObjectSummary> listingStream) {
        final Date expiration = DateTime.now().plusHours(1).toDate();
        return listingStream
                .filter(new Predicate<S3ObjectSummary>() {
                    @Override
                    public boolean apply(final S3ObjectSummary summary) {
                        final String key = summary.getKey();
                        return (key.endsWith(".hex") || key.endsWith(".bin") || key.endsWith(".zip"));
                    }
                })
                .transform(new Function<S3ObjectSummary, FirmwareUpdate>() {
                    @Nullable
                    @Override
                    public FirmwareUpdate apply(final S3ObjectSummary objectSummary) {
                        final String key = objectSummary.getKey();
                        final GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, key);
                        generatePresignedUrlRequest.setMethod(HttpMethod.GET); // Default.
                        generatePresignedUrlRequest.setExpiration(expiration);

                        final URL s = amazonS3Client.generatePresignedUrl(generatePresignedUrlRequest);

                        LOGGER.debug("Generated url for key = {}", key);
                        return new FirmwareUpdate(key, s.toExternalForm(), objectSummary.getLastModified().toString());
                    }
                })
                .toSortedList(FirmwareUpdate.createOrdering());
    }


    @Path("/{type}/manifest")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<FirmwareUpdate> getManifestStable(final @Scope(OAuthScope.SENSORS_BASIC) AccessToken accessToken,
                                                  final @PathParam("type") String type) {
        if (!isValidType(type)) {
            LOGGER.warn("Unrecognized type '{}' given", type);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.withBucketName(bucketName);
        listObjectsRequest.withPrefix("stable");

        final ObjectListing objectListing = amazonS3Client.listObjects(listObjectsRequest);
        final FluentIterable<S3ObjectSummary> listingStream = FluentIterable.from(objectListing.getObjectSummaries())
                .filter(new Predicate<S3ObjectSummary>() {
                    @Override
                    public boolean apply(final S3ObjectSummary objectSummary) {
                        // Key is of form stable/{type}+{type}_{tag}.(hex|bin|zip)
                        return objectSummary.getKey().contains("/" + type + "+");
                    }
                });
        return createFirmwareUpdatesFromListing(listingStream);
    }


    @Path("/latest/manifest")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<FirmwareUpdate> getManifestLatest(@Scope(OAuthScope.SENSORS_BASIC) AccessToken accessToken) {
        final ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.withBucketName(bucketName);
        listObjectsRequest.withPrefix("latest");

        final ObjectListing objectListing = amazonS3Client.listObjects(listObjectsRequest);
        final FluentIterable<S3ObjectSummary> listingStream = FluentIterable.from(objectListing.getObjectSummaries());
        return createFirmwareUpdatesFromListing(listingStream);
    }
}
