package com.hello.suripu.core.models;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by david on 2/3/17.
 * <p>
 * Represents one row from the database.
 */
@SuppressWarnings({"Guava", "WeakerAccess"})
public class VoiceCommand {

    private final String title;
    private final String description;
    private final String commandTitle;
    private final String command;
    private final String icon;

    public VoiceCommand(final String title,
                        final String description,
                        final String commandTitle,
                        final String command,
                        final String icon) {
        this.title = checkNotNull(title, "VoiceCommand title can not be null.");
        this.description = checkNotNull(description, "VoiceCommand description can not be null.");
        this.commandTitle = checkNotNull(commandTitle, "VoiceCommand commandTitle can not be null.");
        this.command = checkNotNull(command, "VoiceCommand command can not be null.");
        this.icon = checkNotNull(icon, "VoiceCommand icon can not be null.");
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

    public Optional<String> getNormalPhoto() {
        return Optional.of(formatIcon(PhotoSize.NORMAL));
    }

    public Optional<String> getHighPhoto() {
        return Optional.of(formatIcon(PhotoSize.HIGH));
    }

    public Optional<String> getExtraHighPhoto() {
        return Optional.of(formatIcon(PhotoSize.EXTRA_HIGH));
    }

    private String formatIcon(final PhotoSize photoSize) {
        return String.format("/%s@%dx.png", icon, photoSize.size);
    }

    private enum PhotoSize {
        NORMAL(1),
        HIGH(2),
        EXTRA_HIGH(3);
        private final int size;

        PhotoSize(final int size) {
            this.size = size;
        }

    }

}
