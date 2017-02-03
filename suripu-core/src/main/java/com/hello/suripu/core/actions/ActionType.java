package com.hello.suripu.core.actions;

/**
 * Created by ksg on 1/23/17
 */
public enum ActionType {
    LOGIN("login"),
    TRENDS("trends");

    private final String value;
    ActionType(final String value) {
        this.value = value;
    }

    public String string() { return value;}
}
