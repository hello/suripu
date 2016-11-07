package com.hello.suripu.core.roomstate;

public enum Condition {
    UNKNOWN(0),
    IDEAL(1),
    WARNING(2),
    ALERT(3),
    IDEAL_EXCLUDING_LIGHT(4),
    CALIBRATING(5);

    private final int value;

    private Condition(final int value) {
        this.value = value;
    }
}
