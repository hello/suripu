package com.hello.suripu.core.models;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by david on 2/3/17.
 * <p>
 * Represents one row from the database.
 */
public class VoiceCommandRow {

    private final String title;
    private final String description;
    private final String commandTitle;
    private final String command;

    public VoiceCommandRow(final String title,
                           final String description,
                           final String commandTitle,
                           final String command) {
        this.title = checkNotNull(title, "VoiceCommandRow title can not be null.");
        this.description = checkNotNull(description, "VoiceCommandRow description can not be null.");
        this.commandTitle = checkNotNull(commandTitle, "VoiceCommandRow commandTitle can not be null.");
        this.command = checkNotNull(command, "VoiceCommandRow command can not be null.");
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public String getCommandTitle() {
        return this.commandTitle;
    }

    public String getCommand() {
        return this.command;
    }
}
