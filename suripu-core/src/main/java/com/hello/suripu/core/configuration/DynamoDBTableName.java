package com.hello.suripu.core.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DynamoDBTableName {

    ALARM_INFO("alarm_info"),
    ALARM_LOG("alarm_log"),
    BAYESNET_MODEL("hmm_bayesnet_models"),
    BAYESNET_PRIORS("hmm_bayesnet_priors"),
    CALIBRATION("calibration"),
    FEATURES("features"),
    FIRMWARE_UPGRADE_PATH("firmware_upgrade_path"),
    FIRMWARE_VERSIONS("firmware_versions"),
    INSIGHTS("insights"),
    OTA_HISTORY("ota_history"),
    PASSWORD_RESET("password_reset"),
    PILL_KEY_STORE("pill_key_store"),
    PILL_LAST_SEEN("pill_last_seen"),
    RING_TIME_HISTORY("ring_time_history"),
    SENSE_EVENTS("sense_events"),
    SENSE_KEY_STORE("sense_key_store"),
    SENSE_LAST_SEEN("sense_last_seen"),
    SENSE_PREFIX("sense_prefix"),
    SLEEP_HMM("sleep_hmm"),
    SLEEP_SCORE("sleep_score"),
    SLEEP_STATS("sleep_stats"),
    SMART_ALARM_LOG("smart_alarm_log"),
    SYNC_RESPONSE_COMMANDS("sync_response_commands"),
    TEAMS("teams"),
    TIMELINE_LOG("timeline_log"),
    TIMELINE("timeline"),
    TIMEZONE_HISTORY("timezone_history"),
    WIFI_INFO("wifi_info");

    private String value;

    private DynamoDBTableName(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public DynamoDBTableName fromString(final String val) {
        final DynamoDBTableName[] tableNames = DynamoDBTableName.values();

        for (final DynamoDBTableName tableName: tableNames) {
            if (tableName.value.equalsIgnoreCase(val)) {
                return tableName;
            }
        }

        throw new IllegalArgumentException(String.format("%s is not a valid DynamoDB Table name", val));
    }
}
