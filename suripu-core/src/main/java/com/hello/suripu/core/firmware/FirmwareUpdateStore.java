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
import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.collect.Ordering;
import com.google.common.io.CharStreams;
import com.google.protobuf.ByteString;
import com.hello.suripu.api.output.OutputProtos.SyncResponse;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class FirmwareUpdateStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirmwareUpdateStore.class);

    private final FirmwareUpdateDAO firmwareUpdateDAO;
    private final AmazonS3 s3;
    private final String bucketName;
    private final AmazonS3 s3Signer;

    final Cache<String, Map<Integer, List<SyncResponse.FileDownload>>> s3FWCache;

    public FirmwareUpdateStore(final FirmwareUpdateDAO firmwareUpdateDAOImpl, 
                               final AmazonS3 s3, 
                               final String bucketName, 
                               final AmazonS3 s3Signer,
                               final Cache<String, Map<Integer, List<SyncResponse.FileDownload>>> s3FWCache) {
        this.firmwareUpdateDAO = firmwareUpdateDAOImpl;
        this.s3 = s3;
        this.bucketName = bucketName;
        this.s3Signer = s3Signer;
        this.s3FWCache = s3FWCache;
    }

    public static FirmwareUpdateStore create(final FirmwareUpdateDAO firmwareUpdateDAOImpl,
                                             final AmazonS3 s3,
                                             final String bucketName,
                                             final AmazonS3 s3Signer,
                                             final Cache<String, Map<Integer, List<SyncResponse.FileDownload>>> s3Cache) {
        return new FirmwareUpdateStore(firmwareUpdateDAOImpl, s3, bucketName, s3Signer, s3Cache);
    }

    public void insertFile(final FirmwareFile firmwareFile, final String deviceId, final Integer firmwareVersion) {
        firmwareUpdateDAO.insert(firmwareFile, deviceId, firmwareVersion);
    }

    public void reset(final String deviceId) {
        firmwareUpdateDAO.reset(deviceId);
    }


    public List<FirmwareFile> getFirmwareFiles(final String deviceId, final Integer firmwareVersion) {
        return firmwareUpdateDAO.getFiles(deviceId, firmwareVersion);
    }

    public List<SyncResponse.FileDownload> getFirmwareUpdateContent(final String deviceId, final Integer currentFirmwareVersion) {

        final List<FirmwareFile> files = firmwareUpdateDAO.getFiles(deviceId, currentFirmwareVersion);

        LOGGER.debug("Found {} files to update", files.size());
        final List<SyncResponse.FileDownload> fileDownloadList = new ArrayList<>();

        final Date expiration = new java.util.Date();
        long msec = expiration.getTime();
        msec += 1000 * 60 * 60; // 1 hour.
        expiration.setTime(msec);

        for(final FirmwareFile f : files) {

            final GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(f.s3Bucket, f.s3Key);
            generatePresignedUrlRequest.setMethod(HttpMethod.GET); // Default.
            generatePresignedUrlRequest.setExpiration(expiration);

            final URL s = s3Signer.generatePresignedUrl(generatePresignedUrlRequest);
            LOGGER.debug("{}", s);

            final SyncResponse.FileDownload.Builder fileDownloadBuilder = SyncResponse.FileDownload.newBuilder()
                    .setUrl(s.getPath() + "?" + s.getQuery())
                    .setHost(s.getHost())
                    .setCopyToSerialFlash(f.copyToSerialFlash)
                    .setResetApplicationProcessor(f.resetApplicationProcessor)
                    .setResetNetworkProcessor(f.resetNetworkProcessor)
                    .setSerialFlashFilename(f.serialFlashFilename)
                    .setSerialFlashPath(f.serialFlashPath)
                    .setSdCardFilename(f.sdCardFilename)
                    .setSdCardPath(f.sdCardPath);

            if(!f.sha1.isEmpty()) {
                try {
                    fileDownloadBuilder.setSha1(ByteString.copyFrom(Hex.decodeHex(f.sha1.toCharArray())));
                } catch (DecoderException e) {
                    LOGGER.error("Failed decoding sha1 from hex");
                }
            }

            fileDownloadList.add(fileDownloadBuilder.build());
        }

        return fileDownloadList;
    }

    public Map<Integer, List<SyncResponse.FileDownload>> getFirmwareFilesForGroup(final String group) {
        
        final Map<Integer, List<SyncResponse.FileDownload>> firmwareFileList = new HashMap<>();
        final ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.withBucketName(bucketName);
        listObjectsRequest.withPrefix("sense/" + group);

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
                String text ;
                try {
                    text = CharStreams.toString(new InputStreamReader(s3ObjectInputStream, Charsets.UTF_8));
                } catch (IOException e) {
                    LOGGER.error("Failed reading build_info from s3: {}", e.getMessage());
                    return Collections.EMPTY_MAP;
                }

                final Iterable<String> strings = Splitter.on("\n").split(text);
                final String firstLine = strings.iterator().next();
                String[] parts = firstLine.split(":");
                try {
                    firmwareVersion = Integer.parseInt(parts[1].trim(), 16);
                } catch (NumberFormatException nfe) {
                    LOGGER.error("Firmware version in {} is not a valid firmware version. Ignoring this update", group);
                    return Collections.EMPTY_MAP;
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
        firmwareFileList.put(firmwareVersion, sortedFiles);
        
        return firmwareFileList;
    }
    /**
     * Attempts retrieval of file list for group from S3 cache and compares fw version number to see if update is needed
     * @param group
     * @return
     */
    public List<SyncResponse.FileDownload> getFirmwareUpdate(final String deviceId, final String group, final int currentFirmwareVersion) {

        Map<Integer, List<SyncResponse.FileDownload>> fw_files = Collections.EMPTY_MAP;
        
        try {
            fw_files = s3FWCache.get(group, new Callable<Map<Integer, List<SyncResponse.FileDownload>>>() {
                            @Override
                            public Map<Integer, List<SyncResponse.FileDownload>> call() throws Exception {
                                LOGGER.debug("Nothing in cache found for group: [{}]. Grabbing info from S3.", group);
                                return getFirmwareFilesForGroup(group);
                            }
                        });
            } catch (ExecutionException e) {
                LOGGER.error("Exception while retrieving S3 file list.");
            }
        
        if (fw_files.isEmpty()) {
            LOGGER.error("Failed to retrieve S3 file list.");
            return Collections.EMPTY_LIST;
        }
        
        final Integer firmwareVersion = new ArrayList<>(fw_files.keySet()).get(0);
        
        LOGGER.warn("Versions to update: {}, current version = {} for deviceId = {}", firmwareVersion, currentFirmwareVersion, deviceId);
        if (firmwareVersion.equals(currentFirmwareVersion)) {
            LOGGER.warn("Versions match: {}, current version = {}", firmwareVersion, currentFirmwareVersion);
            return Collections.EMPTY_LIST;
        }
        
        /**
        // Cache Stats
        CacheStats stats = s3FWCache.stats();
        LOGGER.debug("Hit Rate: {}", stats.hitRate());
        LOGGER.debug("Miss Rate: {}", stats.missRate());
        LOGGER.debug("loadException Rate: {}", stats.loadExceptionRate());
        LOGGER.debug("Load Penalty: {}", stats.averageLoadPenalty());
        
        CacheStats delta = s3FWCache.stats()
                .minus(stats);
        LOGGER.debug("Hit Count: {}", delta.hitCount());
        LOGGER.debug("Miss Count: {}", delta.missCount());
        LOGGER.debug("Load Success Count: {}", delta.loadSuccessCount());
        LOGGER.debug("Load Exception Count: {}", delta.loadExceptionCount());
        LOGGER.debug("Total Load Time: {}", delta.totalLoadTime());
        **/
        
        return fw_files.get(firmwareVersion);
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
}