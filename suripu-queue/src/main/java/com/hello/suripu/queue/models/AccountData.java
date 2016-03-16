package com.hello.suripu.queue.models;

import org.joda.time.DateTime;

/**
 * Created by ksg on 3/15/16
 */

public class AccountData {
    private final long accountId;
    private final int offsetMillis;
    private final DateTime lastSeenTimestamp;

    public AccountData(final long accountId, final int offsetMillis, final DateTime lastSeenTimestamp) {
        this.accountId = accountId;
        this.offsetMillis = offsetMillis;
        this.lastSeenTimestamp = lastSeenTimestamp;
    }
}
