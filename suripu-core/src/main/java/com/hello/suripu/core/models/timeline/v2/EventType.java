package com.hello.suripu.core.models.timeline.v2;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EventType {
    IN_BED(0),
    GENERIC_MOTION(2),
    PARTNER_MOTION(3),
    GENERIC_SOUND(4),
    SNORED(5),
    SLEEP_TALKED(6),
    LIGHT(7),
    LIGHTS_OUT(8),
    SUNSET(9),
    SUNRISE(10),
    GOT_IN_BED(11),
    FELL_ASLEEP(12),
    GOT_OUT_OF_BED(13),
    WOKE_UP(14),
    ALARM_RANG(15),
    UNKNOWN(16),
    SLEEP_DISTURBANCE(17),
    ;


    private static EventType[] cachedValues = null;
    public static EventType fromInt(int i) {
        if(EventType.cachedValues == null) {
            EventType.cachedValues = EventType.values();
        }
        return EventType.cachedValues[i];
    }

    public final int value;

    EventType(int value) {
        this.value = value;
    }


    @JsonCreator
    public static EventType fromString(String value) {
        if(EventType.cachedValues == null) {
            EventType.cachedValues = EventType.values();
        }
        for(final EventType type : EventType.cachedValues) {
            if(type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
