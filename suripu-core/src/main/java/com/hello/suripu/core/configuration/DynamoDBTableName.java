package com.hello.suripu.core.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DynamoDBTableName {

    FEATURES("features"),
    ALARM_INFO("alarm_info"),
    SENSE_KEY_STORE("sense_key_store"),
    PILL_KEY_STORE("pill_key_store"),
    PASSWORD_RESET("password_reset"),
    SENSE_EVENTS("sense_events"),
    TEAMS("teams"),
    FIRMWARE_VERSIONS("firmware_versions"),
    SYNC_RESPONSE_COMMANDS("sync_response_commands"),
    OTA_HISTORY("ota_history"),
    FIRMWARE_UPGRADE_PATH("firmware_upgrade_path"),
    RING_TIME_HISTORY("ring_time_history"),
    SENSE_PREFIX("sense_prefix"),
    SENSE_LAST_SEEN("sense_last_seen"),
    PILL_LAST_SEEN("pill_last_seen"),
    ALARM("alarm");

    private String value;

    private DynamoDBTableName(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DynamoDBTableName fromString(final String val) {
        final DynamoDBTableName[] tableNames = DynamoDBTableName.values();

        for (final DynamoDBTableName tableName: tableNames) {
            if (tableName.value.equals(val)) {
                return tableName;
            }
        }

        throw new IllegalArgumentException(String.format("%s is not a valid DynamoDB Table name", val));
    }
}
