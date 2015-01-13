package com.hello.suripu.core.models;

/**
 * Created by kingshy on 1/12/15.
 */
public class AccountInfo {
    public enum Type {
        CITY("city"),
        TEMPERATURE("temperature"), // hot or cold sleeper
        SOUND("sound"), // sensitivity to sound
        LIGHT("light"), // sensitivity to light -- scale
        WORKOUT("workout"), // workout regularly -- boolean
        LIGHT_SLEEPER("light_sleeper"), // boolean
        SNORE("snore"), // boolean
        SLEEP_TALK("sleep_talk"), // boolean
        CAFFEINE("caffeine"); // text

        private String value;
        private Type(String value) {
            this.value = value;
        }
    }
}
