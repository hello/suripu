package com.hello.suripu.service.registration;

public class FailedToSignException extends RuntimeException {

    public FailedToSignException(final String message) {
        super(message);
    }
}
