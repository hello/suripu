package com.hello.suripu.core.actions;

/**
 * Created by ksg on 1/25/17
 */
public enum ActionResult {
    OKAY("okay"),
    FAIL("fail"),
    NO_DATA("no_data");

    private String value;

    ActionResult(final String value) {
        this.value = value;
    }

    public String string() { return value;}
}
