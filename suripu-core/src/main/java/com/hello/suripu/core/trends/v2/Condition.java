package com.hello.suripu.core.trends.v2;

/**
 * Created by kingshy on 1/21/16.
 */
public enum Condition {
    UNKNOWN("UNKNOWN"),
    IDEAL("IDEAL"),
    WARNING("WARNING"),
    ALERT("ALERT");

    private String value;

    private Condition(final String value) { this.value = value; }
}