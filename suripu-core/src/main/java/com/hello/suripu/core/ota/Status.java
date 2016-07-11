package com.hello.suripu.core.ota;

public enum Status {

    NOT_REQUIRED,
    REQUIRED,
    RESPONSE_SENT,
    IN_PROGRESS,
    COMPLETE,
    ERROR,
    UNKNOWN,;

    public static Status fromString(final String text) {
        if (text == null) {
            return Status.UNKNOWN;
        }

        for (final Status status : Status.values()) {
            if (text.equalsIgnoreCase(status.toString())) {
                return status;
            }
        }
        throw new IllegalArgumentException(String.format("Invalid ota status value='%s'", text));
    }
}
