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

    @JsonProperty("voice_command_topics")
    private final List<VoiceCommandTopic> voiceCommandTopics;

    public VoiceCommandResponse(final List<VoiceCommand> dbVoiceCommands) {
        checkNotNull(dbVoiceCommands, "VoiceCommandResponse dbVoiceCommands can not be null.");
        Map<String, VoiceCommandTopic> tempMap = new HashMap<>();
        for (final VoiceCommand voiceCommand : dbVoiceCommands) {
            final VoiceCommandTopic voiceCommandTopic;
            if (tempMap.containsKey(voiceCommand.getTitle())) {
                voiceCommandTopic = tempMap.get(voiceCommand.getTitle());
            } else {
                voiceCommandTopic = new VoiceCommandTopic(voiceCommand.getTitle(), voiceCommand.getDescription());
                tempMap.put(voiceCommand.getTitle(), voiceCommandTopic);

            }
            voiceCommandTopic.addCategory(voiceCommand.getCommandTitle(), voiceCommand.getCommand());
        }
        voiceCommandTopics = new ArrayList<>(tempMap.values());
    }

    private class VoiceCommandTopic {

        @JsonProperty("title")
        private final String title;

        @JsonProperty("description")
        private final String description;

        @JsonProperty("subtopics")
        private final List<VoiceCommandSubTopic> voiceCommandSubTopics = new ArrayList<>();


        VoiceCommandTopic(final String title,
                          final String description) {
            this.title = checkNotNull(title, "VoiceCommandTopic title can not be null.");
            this.description = checkNotNull(description, "VoiceCommandTopic description can not be null.");
        }

        void addCategory(final String commandTitle,
                         final String command) {
            for (final VoiceCommandSubTopic voiceCommandSubTopic : this.voiceCommandSubTopics) {
                if (voiceCommandSubTopic.commandTitle.equals(commandTitle)) {
                    voiceCommandSubTopic.addCommand(command);
                    return;
                }
            }
            this.voiceCommandSubTopics.add(new VoiceCommandSubTopic(commandTitle, command));
        }
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private class VoiceCommandSubTopic {

        @JsonProperty("command_title")
        private final String commandTitle;

        @JsonProperty("commands")
        private final List<String> commands = new ArrayList<>();

        VoiceCommandSubTopic(final String commandTitle,
                             final String command) {
            this.commandTitle = checkNotNull(commandTitle, "VoiceCommandSubTopic commandTitle can not be null.");
            this.commands.add(checkNotNull(command, "VoiceCommandSubTopic command can not be null."));
        }

        void addCommand(final String command) {
            this.commands.add(checkNotNull(command, "VoiceCommandSubTopic command can not be null."));
        }
    }


}
