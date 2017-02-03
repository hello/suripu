package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by david on 2/3/17.
 */
public class VoiceCommand {

    @JsonProperty("title")
    private final String title;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("categories")
    private final ArrayList<VoiceCommandCategory> categories;

    public VoiceCommand(final String title,
                        final String description,
                        final ArrayList<VoiceCommandCategory> categories) {

        this.title = checkNotNull(title, "VoiceCommand title can not be null.");
        this.description = checkNotNull(description, "VoiceCommand description can not be null.");
        this.categories = checkNotNull(categories, "VoiceCommand categories can not be null.");
    }
}
