package com.hello.suripu.core.actions;

import com.hello.suripu.core.trends.v2.TimeScale;

/**
 * Created by ksg on 1/23/17
 */
public enum ActionType {
    LOGIN("login"), // set
    LOGOUT("logout"),
    TRENDS_WEEK("trends_week"), // set
    TRENDS_MONTH("trends_month"),   // set
    TRENDS_QUARTER("trends_quarter"),   // set
    TIMELINE_V2("timeline_v2"), // set
    TIMELINE_FEEDBACK("timeline_feedback"), // set
    TIMELINE_CORRECT("timeline_correct"), // set
    PILL_PAIR("pill_pair"),
    PILL_UNPAIR("pill_unpair"), // set
    SENSE_PAIR("sense_pair"),
    SENSE_UNPAIR("sense_unpair"), // set
    FACTORY_RESET_UNPAIR("factory_reset_unpair"), // set
    ALARM_SET("alarm_set"),
    ALARM_GET("alarm_get"),
    INSIGHTS_DETAILS("insights_detail"),
    FORCE_OTA("force_ota"), // set
    PASSWORD_RESET("password_reset"),   // set
    PHOTO_UPLOAD("photo_upload"),
    PROVISION_SENSE("provision_sense"),
    PROVISION_PILL("provision_pill"),
    QUESTION_GET("question_get"),   // set
    QUESTION_SAVE("question_save"), // set
    QUESTION_SKIP("question_skip"), // set
    ROOM_CONDITIONS_CURRENT("room_conditions_current"), // set
    ROOM_CONDITIONS_BATCH("room_conditions_batch"), // set
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

    public static ActionType getTrendsType(final TimeScale timeScale) {
        if (timeScale.equals(TimeScale.LAST_WEEK)) {
            return TRENDS_WEEK;
        } else if (timeScale.equals(TimeScale.LAST_MONTH)) {
            return TRENDS_MONTH;
        } else {
            return TRENDS_QUARTER;
        }
    }
}
