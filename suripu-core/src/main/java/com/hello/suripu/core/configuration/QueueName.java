package com.hello.suripu.core.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum QueueName {

    AUDIO_FEATURES("audio_features"),
    ACTIVITY_STREAM("activity_stream"),
    REGISTRATIONS("registrations"),
    ENCODE_AUDIO("encode_audio"),
    BATCH_PILL_DATA ("batch_pill_data"),
    SENSE_SENSORS_DATA("sense_sensors_data"),
    LOGS("logs"),
    WORKER_TASKS("worker_tasks");

    private String value;

    private QueueName(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @JsonCreator
    public static QueueName fromString(final String val) {
        final QueueName[] queueNames = QueueName.values();

        for (final QueueName queueName: queueNames) {
            if (queueName.value.equals(val)) {
                return queueName;
            }
        }

        throw new IllegalArgumentException(String.format("%s is not a valid KinesisStreamName", val));
    }
}
