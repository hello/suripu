package com.hello.suripu.core.models.sleep_sounds;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.firmware.HardwareVersion;
import com.hello.suripu.core.models.FileInfo;

/**
 * Created by jakepiccolo on 2/18/16.
 */
public class Sound {

    @JsonProperty("id")
    public final Long id;

    @JsonProperty("preview_url")
    public final String previewUrl;

    @JsonProperty("name")
    public final String name;

    @JsonIgnore
    public final String filePath;    // For Sense

    @JsonIgnore
    public final String url;

    @JsonIgnore
    public final HardwareVersion hardwareVersion;

    private Sound(final Long id, final String previewUrl, final String name, final String filePath, final String url, final HardwareVersion hardwareVersion) {
        this.id = id;
        this.name = name;
        this.previewUrl = previewUrl;
        this.filePath = filePath;
        this.url = url;
        this.hardwareVersion = hardwareVersion;
    }

    public static Sound create(final Long id, final String previewUrl, final String name, final String filePath, final String url, final HardwareVersion hardwareVersion) {
        return new Sound(id, previewUrl, name, filePath, url, hardwareVersion);
    }

    public static Sound fromFileInfo(final FileInfo fileInfo) {
        switch (fileInfo.fileType) {
            case SLEEP_SOUND:
            case ALARM:
                return create(fileInfo.id, fileInfo.previewUri, fileInfo.name, fileInfo.path, fileInfo.uri, fileInfo.hardwareVersion);
            default:
                throw new IllegalArgumentException(String.format("Invalid FileType for Sound: %s", fileInfo.fileType));
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Sound)) {
            return false;
        }
        final Sound otherSound = (Sound) obj;
        return (id.equals(otherSound.id) &&
                previewUrl.equals(otherSound.previewUrl) &&
                name.equals(otherSound.name) &&
                filePath.equals(otherSound.filePath) &&
                url.equals(otherSound.url));
    }
}
