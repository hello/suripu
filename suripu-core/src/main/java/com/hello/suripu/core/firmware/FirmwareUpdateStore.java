package com.hello.suripu.core.firmware;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
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

    public FirmwareUpdateStore(final FirmwareUpdateDAO firmwareUpdateDAOImpl, final AmazonS3 s3) {
        this.firmwareUpdateDAO = firmwareUpdateDAOImpl;
        this.s3 = s3;
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
}
