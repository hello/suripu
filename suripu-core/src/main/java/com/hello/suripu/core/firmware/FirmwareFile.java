package com.hello.suripu.core.firmware;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FirmwareFile {

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

    @JsonIgnore
    public String sha1;

    private FirmwareFile(
            final Boolean copyToSerialFlash,
            final Boolean resetNetworkProcessor,
            final Boolean resetApplicationProcessor,
            final String serialFlashFilename,
            final String serialFlashPath,
            final String sdCardFilename,
            final String sdCardPath) {
        this.copyToSerialFlash = copyToSerialFlash;
        this.resetNetworkProcessor = resetNetworkProcessor;
        this.resetApplicationProcessor = resetApplicationProcessor;
        this.serialFlashFilename = serialFlashFilename;
        this.serialFlashPath = serialFlashPath;
        this.sdCardFilename = sdCardFilename;
        this.sdCardPath = sdCardPath;
        this.sha1 = "";
    }

    @JsonCreator
    public static FirmwareFile create(
            @JsonProperty("copy_to_serial_flash") final Boolean copyToSerialFlash,
            @JsonProperty("reset_network_processor") final Boolean resetNetworkProcessor,
            @JsonProperty("reset_application_processor") final Boolean resetApplicationProcessor,
            @JsonProperty("serial_flash_filename") final String serialFlashFilename,
            @JsonProperty("serial_flash_path") final String serialFlashPath,
            @JsonProperty("sd_card_filename") final String sdCardFilename,
            @JsonProperty("sd_card_path") final String sdCardPath) {
        return new FirmwareFile(copyToSerialFlash, resetNetworkProcessor, resetApplicationProcessor, serialFlashFilename, serialFlashPath, sdCardFilename, sdCardPath);
    }

    private FirmwareFile(
            final Boolean copyToSerialFlash,
            final Boolean resetNetworkProcessor,
            final Boolean resetApplicationProcessor,
            final String serialFlashFilename,
            final String serialFlashPath,
            final String sdCardFilename,
            final String sdCardPath,
            final String sha1) {
        this.copyToSerialFlash = copyToSerialFlash;
        this.resetNetworkProcessor = resetNetworkProcessor;
        this.resetApplicationProcessor = resetApplicationProcessor;
        this.serialFlashFilename = serialFlashFilename;
        this.serialFlashPath = serialFlashPath;
        this.sdCardFilename = sdCardFilename;
        this.sdCardPath = sdCardPath;
        this.sha1 = sha1;
    }
}
