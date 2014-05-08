package com.hello.suripu.core;

import org.joda.time.DateTime;

/**
 * Created by pangwu on 5/6/14.
 */
public class TrackerMotion extends SensorSample {
    public static final float FLOAT_TO_INT_CONVERTER = 10000000;

    public final long id;
    public final long accountId;
    public final String trackerId;

    public TrackerMotion(final long id,
                         final long accountId,
                         final String trackerId,
                         final DateTime dateTimeInUTC,
                         final int value,
                         final int timeZoneOffset){
        super(dateTimeInUTC, value / FLOAT_TO_INT_CONVERTER, timeZoneOffset);
        this.id = id;
        this.accountId = accountId;
        this.trackerId = trackerId;

    }
}
