package com.hello.suripu.core.trends.v2;


public enum TimeScale {

    LAST_WEEK("LAST_WEEK") {
        public final int getDays() {
            return 7;
        }
    },

    LAST_MONTH("LAST_MONTH") {
        public final int getDays() {
            return 30;
        }
    },

    LAST_THREE_MONTHS("LAST_3_MONTHS") {
        public final int getDays() {
            return 90;
        }
    };

    private String value;

    private TimeScale(final String value) { this.value = value; }

    public String getValue() { return this.value; }

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

    abstract public int getDays();

}