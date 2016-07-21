package com.hello.suripu.core.speech;

/**
 * Created by ksg on 6/29/16
 */
public class Intention {
    public enum IntentType {
        NONE(0),
        SLEEP_SOUNDS(1),
        ALARM(2),
        SLEEP_REPORT(3),
        ROOM_CONDITIONS(4);

        protected int value;

        IntentType(final int value) { this.value = value; }
        public final int getValue() { return this.value;}

        public static IntentType fromInteger(final int value) {
            for (final IntentType intent : IntentType.values()) {
                if (value == intent.value)
                    return intent;
            }
            throw new IllegalArgumentException("Invalid Intent integer");
        }

        public static IntentType fromString(final String text) {
            if (text != null) {
                for (final IntentType intent : IntentType.values()) {
                    if (text.equalsIgnoreCase(intent.toString()))
                        return intent;
                }
            }
            throw new IllegalArgumentException("Invalid Intent string");
        }
    }

    public enum ActionType {
        NONE(0),
        PLAY_SOUND(1),
        STOP_SOUND(2),
        GET_CONDITION(3),
        GET_SENSOR(4);

        protected int value;

        ActionType(final int value) { this.value = value; }
        public final int getValue() { return this.value;}

        public static ActionType fromInteger(final int value) {
            for (final ActionType action : ActionType.values()) {
                if (value == action.value)
                    return action;
            }
            throw new IllegalArgumentException("Invalid ActionType integer");
        }

        public static ActionType fromString(final String text) {
            if (text != null) {
                for (final ActionType action : ActionType.values()) {
                    if (text.equalsIgnoreCase(action.toString()))
                        return action;
                }
            }
            throw new IllegalArgumentException("Invalid ActionType string");
        }

    }

    public enum IntentCategory {
        NONE(0),
        DEFAULT(1),
        SOUND_NAME(2),
        SOUND_DURATION(3),
        SOUND_NAME_DURATION(4),
        OVERALL_CONDITIONS(5),
        TEMPERATURE(6),
        HUMIDITY(7),
        LIGHT(8),
        AIR_QUALITY(9),
        SOUND(10);

        protected int value;

        IntentCategory(final int value) { this.value = value; }
        public final int getValue() { return this.value;}

        public static IntentCategory fromInteger(final int value) {
            for (final IntentCategory category : IntentCategory.values()) {
                if (value == category.value)
                    return category;
            }
            throw new IllegalArgumentException("Invalid IntentCategory integer");
        }

        public static IntentCategory fromString(final String text) {
            if (text != null) {
                for (final IntentCategory category : IntentCategory.values()) {
                    if (text.equalsIgnoreCase(category.toString()))
                        return category;
                }
            }
            throw new IllegalArgumentException("Invalid IntentCategory string");
        }
    }
}
