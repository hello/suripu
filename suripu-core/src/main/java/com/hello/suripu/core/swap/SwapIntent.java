package com.hello.suripu.core.swap;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class SwapIntent {

    private final String currentSenseId;
    private final String newSenseId;
    private final Long accountId;
    private final DateTime dateTime;

    private SwapIntent(final String currentSenseId, final String newSenseId, final Long accountId, final DateTime dateTime) {
        this.currentSenseId = currentSenseId;
        this.newSenseId = newSenseId;
        this.accountId = accountId;
        this.dateTime = dateTime;
    }

    public static SwapIntent create(
            final String currentSenseId,
            final String newSenseId,
            final Long accountId) {
        return new SwapIntent(currentSenseId, newSenseId, accountId, DateTime.now(DateTimeZone.UTC));
    }

    public static SwapIntent create(
            final String currentSenseId,
            final String newSenseId,
            final Long accountId,
            final DateTime dateTime) {
        return new SwapIntent(currentSenseId, newSenseId, accountId, dateTime);
    }

    public String newSenseId() {
        return newSenseId;
    }

    public String currentSenseId() {
        return currentSenseId;
    }

    public Long accountId() {
        return accountId;
    }

    public DateTime dateTime() {
        return dateTime;
    }
}
