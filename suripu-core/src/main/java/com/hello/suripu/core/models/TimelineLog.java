package com.hello.suripu.core.models;

import org.joda.time.DateTime;

/**
 * Created by benjo on 4/9/15.
 */
public class TimelineLog {
    public final long accountId;
    public final String algorithm;
    public final DateTime createdDate;
    public final DateTime targetDate;
    public final String version;

    public TimelineLog(long accountId, String algorithm, DateTime createdDate, DateTime targetDate, String version) {
        this.accountId = accountId;
        this.algorithm = algorithm;
        this.createdDate = createdDate;
        this.targetDate = targetDate;
        this.version = version;
    }
}
