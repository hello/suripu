package com.hello.suripu.core.firmware;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FirmwareFile {

    @JsonProperty("s3_key")
    public final String s3Key;

    @JsonProperty("s3_bucket")
    public final String s3Bucket;

    @JsonProperty("copy_to_serial_flash")
    public final Boolean copyToSerialFlash;

    @JsonProperty("reset_network_processor")
    public final Boolean resetNetworkProcessor;

    @JsonProperty("reset_application_processor")
    public final Boolean resetApplicationProcessor;

    @JsonProperty("serial_flash_filename")
    public final String serialFlashFilename;

    @JsonProperty("serial_flash_path")
    public final String serialFlashPath;

    @JsonProperty("sd_card_filename")
    public final String sdCardFilename;

    @JsonProperty("sd_card_path")
    public final String sdCardPath;

    @JsonCreator
    public FirmwareFile(
            @JsonProperty("s3_bucket") final String s3Bucket,
            @JsonProperty("s3_key") final String s3Key,
            @JsonProperty("copy_to_serial_flash") final Boolean copyToSerialFlash,
            @JsonProperty("reset_network_processor") final Boolean resetNetworkProcessor,
            @JsonProperty("reset_application_processor") final Boolean resetApplicationProcessor,
            @JsonProperty("serial_flash_filename") final String serialFlashFilename,
            @JsonProperty("serial_flash_path") final String serialFlashPath,
            @JsonProperty("sd_card_filename") final String sdCardFilename,
            @JsonProperty("sd_card_path") final String sdCardPath) {
        this.s3Key = s3Key;
        this.s3Bucket = s3Bucket;
        this.copyToSerialFlash = copyToSerialFlash;
        this.resetNetworkProcessor = resetNetworkProcessor;
        this.resetApplicationProcessor = resetApplicationProcessor;
        this.serialFlashFilename = serialFlashFilename;
        this.serialFlashPath = serialFlashPath;
        this.sdCardFilename = sdCardFilename;
        this.sdCardPath = sdCardPath;
    }


    public static FirmwareFile withS3Info(final FirmwareFile firmwareFile, final String s3Bucket, final String s3Key) {
        return new FirmwareFile(s3Bucket, s3Key, firmwareFile.copyToSerialFlash, firmwareFile.resetNetworkProcessor,
                firmwareFile.resetApplicationProcessor, firmwareFile.serialFlashFilename, firmwareFile.serialFlashPath,
                firmwareFile.sdCardFilename, firmwareFile.sdCardPath);
    }
}
