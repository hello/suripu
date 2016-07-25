package com.hello.suripu.core.firmware;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import java.net.URL;
import java.util.List;


public interface FirmwareHelper {

    /**
     *
     * @return bucket configured
     */
    String bucket();


    /**
     * Lists all files inside a bucket for sense
     * @param cacheKey
     * @return
     */
    List<S3ObjectSummary> summaries(final FirmwareCacheKey cacheKey, final SenseFirmwareUpdateQuery query);

    /**
     * Parse text from file and return text to be parsed as fw version
     * @param summaryList
     * @return
     */
    Optional<String> getTextForFirmwareVersion(final List<S3ObjectSummary> summaryList);


    /**
     * Each filename is paired with a sha1 hash
     * @param summaryList
     * @return
     */
    ImmutableMap<String, String> computeFilenameSHAMap(final List<S3ObjectSummary> summaryList);


    /**
     * Computes Sha1 of object identified as filename
     * @param filename
     * @return
     */
    byte[] computeSha1ForS3File(final String filename);


    /**
     * Sign a url with expiration time.
     * @param objectName
     * @param expiresInMinutes
     * @return
     */
    URL signUrl(final String objectName, final int expiresInMinutes);
}
