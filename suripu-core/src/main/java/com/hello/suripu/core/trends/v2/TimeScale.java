package com.hello.suripu.core.trends.v2;

/**
 * Created by ksg on 01/21/16
 */

public enum TimeScale {

    LAST_WEEK("LAST_WEEK", 7),
    LAST_MONTH("LAST_MONTH", 30),
    LAST_3_MONTHS("LAST_3_MONTHS", 90);

    private String value;
    private final int days;

    private TimeScale(final String value, final int days) {
        this.value = value;
        this.days = days;
    }

    public String getValue() { return this.value; }

    public int getDays() { return this.days; }

    public static TimeScale fromString(final String text) {
        if (text != null) {
            for (final TimeScale timeScale : TimeScale.values()) {
                if (text.equalsIgnoreCase(timeScale.getValue())) {
                    return timeScale;
                }
            }
        }
        throw new IllegalArgumentException("Invalid time-scale.");
    }
}