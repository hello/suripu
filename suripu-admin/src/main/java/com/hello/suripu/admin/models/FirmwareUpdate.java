package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.collect.Ordering;

import javax.annotation.Nullable;

public class FirmwareUpdate {

    @JsonProperty("name")
    public final String name;

    @JsonProperty("url")
    public final String url;

    @JsonProperty("created")
    public final long created;


    public FirmwareUpdate(final String name, final String url, final long created) {
        this.name = name;
        this.url = url;
        this.created = created;
    }


    public static Ordering<FirmwareUpdate> createOrdering() {
        return Ordering.natural()
                       .reverse()
                       .onResultOf(new Function<FirmwareUpdate, Comparable>() {
                           @Nullable
                           @Override
                           public Comparable apply(final FirmwareUpdate update) {
                               return update.created;
                           }
                       });
    }
}
