package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by david on 2/3/17.
 * <p>
 * This will be returned to our mobile friends.
 */
public class VoiceCommandResponse {

    @JsonProperty("voice_subjects")
    private final List<VoiceCommandSubject> voiceSubjects = new ArrayList<>();

    public VoiceCommandResponse(final List<VoiceCommand> dbVoiceCommands) {
        checkNotNull(dbVoiceCommands, "VoiceCommandResponse dbVoiceCommands can not be null.");
        for (final VoiceCommand voiceCommand : dbVoiceCommands) {
            VoiceCommandSubject voiceCommandSubject = getVoiceCommandSubject(voiceCommand.getTitle());
            if (voiceCommandSubject == null) {
                voiceCommandSubject = new VoiceCommandSubject(voiceCommand.getTitle(), voiceCommand.getDescription());
                this.voiceSubjects.add(voiceCommandSubject);
            }
            voiceCommandSubject.addCategory(voiceCommand.getCommandName(), voiceCommand.getCommandDescription());
        }

    }

    private VoiceCommandSubject getVoiceCommandSubject(final String title) {
        for (final VoiceCommandSubject voiceCommandSubject : this.voiceSubjects) {
            if (voiceCommandSubject.title.equals(title)) {
                return voiceCommandSubject;
            }
        }
        return null;
    }

    private class VoiceCommandSubject {

        @JsonProperty("title")
        private final String title;

        @JsonProperty("description")
        private final String description;

        @JsonProperty("command")
        private final List<VoiceCommandCategory> categories = new ArrayList<>();

        public VoiceCommandSubject(final String title,
                                   final String description) {
            this.title = checkNotNull(title, "VoiceCommandSubject title can not be null.");
            this.description = checkNotNull(description, "VoiceCommandSubject description can not be null.");
        }

        public void addCategory(final String commandName,
                                final String commandDescription) {
            for (final VoiceCommandCategory voiceCommandCategory : this.categories) {
                if (voiceCommandCategory.commandName.equals(commandName)) {
                    voiceCommandCategory.addDescription(commandDescription);
                    return;
                }
            }
            this.categories.add(new VoiceCommandCategory(commandName, commandDescription));
        }

    }

    private class VoiceCommandCategory {

        @JsonProperty("commandName")
        private final String commandName;

        @JsonProperty("commandDescriptions")
        private final List<String> commandDescriptions = new ArrayList<>();

        public VoiceCommandCategory(final String commandName,
                                    final String commandDescription) {
            this.commandName = checkNotNull(commandName, "VoiceCommandCategory commandName can not be null.");
            this.commandDescriptions.add(checkNotNull(commandDescription, "VoiceCommandCategory commandDescriptions can not be null."));
        }

        public void addDescription(final String commandDescription) {
            this.commandDescriptions.add(checkNotNull(commandDescription, "VoiceCommandCategory commandDescription can not be null."));
        }
    }


}
