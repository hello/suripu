package com.hello.suripu.queue.models;

import org.joda.time.DateTime;

/**
 * Created by ksg on 3/15/16
 */

public class AccountData {
    public final long accountId;
    public final int offsetMillis;
    public final DateTime lastSeenTimestamp;

    public AccountData(final long accountId, final int offsetMillis, final DateTime lastSeenTimestamp) {
        this.accountId = accountId;
        this.offsetMillis = offsetMillis;
        this.lastSeenTimestamp = lastSeenTimestamp;
    }
}
