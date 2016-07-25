package com.hello.suripu.core.firmware;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class FirmwareS3Helper implements FirmwareHelper {

    private final static Map<HardwareVersion, String> s3Prefixes = ImmutableMap.of(
            HardwareVersion.SENSE_ONE, "sense",
            HardwareVersion.SENSE_ONE_FIVE, "sense1p5"
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(FirmwareS3Helper.class);
    private final AmazonS3 s3;
    private final AmazonS3 s3Signer;
    private final String bucketName;


    private FirmwareS3Helper(final AmazonS3 s3, final AmazonS3 s3Signer, final String bucketName) {
        this.s3 = s3;
        this.s3Signer = s3Signer;
        this.bucketName = bucketName;
    }


    // Static constructor methods

    public static FirmwareS3Helper create(final AmazonS3 s3, final AmazonS3 s3Signer, final String bucketName) {
        return new FirmwareS3Helper(s3, s3Signer, bucketName);
    }



    // Public methods

    @Override
    public List<S3ObjectSummary> summaries(final FirmwareCacheKey cacheKey, final SenseFirmwareUpdateQuery query) {

        if (!s3Prefixes.containsKey(cacheKey.hardwareVersion)) {
            LOGGER.warn("error=hw-version-not-configured hw_version={} sense_id={}", cacheKey.hardwareVersion, query.senseId);
           return new ArrayList<>();
        }

        return summaries(s3Prefixes.get(cacheKey.hardwareVersion), cacheKey.humanReadableGroupName);
    }

    @Override
    public String bucket() {
        return bucketName;
    }

    @Override
    public Optional<String> getTextForFirmwareVersion(final List<S3ObjectSummary> summaryList) {
        for(final S3ObjectSummary summary: summaryList) {
            if (!summary.getKey().contains("build_info.txt")) {
                continue;
            }

            final S3Object s3Object = s3.getObject(bucketName, summary.getKey());
            try {
                final String text = CharStreams.toString(new InputStreamReader(s3Object.getObjectContent(), Charsets.UTF_8));
                return Optional.of(text);
            } catch (IOException e) {
                LOGGER.error("error=failed-reading-build-info message={}", e.getMessage());
            } finally {
                try {
                    s3Object.close();
                } catch (IOException e) {
                    LOGGER.error("error=failed-closing-s3-stream");
                }
            }
        }
        return Optional.absent();
    }

    @Override
    public ImmutableMap<String, String> computeFilenameSHAMap(final List<S3ObjectSummary> summaryList) {
        final Map<String, String> filenameSHAMap = Maps.newHashMap(); //a map of filename->SHA1 value from S3 metaData
        for(final S3ObjectSummary summary: summaryList) {
            if (!summary.getKey().contains(".map") && !summary.getKey().contains(".out") && !summary.getKey().contains(".txt")) {
                LOGGER.trace("Adding file: {} to list of files to be prepared for update", summary.getKey());

                //Get SHA1 checksum from metadata Key 'x-amz-meta-sha'
                final ObjectMetadata metaData = s3.getObjectMetadata(bucketName, summary.getKey());
                final String metaDataChecksum = metaData.getUserMetaDataOf("sha");
                final String fileChecksum = (metaDataChecksum == null) ? "" : metaDataChecksum;

                filenameSHAMap.put(summary.getKey(), fileChecksum);
            }
        }

        return ImmutableMap.copyOf(filenameSHAMap);
    }


    @Override
    public byte[] computeSha1ForS3File(final String fileName) {
        final S3Object s3Object = s3.getObject(bucketName, fileName);
        final S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();

        try {
            return DigestUtils.sha1(s3ObjectInputStream);

        } catch (IOException e) {
            LOGGER.error("Failed computing sha1 for {}", fileName);
            throw new RuntimeException(e.getMessage());
        } finally {
            try {
                s3Object.close();
            } catch (IOException e) {
                LOGGER.error("Failed closing S3 stream");
            }
        }
    }

    @Override
    public URL signUrl(final String objectName, final int expiresInMinutes) {
        final GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, objectName);
        generatePresignedUrlRequest.setMethod(HttpMethod.GET); // Default.
        generatePresignedUrlRequest.setExpiration(expireInMinutes(expiresInMinutes));

        final URL signedURL = s3Signer.generatePresignedUrl(generatePresignedUrlRequest);
        LOGGER.debug("signed_url={}", signedURL);
        return signedURL;
    }


    // Private methods

    private List<S3ObjectSummary> summaries(final String prefix, final String key) {

        final ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(bucketName)
                .withPrefix(prefix + "/" + key + "/");

        final ObjectListing objectListing = s3.listObjects(listObjectsRequest);
        return objectListing.getObjectSummaries();
    }

    private static Date expireInMinutes(int minutes) {
        final Date expiration = new java.util.Date();
        long msec = expiration.getTime();
        msec += 1000 * 60 * minutes;
        expiration.setTime(msec);
        return expiration;
    }
}
