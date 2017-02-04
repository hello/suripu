package com.hello.suripu.core.models;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by david on 2/3/17.
 * <p>
 * Represents one row from the database.
 */
public class VoiceCommand {

    private final String title;
    private final String description;
    private final String commandName;
    private final String commandDescription;

    public VoiceCommand(final String title,
                        final String description,
                        final String commandName,
                        final String commandDescription) {
        this.title = checkNotNull(title, "VoiceCommand title can not be null.");
        this.description = checkNotNull(description, "VoiceCommand description can not be null.");
        this.commandName = checkNotNull(commandName, "VoiceCommand commandName can not be null.");
        this.commandDescription = checkNotNull(commandDescription, "VoiceCommand commandDescription can not be null.");
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public String getCommandName() {
        return this.commandName;
    }

    public String getCommandDescription() {
        return this.commandDescription;
    }
}
