package com.hello.suripu.core.speech.models;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Created by ksg on 7/20/16
 */
public enum Result {
    NONE("none", 0),
    OK("ok", 1),
    REJECTED("rejected", 2),
    TRY_AGAIN("try again", 3),
    UNKNOWN("unknown", 4);

    private final String name;
    private final int value;

    Result(String name, int value) {
        this.name = name;
        this.value = value;
    }

    @JsonValue
    public String getName() { return this.name; }

    public int getValue() { return  this.value; }

    public static Result fromString(final String text) {
        if (text != null) {
            for (final Result result : Result.values()) {
                if (text.equalsIgnoreCase(result.toString()))
                    return result;
            }
        }
        return Result.NONE;
    }

}
