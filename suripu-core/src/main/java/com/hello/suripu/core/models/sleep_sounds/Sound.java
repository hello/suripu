package com.hello.suripu.core.models.sleep_sounds;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    private Sound(final Long id, final String previewUrl, final String name, final String filePath, final String url) {
        this.id = id;
        this.name = name;
        this.previewUrl = previewUrl;
        this.filePath = filePath;
        this.url = url;
    }

    public static Sound create(final Long id, final String previewUrl, final String name, final String filePath, final String url) {
        return new Sound(id, previewUrl, name, filePath, url);
    }
}
