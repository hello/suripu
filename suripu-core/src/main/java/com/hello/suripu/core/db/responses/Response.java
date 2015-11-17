package com.hello.suripu.core.db.responses;

import com.google.common.base.Optional;

/**
 * Created by jakepiccolo on 11/12/15.
 */
public class Response<T> {

    public enum Status {
        SUCCESS,
        FAILURE,
        PARTIAL_RESULTS,
    }

    public final T data;
    public final Status status;
    public final Optional<? extends Exception> exception;

    public Response(final T data, final Status status, final Optional<? extends Exception> exception) {
        this.data = data;
        this.status = status;
        this.exception = exception;
    }

    public static <T> Response create(final T data, final Status status) {
        return new Response<>(data, status, Optional.<Exception>absent());
    }

    public static <T> Response create(final T data, final Status status, Exception exception) {
        return new Response<>(data, status, Optional.of(exception));
    }
}
