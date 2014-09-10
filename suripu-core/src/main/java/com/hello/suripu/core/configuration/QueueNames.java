package com.hello.suripu.core.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum QueueNames {

    PILL_DATA ("pill_data"),
    MORPHEUS_DATA ("morpheus_data"),
    AUDIO_FEATURES("audio_features");

    private String value;

    private QueueNames(String value) {
        this.value = value;
    }


    @JsonCreator
    public static QueueNames fromString(final String val) {
        final QueueNames[] queueNames = QueueNames.values();

        for (final QueueNames queueName: queueNames) {
            if (queueName.value.equals(val)) {
                return queueName;
            }
        }

        throw new IllegalArgumentException(String.format("%s is not a valid KinesisStreamName", val));
    }
}
