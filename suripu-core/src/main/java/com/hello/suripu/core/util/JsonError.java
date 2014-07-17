package com.hello.suripu.core.util;

public class JsonError{

    public final Integer code;
    public final String message;

    public JsonError(final Integer code, final String message) {
        this.code = code;
        this.message = message;
    }
}
