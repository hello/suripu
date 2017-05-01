package com.hello.suripu.core.flipper;

public class FeatureFlipper {

    // PLEASE, KEEP CONSTANTS IN ALPHABETICAL ORDER!

    public final static String ALARM_WORKER_DROP_IF_TOO_OLD = "alarm_worker_drop_if_too_old";
    public final static String ALL_SENSOR_QUERY_USE_UTC_TS = "all_sensor_query_user_utc_ts";
    public final static String ALLOW_RESPONSE_COMMANDS = "allow_response_commands";
    public final static String ALWAYS_ON_AUDIO = "always_on_audio";
    public final static String ATTEMPT_TO_CORRECT_PILL_REPORTED_TIMESTAMP = "attempt_to_correct_pill_reported_timestamp";
    public final static String ATTEMPT_TO_CORRECT_SENSE_REPORTED_TIMESTAMP = "attempt_to_correct_sense_reported_timestamp";
    public final static String AUDIO_CAPTURE = "audio_capture";
    public final static String AUDIO_STORAGE = "audio_storage";
    public final static String AUDIO_PEAK_ENERGY_DB = "audio_peak_energy_db";

    public final static String BYPASS_OTA_CHECKS = "bypass_ota_checks";

    public final static String CALIBRATION = "calibration";
    public final static String CBTI_GOAL_GO_OUTSIDE = "cbti_goal_go_outside";
    public final static String COMMON_DB_UNAVAILABLE = "common_db_unavailable";
    public final static String COMPENSATE_LIGHT_WITH_SENSE_COLOR = "compensate_light_with_sense_color";

    public final static String DAY_SLEEPER = "day_sleeper";
    public final static String DEBUG_MODE_PILL_PAIRING = "debug-mode-pill-pairing";
    public final static String DECAYING_SMART_ALARM_THRESHOLD = "decaying-smart-alarm_threshold";
    public final static String DELAY_CURRENT_ROOM_STATE_THRESHOLD = "delay_current_room_state_threshold";
    public final static String DUST_SMOOTH = "dust_smooth";
    public final static String DYNAMODB_DEVICE_DATA = "dynamodb_device_data";
    public final static String DYNAMODB_DEVICE_DATA_TIMELINE = "dynamodb_device_data_timeline";
    public final static String DYNAMODB_DEVICE_DATA_INSIGHTS = "dynamodb_device_data_insights";
    public final static String DYNAMODB_PILL_DATA_TIMELINE = "dynamodb_pill_data_timeline";

    public final static String ENABLE_OTA_UPDATES = "enable_ota_updates";
    public final static String ENVIRONMENT_IN_TIMELINE_SCORE = "environment_in_timeline_score";
    public final static String EXPIRE_TIMELINE_IN_PROCESSING_TIME_SPAN = "expire_timeline_in_processing_time_span";
    public final static String EXTRA_EVENTS = "extra_events";

    public final static String FEEDBACK_IN_TIMELINE = "feedback_in_timeline";
    public final static String FORCE_EVT_OTA_UPDATE = "pang-fire-fighting";
    public final static String FORCE_HTTP_500 = "force_http_500";
    public final static String FW_VERSIONS_REQUIRING_UPDATE = "fw_versions_requiring_update";

    public final static String HMM_ALGORITHM = "hmm_algorithm";
    public final static String HMM_PARTNER_FILTER = "hmm_partner_filter";

    public final static String IN_OUT_BED_EVENTS = "in_out_bed_events";
    public final static String INCREASE_UPLOAD_INTERVAL = "increase_upload_interval";
    public final static String INSIGHTS_AIR_QUALITY = "insights_air_quality";
    public final static String INSIGHTS_BED_LIGHT_DURATION = "insights_bed_light_duration";
    public final static String INSIGHTS_BED_LIGHT_INTENSITY_RATIO = "insights_bed_light_intensity_ratio";
    public final static String INSIGHTS_CAFFEINE = "insights_caffeine";
    public final static String INSIGHTS_HUMIDITY = "insights_humidity";
    public final static String INSIGHTS_LAST_SEEN = "insights_last_seen";
    public final static String INSIGHTS_SLEEP_DEPRIVATION = "insights_sleep_deprivation";
    public final static String INSIGHTS_WAKE_VARIANCE = "insights_wake_variance";
    public final static String INSIGHTS_MARKETING_SCHEDULE = "insights_marketing_schedule";
    public final static String INSIGHTS_SLEEP_TIME = "insights_sleep_time";
    public final static String IN_BED_SEARCH = "in_bed_search";

    public final static String MEASURE_CLOCK_DRIFT = "measure_clock_drift";
    public final static String MISSING_DATA_DEFAULT_VALUE = "missing_data_default_value";
    public final static String MOTION_MASK_PARTNER_FILTER = "motion_mask_partner_filter";
    public final static String MIN_MOTION_AMPLITUDE_HIGH_THRESHOLD = "min_motion_amplitude_high_threshold";


    public final static String NEW_INVALID_NIGHT_FILTER = "new_invalid_night_filter";
    public final static String NEW_ROOM_CONDITION = "new_room_condition";
    public final static String NEURAL_NET_ALGORITHM = "neural_net_algorithm";
    public final static String NEURAL_NET_FOUR_EVENTS_ALGORITHM = "neural_net_four_events_algorithm";

    public final static String OFF_BED_HMM_MOTION_FILTER = "off_bed_hmm_motion_filter";
    public final static String OFFICE_ONLY_OVERRIDE = "office_only_override";
    public final static String ONLINE_HMM_ALGORITHM = "online_hmm_algorithm";
    public final static String ONLINE_HMM_LEARNING = "online_hmm_learning";
    public final static String OTA_RELEASE = "release";
    public final static String OUTLIER_FILTER = "outlier_filter";

    public final static String PARTNER_FILTER = "partner_filter";
    public final static String PCH_SPECIAL_OTA = "pch_special_ota";
    public final static String PERSIST_SIGNIFICANT_WIFI_RSSI_CHANGE = "persist_significant_wifi_rssi_change";
    public final static String PILL_HEARTBEAT_DYNAMODB = "pill_heartbeat_dynamodb";
    public final static String PILL_HEARTBEAT_DYNAMODB_READ = "pill_heartbeat_dynamodb_read";
    public final static String PILL_PAIR_MOTION_FILTER = "pill_pair_motion_filter";
    public final static String POLLY_RESPONSE_SERVICE = "polly_response_service";
    public final static String PRINT_RAW_PB = "print_raw_pb";
    public final static String PROGRESSIVE_SMART_ALARM = "progressive_smart_alarm";
    public final static String PUSH_NOTIFICATIONS_ENABLED = "push_notifications_enabled";

    public final static String QUESTION_ANOMALY_LIGHT_VISIBLE = "question_anomaly_light_enabled";
    public final static String QUESTION_ASK_TIME_ENABLED = "question_ask_time_enabled";
    public final static String QUESTION_SURVEY_PROCESSOR_ENABLED = "question_survey_processor_enabled";
    public final static String QUESTION_CORE_PROCESSOR_ENABLED = "question_core_processor_enabled";

    public final static String REBOOT_CLOCK_OUT_OF_SYNC_DEVICES = "reboot_clock_out_of_sync_devices";
    public final static String REDUCE_BATCH_UPLOAD_INTERVAL = "reduce_batch_upload_interval";
    public final static String RING_DURATION_FROM_CONFIG = "ring_duration_from_config";

    public final static String SENSE_LAST_SEEN_VIEW_DYNAMODB = "sense_last_seen_view_dynamodb";
    public final static String SENSE_LAST_SEEN_VIEW_DYNAMODB_READ = "sense_last_seen_view_dynamodb_read";
    public final static String SENSORS_DB_UNAVAILABLE = "sensors_db_unavailable";
    public final static String SLEEP_SCORE_DURATION_WEIGHTING = "sleep_score_duration_weighting";
    public final static String SLEEP_SCORE_DURATION_WEIGHTING_V2 = "sleep_score_duration_weighting_V2";
    public final static String SLEEP_SCORE_DURATION_V2 = "sleep_score_duration_v2";
    public final static String SLEEP_SCORE_NO_MOTION_ENFORCEMENT = "sleep_score_no_motion_enforcement";
    public final static String SLEEP_SCORE_TIMES_AWAKE_PENALTY = "sleep_score_times_awake_penalty";
    public final static String SLEEP_SCORE_V3 = "sleep_score_v3";
    public final static String SLEEP_SCORE_V4 = "sleep_score_v4";
    public final static String SLEEP_SCORE_V5 = "sleep_score_v5";

    // Return enum to the app that Sleep Sounds cannot be played because Sense requires a firmware update
    public final static String SLEEP_SOUNDS_DISPLAY_FW_UPDATE = "sleep_sounds_display_fw_update";

    // Show the Sleep Sounds UI in the app
    public final static String SLEEP_SOUNDS_ENABLED = "sleep_sounds_enabled";

    public final static String SLEEP_SOUNDS_OVERRIDE_OTA = "sleep_sounds_override_ota";
    public final static String SLEEP_SEGMENT_OFFSET_REMAPPING = "sleep_segment_offset_remapping";
    public final static String SLEEP_STATS_MEDIUM_SLEEP = "sleep_stats_medium_sleep";
    public final static String SLEEP_STATS_UNINTERRUPTED_SLEEP = "sleep_stats_uninterrupted_sleep";
    public final static String SMART_ALARM = "smart_alarm";

    public final static String SMART_ALARM_LOGGING = "smart_alarm_log";
    public final static String SMART_ALARM_REFACTORED = "smart_alarm_refactored";
    public final static String SOUND_EVENTS_USE_HIGHER_THRESHOLD = "sound_events_use_higher_threshold";
    public final static String SOUND_INFO_TIMELINE = "sound_info_timeline";
    public final static String STOP_PROCESS_TIMELINE_FROM_WORKER = "stop_process_timeline_from_worker";

    public final static String TIMELINE_EVENT_SLEEP_SCORE_ENFORCEMENT = "timeline_event_sleep_score_enforcement";
    public final static String TIMELINE_IN_SLEEP_INSIGHTS = "timeline_in_sleep_insights";
    public final static String TIMELINE_SLEEP_PERIOD = "timeline_sleep_period";
    public final static String TIMELINE_V2_AVAILABLE = "timeline_v2_available";
    public final static String TIMELINE_LOCKDOWN = "timeline_lockdown";
    public final static String TIMELINE_PROCESSOR_V3_ENABLED = "timeline_processor_v3_enabled";

    public final static String VIEW_SENSORS_UNAVAILABLE = "view_sensors_unavailable";
    public final static String VIEW_TIMELINE_UNAVAILABLE = "view_timeline_unavailable";
    public final static String VIEW_TRENDS_UNAVAILABLE = "view_trends_unavailable";
    public final static String VOTING_ALGORITHM = "voting_algorithm";

    public final static String WORKER_CLEAR_ALL_CACHE = "worker_clear_all_cache";
    public final static String WORKER_PG_CACHE = "worker_pg_cache";
    public final static String WORKER_KINESIS_TIMEZONES = "worker_kinesis_timezones";

    public final static String APP_LAST_SEEN_PUSH_ENABLED = "app_last_seen_push_enabled";
}