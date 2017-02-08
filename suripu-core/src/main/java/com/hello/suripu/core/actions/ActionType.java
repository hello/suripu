package com.hello.suripu.core.actions;

/**
 * Created by ksg on 1/23/17
 */
public enum ActionType {
    LOGIN("login"),
    LOGOUT("logout"),
    TRENDS_WEEK("trends_week"),
    TRENDS_MONTH("trends_month"),
    TRENDS_QUARTER("trends_quarter"),
    TIMELINE_V1("timeline_v1"),
    TIMELINE_V2("timeline_v2"),
    TIMELINE_FEEDBACK("timeline_feedback"),
    TIMELINE_CORRECT("timeline_correct"),
    PILL_PAIR("pill_pair"),
    PILL_UNPAIR("pill_unpair"),
    SENSE_PAIR("sense_pair"),
    SENSE_UNPAIR("sense_unpair"),
    ALARM_SET("alarm_set"),
    ALARM_GET("alarm_get"),
    INSIGHTS_DETAILS("insights_detail"),
    FORCE_OTA("force_ota"),
    PASSWORD_RESET("password_reset"),
    PHOTO_UPLOAD("photo_upload"),
    PROVISION_SENSE("provision_sense"),
    PROVISION_PILL("provision_pill"),
    QUESTION_GET("question_get"),
    QUESTION_SAVE("question_save"),
    QUESTION_SKIP("question_skip"),
    ROOM_CONDITIONS_CURRENT("room_conditions_current"),
    ROOM_CONDITIONS_BATCH("room_conditions_batch"),
    ALERTS("alerts"),
    EXPANSION_SET_STATE("expansion_set_state"),
    EXPANSION_SET_CONFIG("expansion_set_config"),
    SLEEP_SOUND_PLAY("sleep_sound_play"),
    SLEEP_SOUND_STOP("sleep_sound_stop");

    private final String value;
    ActionType(final String value) {
        this.value = value;
    }

    public String string() { return value;}
}
