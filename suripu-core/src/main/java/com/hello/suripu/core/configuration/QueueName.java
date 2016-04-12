package com.hello.suripu.core.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum QueueName {

    AUDIO_FEATURES("audio_features"),
    AUDIO_PRODUCTS("audio_products"),
    ACTIVITY_STREAM("activity_stream"),
    REGISTRATIONS("registrations"),
    ENCODE_AUDIO("encode_audio"),
    BATCH_PILL_DATA ("batch_pill_data"),
    SENSE_SENSORS_DATA("sense_sensors_data"),
    SENSE_SENSORS_DATA_FANOUT_ONE("sense_sensors_data_fanout_one"),
    LOGS("logs");

    private String value;

    private QueueName(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public QueueName fromString(final String val) {
        return QueueName.getFromString(val);
    }

    public static QueueName getFromString(final String val) {
        final QueueName[] queueNames = QueueName.values();

        for (final QueueName queueName: queueNames) {
            if (queueName.value.equalsIgnoreCase(val)) {
                return queueName;
            }
        }

        throw new IllegalArgumentException(String.format("%s is not a valid KinesisStreamName", val));
    }
}
