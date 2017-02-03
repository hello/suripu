package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by david on 2/3/17.
 */
public class VoiceCommandCategory {

    @JsonProperty("title")
    private final String title;

    @JsonProperty("commands")
    private final ArrayList<String> commands;

    public VoiceCommandCategory(final String title,
                                final ArrayList<String> commands) {
        this.title = checkNotNull(title, "VoiceCommandCategory title can not be null.");
        this.commands = checkNotNull(commands, "VoiceCommandCategory commands can not be null.");
    }
}
