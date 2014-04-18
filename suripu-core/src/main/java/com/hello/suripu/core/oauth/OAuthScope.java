package com.hello.suripu.core.oauth;

public enum OAuthScope {

    USER_BASIC (0),
    USER_EXTENDED (1),
    SENSORS_BASIC(2),
    SENSORS_EXTENDED(3),
    SENSORS_WRITE(4),
    SCORE_READ(5),
    SLEEP_LABEL_BASIC(6),
    SLEEP_LABEL_WRITE(7);

    private int value;

    private OAuthScope(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
