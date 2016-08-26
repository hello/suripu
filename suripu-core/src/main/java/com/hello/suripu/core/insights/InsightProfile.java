package com.hello.suripu.core.insights;

import com.hello.suripu.core.models.DeviceAccountPair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class InsightProfile {

    private final DeviceAccountPair sense;
    private final DeviceAccountPair pill;
    private final DateTime utcNow;
    private final DateTime accountCreated;

    public InsightProfile(final DeviceAccountPair sense, final DeviceAccountPair pill, final DateTime utcNow, final DateTimeZone dateTimeZone, final DateTime accountCreated) {
        this.sense = sense;
        this.pill = pill;
        this.utcNow = utcNow;
        this.accountCreated = accountCreated;
    }

    public DateTime utcnow() {
        return utcNow;
    }

    public DeviceAccountPair sense() {
        return sense;
    }

    public DeviceAccountPair pill() {
        return pill;
    }

    public DateTimeZone timeZone() {
        return null;
    }

    public DateTime accountCreated() {
        return accountCreated;
    }

    public Long accountId() {
        return pill.accountId;
    }
}
