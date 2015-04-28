package com.hello.suripu.core.firmware;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Ordering;
import com.google.common.io.CharStreams;
import com.google.protobuf.ByteString;
import com.hello.suripu.api.output.OutputProtos;
import com.hello.suripu.api.output.OutputProtos.SyncResponse;

import com.hello.suripu.core.db.OTAHistoryDAODynamoDB;
import com.hello.suripu.core.models.OTAHistory;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.math3.util.Pair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FirmwareUpdateStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirmwareUpdateStore.class);

    private final OTAHistoryDAODynamoDB otaHistoryDAO;
    private final AmazonS3 s3;
    private final String bucketName;
    private final AmazonS3 s3Signer;
    private final static String FIRMWARE_BUCKET_ASIA = "hello-firmware-asia";

    final Cache<String, Pair<Integer, List<SyncResponse.FileDownload>>> s3FWCache;

    public FirmwareUpdateStore(final OTAHistoryDAODynamoDB otaHistoryDAO,
                               final AmazonS3 s3, 
                               final String bucketName, 
                               final AmazonS3 s3Signer,
                               final Cache<String, Pair<Integer, List<SyncResponse.FileDownload>>> s3FWCache) {
        this.otaHistoryDAO = otaHistoryDAO;
        this.s3 = s3;
        this.bucketName = bucketName;
        this.s3Signer = s3Signer;
        this.s3FWCache = s3FWCache;
    }

    public static FirmwareUpdateStore create(final OTAHistoryDAODynamoDB otaHistoryDAO,
                                             final AmazonS3 s3,
                                             final String bucketName,
                                             final AmazonS3 s3Signer,
                                             final Cache<String, Pair<Integer, List<SyncResponse.FileDownload>>> s3Cache) {
        return new FirmwareUpdateStore(otaHistoryDAO, s3, bucketName, s3Signer, s3Cache);
    }

    public static FirmwareUpdateStore create(final OTAHistoryDAODynamoDB otaHistoryDAO,
                                             final AmazonS3 s3,
                                             final String bucketName,
                                             final AmazonS3 s3Signer,
                                             final Integer s3CacheExpireMinutes) {
        final Cache<String, Pair<Integer, List<OutputProtos.SyncResponse.FileDownload>>> s3Cache = CacheBuilder.newBuilder()
                .expireAfterWrite(s3CacheExpireMinutes, TimeUnit.MINUTES)
                .build();
        return new FirmwareUpdateStore(otaHistoryDAO, s3, bucketName, s3Signer, s3Cache);
    }

    public Pair<Integer, List<SyncResponse.FileDownload>> getFirmwareFilesForGroup(final String group, final String bucketName) {

        final Pair<Integer, List<SyncResponse.FileDownload>> emptyPair = new Pair(-1, Collections.EMPTY_LIST);

        final ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.withBucketName(bucketName);
        listObjectsRequest.withPrefix("sense/" + group + "/");

        final ObjectListing objectListing = s3.listObjects(listObjectsRequest);
        final List<String> files = new ArrayList<>();
        Integer firmwareVersion = 0;
        for(final S3ObjectSummary summary: objectListing.getObjectSummaries()) {
            if(!summary.getKey().contains(".map") && !summary.getKey().contains(".out") && !summary.getKey().contains(".txt")) {
                LOGGER.trace("Adding file: {} to list of files to be prepared for update", summary.getKey());
                files.add(summary.getKey());
            }

            if(summary.getKey().contains("build_info.txt")) {
                final S3Object s3Object = s3.getObject(bucketName, summary.getKey());
                final S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();
                String text;
                try {
                    text = CharStreams.toString(new InputStreamReader(s3ObjectInputStream, Charsets.UTF_8));
                } catch (IOException e) {
                    LOGGER.error("Failed reading build_info from s3: {}", e.getMessage());
                    return emptyPair;
                }

                final Iterable<String> strings = Splitter.on("\n").split(text);
                final String firstLine = strings.iterator().next();
                String[] parts = firstLine.split(":");
                try {
                    firmwareVersion = Integer.parseInt(parts[1].trim(), 16);
                } catch (NumberFormatException nfe) {
                    LOGGER.error("Firmware version in {} is not a valid firmware version. Ignoring this update", group);
                    return emptyPair;
                }
            }
        }

        final List<SyncResponse.FileDownload> fileDownloadList = new ArrayList<>();

        final Date expiration = new java.util.Date();
        long msec = expiration.getTime();
        msec += 1000 * 60 * 60; // 1 hour.
        expiration.setTime(msec);

        for(final String f : files) {

            final GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, f);
            generatePresignedUrlRequest.setMethod(HttpMethod.GET); // Default.
            generatePresignedUrlRequest.setExpiration(expiration);

            final URL s = s3Signer.generatePresignedUrl(generatePresignedUrlRequest);
            LOGGER.debug("S3 URL: {}", s);

            final SyncResponse.FileDownload.Builder fileDownloadBuilder = SyncResponse.FileDownload.newBuilder()
                    .setUrl(s.getPath() + "?" + s.getQuery())
                    .setHost(s.getHost()); // TODO: replace with hello s3 proxy

            if(f.contains("kitsune.bin")) {

                final byte[] sha1 = computeSha1ForS3File(bucketName, f);
                fileDownloadBuilder.setSha1(ByteString.copyFrom(sha1));

                final boolean copyToSerialFlash = true;
                final boolean resetApplicationProcessor = true;
                final String serialFlashFilename = "mcuimgx.bin";
                final String serialFlashPath = "/sys/";
                final String sdCardFilename = "mcuimgx.bin";
                final String sdCardPath = "/";

                fileDownloadBuilder.setCopyToSerialFlash(copyToSerialFlash);
                fileDownloadBuilder.setResetApplicationProcessor(resetApplicationProcessor);
                fileDownloadBuilder.setSerialFlashFilename(serialFlashFilename);
                fileDownloadBuilder.setSerialFlashPath(serialFlashPath);
                fileDownloadBuilder.setSdCardFilename(sdCardFilename);
                fileDownloadBuilder.setSdCardPath(sdCardPath);


                fileDownloadList.add(fileDownloadBuilder.build());
            }


            if(f.contains("top.bin")) {

                final byte[] sha1 = computeSha1ForS3File(bucketName, f);
                fileDownloadBuilder.setSha1(ByteString.copyFrom(sha1));



                final boolean copyToSerialFlash = true;
                final boolean resetApplicationProcessor = false;
                final String serialFlashFilename = "update.bin";
                final String serialFlashPath = "/top/";
                final String sdCardFilename = "top.update.bin";
                final String sdCardPath = "/";

                fileDownloadBuilder.setCopyToSerialFlash(copyToSerialFlash);
                fileDownloadBuilder.setResetApplicationProcessor(resetApplicationProcessor);
                fileDownloadBuilder.setSerialFlashFilename(serialFlashFilename);
                fileDownloadBuilder.setSerialFlashPath(serialFlashPath);
                fileDownloadBuilder.setSdCardFilename(sdCardFilename);
                fileDownloadBuilder.setSdCardPath(sdCardPath);


                fileDownloadList.add(fileDownloadBuilder.build());
            }
        }

        final Ordering<SyncResponse.FileDownload> byResetApplicationProcessor = new Ordering<SyncResponse.FileDownload>() {
            public int compare(SyncResponse.FileDownload left, SyncResponse.FileDownload right) {
                return Boolean.compare(left.getResetApplicationProcessor(), right.getResetApplicationProcessor());
            }
        };

        final List<SyncResponse.FileDownload> sortedFiles = byResetApplicationProcessor.sortedCopy(fileDownloadList);

        return new Pair<>(firmwareVersion, sortedFiles);
    }
    /**
     * Attempts retrieval of file list for group from S3 cache and compares fw version number to see if update is needed
     * @param group
     * @return
     */
    public List<SyncResponse.FileDownload> getFirmwareUpdate(final String deviceId, final String group, final Integer currentFirmwareVersion, final Boolean pchOTA) {

        Pair<Integer, List<SyncResponse.FileDownload>> fw_files = new Pair(-1, Collections.EMPTY_LIST);

        if(pchOTA) {
            LOGGER.info("PCH Device attempting OTA. Getting non-cached file-list for: [{}] from {}.", group, FIRMWARE_BUCKET_ASIA);
            fw_files = getFirmwareFilesForGroup(group, FIRMWARE_BUCKET_ASIA);
        } else {
            try {
                fw_files = s3FWCache.get(group, new Callable<Pair<Integer, List<SyncResponse.FileDownload>>>() {
                    @Override
                    public Pair<Integer, List<SyncResponse.FileDownload>> call() throws Exception {
                        LOGGER.info("Nothing in cache found for group: [{}]. Grabbing info from S3.", group);
                        return getFirmwareFilesForGroup(group, bucketName);
                    }
                });

            } catch (ExecutionException e) {
                LOGGER.error("Exception while retrieving S3 file list.");
            }
        }


        final List<SyncResponse.FileDownload> fwList = fw_files.getValue();

        if (isValidFirmwareUpdate(fw_files, currentFirmwareVersion) && !fwList.isEmpty()) {

            if (!isExpiredPresignedUrl(fwList.get(0).getUrl(), new Date())) {
                //Store OTA Data
                final List<String> urlList = new ArrayList<>();
                for (final SyncResponse.FileDownload fileDL : fwList) {
                    urlList.add(fileDL.getHost() + fileDL.getUrl());
                }
                final DateTime eventTime = new DateTime().toDateTime(DateTimeZone.UTC);
                final OTAHistory newHistoryEntry = new OTAHistory(deviceId, eventTime, currentFirmwareVersion, fw_files.getKey(), urlList);
                final Optional<OTAHistory> insertedEntry = otaHistoryDAO.insertOTAEvent(newHistoryEntry);
                if (!insertedEntry.isPresent()) {
                    LOGGER.error("OTA History Insertion Failed: {} => {} for {} at {}", currentFirmwareVersion, fw_files.getKey(), deviceId, eventTime);
                }
                return fwList;
            }

            //Cache returned a valid update with an expired URL
            LOGGER.info("Expired URL in S3 Cache. Forcing Cleanup.");
            s3FWCache.cleanUp();
        }
        return Collections.EMPTY_LIST;
    }

    private byte[] computeSha1ForS3File(final String bucketName, final String fileName) {
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

    public static boolean isValidFirmwareUpdate(final Pair<Integer, List<SyncResponse.FileDownload>> fw_files, final Integer currentFirmwareVersion) {

        final Integer firmwareVersion = fw_files.getKey();

        if (firmwareVersion == -1) {
            LOGGER.error("Failed to retrieve S3 file list.");
            return false;
        }

        if (firmwareVersion.equals(currentFirmwareVersion)) {
            LOGGER.info("Versions match: {}, current version = {}", firmwareVersion, currentFirmwareVersion);
            return false;
        }

        return true;
    }

    public static boolean isExpiredPresignedUrl(final String presignedUrl, final Date now) {
        final Map<String, String> urlMap = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(presignedUrl.split("\\?")[1]);

        if (now == null) {
            LOGGER.error("Invalid date parameter in pre-signed URL check.");
            return true;
        }

        try {
            final Long expiration = Long.parseLong(urlMap.get("Expires"));
            final Long nowSecs = now.getTime() / 1000L;
            return (nowSecs > expiration);
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid pre-signed URL.");
            return true;
        }
    }
}