package com.hello.suripu.core.swap;

import com.google.common.base.Optional;

public class IntentResult {

    final private Optional<Intent> swapIntent;
    final private Status status;

    private IntentResult(final Optional<Intent> swapIntent, final Status status) {
        this.swapIntent = swapIntent;
        this.status = status;
    }

    public static IntentResult ok(final Intent intent) {
        return new IntentResult(Optional.of(intent), Status.OK);
    }

    public static IntentResult failed(final Status status) {
        if(status == Status.OK) {
            throw new IllegalArgumentException(String.format("status invalid: %s", status));
        }
        return new IntentResult(Optional.<Intent>absent(), status);
    }

    public Optional<Intent> intent() {
        return swapIntent;
    }

    public Status status() {
        return status;
    }
}
