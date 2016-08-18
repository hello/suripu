package com.hello.suripu.core.swap;

import com.google.common.base.Optional;

;

public class SwapResult {

    private final boolean successful;
    private final Optional<Error> error;

    public enum Error {
        OLD_SENSE_NOT_PAIRED_TO_CURRENT_ACCOUNT,
        ALREADY_PAIRED,
        SOMETHING_ELSE;
    }

    public SwapResult(boolean successful, Optional<Error> error) {
        this.successful = successful;
        this.error = error;
    }

    public static SwapResult failed(Error error) {
        return new SwapResult(false, Optional.of(error));
    }

    public static SwapResult success() {
        return new SwapResult(true, Optional.<Error>absent());
    }


    public boolean successful() {
        return successful;
    }

    public Optional<Error> error() {
        return error;
    }
}
