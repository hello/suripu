package com.hello.suripu.core.swap;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

;

public class Result {

    private final boolean successful;
    private final Optional<Error> error;

    public enum Error {
        OLD_SENSE_NOT_PAIRED_TO_CURRENT_ACCOUNT,
        ALREADY_PAIRED,
        SOMETHING_ELSE;
    }

    public Result(boolean successful, Optional<Error> error) {
        this.successful = successful;
        this.error = error;
    }

    public static Result failed(Error error) {
        return new Result(false, Optional.of(error));
    }

    public static Result success() {
        return new Result(true, Optional.<Error>absent());
    }


    public boolean successful() {
        return successful;
    }

    public Optional<Error> error() {
        return error;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(Result.class)
                .add("successful", successful)
                .add("error", error)
                .toString();
    }
}
