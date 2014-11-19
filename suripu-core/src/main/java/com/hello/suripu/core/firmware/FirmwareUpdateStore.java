package com.hello.suripu.core.firmware;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.protobuf.ByteString;
import com.hello.suripu.api.output.OutputProtos.SyncResponse;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FirmwareUpdateStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirmwareUpdateStore.class);

    private final FirmwareUpdateDAO firmwareUpdateDAO;
    private final AmazonS3 s3;
    private final String bucketName;

    public FirmwareUpdateStore(final FirmwareUpdateDAO firmwareUpdateDAOImpl, final AmazonS3 s3, final String bucketName) {
        this.firmwareUpdateDAO = firmwareUpdateDAOImpl;
        this.s3 = s3;
        this.bucketName = bucketName;
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

            final URL s = s3.generatePresignedUrl(generatePresignedUrlRequest);
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

    /**
     * Downloads files from s3 bucket matching the group name
     * @param group
     * @return
     */
    public List<SyncResponse.FileDownload> getFirmwareUpdate(final String group) {

        final ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.withBucketName(bucketName);
        listObjectsRequest.withPrefix("sense/" + group);

        // TODO: add caching?
        final ObjectListing objectListing = s3.listObjects(listObjectsRequest);
        final List<String> files = new ArrayList<>();

        for(final S3ObjectSummary summary: objectListing.getObjectSummaries()) {
            if(!summary.getKey().contains(".map") || summary.getKey().contains(".out")) {
                LOGGER.trace("Adding file: {} to list of files to be prepared for update", summary.getKey());
                files.add(summary.getKey());
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

            final URL s = s3.generatePresignedUrl(generatePresignedUrlRequest);
            LOGGER.debug("S3 URL: {}", s);

            final SyncResponse.FileDownload.Builder fileDownloadBuilder = SyncResponse.FileDownload.newBuilder()
                    .setUrl(s.getPath() + "?" + s.getQuery())
                    .setHost(s.getHost()); // TODO: replace with hello s3 proxy

            if(f.contains("kitsune.bin")) {
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

                final String fakeSha1 = "fakeSha1";
                try {
                    fileDownloadBuilder.setSha1(ByteString.copyFrom(Hex.decodeHex(fakeSha1.toCharArray())));
                } catch (DecoderException e) {
                    LOGGER.error("Failed decoding sha1 from hex");
                }

                fileDownloadList.add(fileDownloadBuilder.build());
            }
        }

        return fileDownloadList;
    }
}