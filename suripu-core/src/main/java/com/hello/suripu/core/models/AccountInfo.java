package com.hello.suripu.core.models;

/**
 * Created by kingshy on 1/12/15.
 */
public class AccountInfo {
    public enum Type {
        NONE("none"),
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

        public static Type fromString(final String text) {
            if (text == null) {
                return Type.NONE;
            }

            for (final Type infoType : Type.values()) {
                if (text.equalsIgnoreCase(infoType.toString())) {
                    return infoType;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    public enum SleepTempType {
        HOT(1), // note, values are response_id
        COLD(2),
        NONE(3);

        private int value;

        private SleepTempType(final int value) {this.value = value;}

        public static SleepTempType fromInteger(final int value) {
            switch (value) {
                case 1:
                    return SleepTempType.HOT;
                case 2:
                    return SleepTempType.COLD;
                default:
                    return SleepTempType.NONE;
            }
        }
    }

}
