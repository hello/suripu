package com.hello.suripu.core.models;

import com.hello.suripu.core.firmware.HardwareVersion;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by jakepiccolo on 3/9/16.
 */
public class FileInfo {

    public enum FileType {
        SLEEP_SOUND,
        ALARM
    }

    public final Long id;

    /**
     * Sense File path
     */
    public final String path;

    public final FileType fileType;

    public final String sha;

    public final String uri;

    public final String previewUri;

    public final String name;

    public final Integer firmwareVersion;
    public final HardwareVersion hardwareVersion;

    final Boolean isPublic;

    public final Integer sizeBytes;

    private FileInfo(Builder builder) {
        id = builder.id;
        path = builder.path;
        fileType = builder.fileType;
        sha = builder.sha;
        uri = builder.uri;
        previewUri = builder.previewUri;
        name = builder.name;
        firmwareVersion = builder.firmwareVersion;
        hardwareVersion = builder.hardwareVersion;
        isPublic = builder.isPublic;
        sizeBytes = builder.sizeBytes;

        checkNotNull(id, "id must not be null");
        checkNotNull(path, "path must not be null");
        checkNotNull(fileType, "fileType must not be null");
        checkNotNull(sha, "sha must not be null");
        checkNotNull(uri, "previewUri must not be null");
        checkNotNull(name, "name must not be null");
        checkNotNull(firmwareVersion, "firmwareVersion must not be null");
        checkNotNull(hardwareVersion, "hardware version must no be null");
        checkNotNull(isPublic, "isPublic must not be null");
    }

    public byte[] getShaBytes() throws DecoderException {
        return Hex.decodeHex(sha.toCharArray());
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private Long id;
        private String path;
        private FileType fileType;
        private String sha;
        private String uri;
        private String previewUri;
        private String name;
        private Integer firmwareVersion;
        private HardwareVersion hardwareVersion = HardwareVersion.SENSE_ONE;
        private Boolean isPublic = false;
        private Integer sizeBytes;

        private Builder() {
        }

        public Builder withId(final Long val) {
            id = val;
            return this;
        }

        public Builder withPath(String val) {
            path = val;
            return this;
        }

        public Builder withFileType(FileType val) {
            fileType = val;
            return this;
        }

        public Builder withSha(String val) {
            sha = val;
            return this;
        }

        public Builder withUri(String val) {
            uri = val;
            return this;
        }

        public Builder withPreviewUri(String val) {
            previewUri = val;
            return this;
        }

        public Builder withName(String val) {
            name = val;
            return this;
        }

        public Builder withFirmwareVersion(Integer val) {
            firmwareVersion = val;
            return this;
        }

        public Builder withHardwareVersion(HardwareVersion val) {
            hardwareVersion = val;
            return this;
        }

        public Builder withIsPublic(Boolean val) {
            isPublic = val;
            return this;
        }

        public Builder withSizeBytes(final Integer val) {
            sizeBytes = val;
            return this;
        }



        public FileInfo build() {
            return new FileInfo(this);
        }
    }
}
