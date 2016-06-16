package com.hello.suripu.core.preferences;

/**
 * Created by jyfan on 6/16/16.
 */
public enum TimeFormat {
    CIVILIAN("12"),
    MILITARY("24");

    private String value;

    TimeFormat(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
