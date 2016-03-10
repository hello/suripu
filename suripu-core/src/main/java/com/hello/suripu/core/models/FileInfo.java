package com.hello.suripu.core.models;

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

    final Boolean isPublic;

    private FileInfo(Builder builder) {
        id = builder.id;
        path = builder.path;
        fileType = builder.fileType;
        sha = builder.sha;
        uri = builder.uri;
        previewUri = builder.previewUri;
        name = builder.name;
        firmwareVersion = builder.firmwareVersion;
        isPublic = builder.isPublic;
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
        private Boolean isPublic;

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

        public Builder withIsPublic(Boolean val) {
            isPublic = val;
            return this;
        }

        public FileInfo build() {
            return new FileInfo(this);
        }
    }
}
