package com.hello.suripu.service.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FirmwareUpdate {

    @JsonProperty("name")
    public final String name;

    @JsonProperty("url")
    public final String url;

    @JsonProperty("created")
    public final String created;


    public FirmwareUpdate(final String name, final String url, final String created) {
        this.name = name;
        this.url = url;
        this.created = created;
    }
}
