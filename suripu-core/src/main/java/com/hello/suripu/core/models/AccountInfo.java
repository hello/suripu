package com.hello.suripu.core.models;

/**
 * Created by kingshy on 1/12/15.
 */
public class AccountInfo {
    public enum Type {
        SLEEP_TEMPERATURE("sleep_temperature"), // hot or cold sleeper
        WORKOUT("workout"),                     // workout regularly -- boolean
        SNORE("snore"),                         // boolean
        SLEEP_TALK("sleep_talk"),               // boolean
        CAFFEINE("caffeine"),                   // boolean
        LIGHT_SLEEPER("light_sleeper"),         // boolean
        NAP("nap"),                             // boolean
        TROUBLE_SLEEPING("trouble_sleeping"),   // boolean
        EAT_LATE("eat_late"),                   // type
        BEDTIME("bedtime");                     // type

        private String value;
        private Type(String value) {
            this.value = value;
        }
    }

    public enum SleepTempType {
        NONE(0),
        HOT(1),
        COLD(2);

        private int value;
        private SleepTempType(final int value) {this.value = value;}
    }
}
