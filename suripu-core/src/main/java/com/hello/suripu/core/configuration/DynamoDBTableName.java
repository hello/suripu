package com.hello.suripu.core.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DynamoDBTableName {

    ALARM("alarm"),
    ALARM_INFO("alarm_info"),
    ALARM_LOG("alarm_log"),
    ALGORITHM_TEST("algorithm_test"),
    APP_STATS("app_stats"),
    CALIBRATION("calibration"),
    DEVICE_DATA("device_data"),
    FEATURE_EXTRACTION_MODELS("feature_extraction_models"),
    FEATURES("features"),
    FILE_MANIFEST("file_manifest"),
    FIRMWARE_UPGRADE_PATH("firmware_upgrade_path"),
    FIRMWARE_VERSIONS("firmware_versions"),
    INSIGHTS("insights"),
    MARKETING_INSIGHTS_SEEN("marketing_insights_seen"),
    NOTIFICATIONS("notifications"),
    ONLINE_HMM_MODELS("online_hmm_models"),
    OTA_HISTORY("ota_history"),
    PASSWORD_RESET("password_reset"),
    PILL_DATA("pill_data"),
    PILL_HEARTBEAT("pill_heartbeat"),
    PILL_KEY_STORE("pill_key_store"),
    PILL_LAST_SEEN("pill_last_seen"),
    PREFERENCES("preferences"),
    PUSH_NOTIFICATION_EVENT("push_notification_event"),
    RING_TIME("ring_time"),
    RING_TIME_HISTORY("ring_time_history"),
    SENSE_EVENTS("sense_events"),
    SENSE_KEY_STORE("sense_key_store"),
    SENSE_LAST_SEEN("sense_last_seen"),
    SENSE_PREFIX("sense_prefix"),
    SENSE_STATE("sense_state"),
    SLEEP_HMM("sleep_hmm"),
    SLEEP_SCORE("sleep_score"),
    SLEEP_SCORE_PARAMETERS("sleep_score_parameters"),
    SLEEP_STATS("sleep_stats"),
    SMART_ALARM_LOG("smart_alarm_log"),
    SYNC_RESPONSE_COMMANDS("sync_response_commands"),
    TAGS("tags"),
    TEAMS("teams"),
    TIMELINE("timeline"),
    TIMELINE_LOG("timeline_log"),
    TIMEZONE_HISTORY("timezone_history"),
    WIFI_INFO("wifi_info"),
    WORKER_LAUNCH_HISTORY("worker_launch_history");


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
