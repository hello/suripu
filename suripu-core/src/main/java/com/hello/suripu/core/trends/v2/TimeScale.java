package com.hello.suripu.core.trends.v2;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;

public enum TimeScale {
    LAST_WEEK("LAST_WEEK"),
    LAST_MONTH("LAST_MONTH"),
    LAST_THREE_MONTHS("LAST_3_MONTHS");

    public final static Map<TimeScale, Integer> TIMESCALE_MAP;
    static {
        final Map<TimeScale, Integer> map = Maps.newLinkedHashMap();
        map.put(TimeScale.LAST_WEEK, 7);
        map.put(TimeScale.LAST_MONTH, 30);
        map.put(TimeScale.LAST_THREE_MONTHS, 90);
        TIMESCALE_MAP = ImmutableMap.copyOf(map);
    }

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
        throw new IllegalArgumentException();
    }

}