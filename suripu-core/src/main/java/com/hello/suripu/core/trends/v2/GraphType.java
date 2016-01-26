package com.hello.suripu.core.trends.v2;

/**
 * Created by kingshy on 1/21/16.
 */
public enum GraphType {
    GRID("GRID"),
    OVERVIEW("OVERVIEW"),
    BAR("BAR"),
    BUBBLES("BUBBLES");

    private String value;

    private GraphType(final String value) {
        this.value = value;
    }
}
