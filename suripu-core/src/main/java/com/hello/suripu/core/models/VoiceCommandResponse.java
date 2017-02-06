package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by david on 2/3/17.
 * <p>
 * This will be returned to our mobile friends.
 */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class VoiceCommandResponse {

    @JsonProperty("voice_subjects")
    private final List<VoiceCommandSubject> voiceSubjects;

    public VoiceCommandResponse(final List<VoiceCommandRow> dbVoiceCommandRows) {
        checkNotNull(dbVoiceCommandRows, "VoiceCommandResponse dbVoiceCommandRows can not be null.");
        Map<String, VoiceCommandSubject> tempMap = new HashMap<>();
        for (final VoiceCommandRow voiceCommandRow : dbVoiceCommandRows) {
            final VoiceCommandSubject voiceCommandSubject;
            if (tempMap.containsKey(voiceCommandRow.getTitle())) {
                voiceCommandSubject = tempMap.get(voiceCommandRow.getTitle());
            } else {
                voiceCommandSubject = new VoiceCommandSubject(voiceCommandRow.getTitle(), voiceCommandRow.getDescription());
                tempMap.put(voiceCommandRow.getTitle(), voiceCommandSubject);

            }
            voiceCommandSubject.addCategory(voiceCommandRow.getCommandTitle(), voiceCommandRow.getCommand());
        }
        voiceSubjects = new ArrayList<>(tempMap.values());
    }

    private class VoiceCommandSubject {

        @JsonProperty("title")
        private final String title;

        @JsonProperty("description")
        private final String description;

        @JsonProperty("commands")
        private final List<VoiceCommand> voiceCommands = new ArrayList<>();


        VoiceCommandSubject(final String title,
                            final String description) {
            this.title = checkNotNull(title, "VoiceCommandSubject title can not be null.");
            this.description = checkNotNull(description, "VoiceCommandSubject description can not be null.");
        }

        void addCategory(final String commandTitle,
                         final String command) {
            for (final VoiceCommand voiceCommand : this.voiceCommands) {
                if (voiceCommand.commandTitle.equals(commandTitle)) {
                    voiceCommand.addCommand(command);
                    return;
                }
            }
            this.voiceCommands.add(new VoiceCommand(commandTitle, command));
        }
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private class VoiceCommand {

        @JsonProperty("command_title")
        private final String commandTitle;

        @JsonProperty("command")
        private final List<String> commands = new ArrayList<>();

        VoiceCommand(final String commandTitle,
                     final String command) {
            this.commandTitle = checkNotNull(commandTitle, "VoiceCommand commandTitle can not be null.");
            this.commands.add(checkNotNull(command, "VoiceCommand command can not be null."));
        }

        void addCommand(final String command) {
            this.commands.add(checkNotNull(command, "VoiceCommand command can not be null."));
        }
    }


}
