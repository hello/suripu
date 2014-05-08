package com.hello.suripu.core;

import org.joda.time.DateTime;

/**
 * Created by pangwu on 5/6/14.
 */
public class TrackerMotionSample extends SensorSample {
    public static final float FLOAT_TO_INT_CONVERTER = 10000000;

    public final long id;
    public final long accountId;
    public final String trackerId;

    public TrackerMotionSample(final long id,
                               final long accountId,
                               final String trackerId,

                               // Convert the datetime to user local timezone, good ? bad?
                               final DateTime dateTimeInLocalTime,
                               final int value){
        super(dateTimeInLocalTime, value / FLOAT_TO_INT_CONVERTER);
        this.id = id;
        this.accountId = accountId;
        this.trackerId = trackerId;

    }
}
