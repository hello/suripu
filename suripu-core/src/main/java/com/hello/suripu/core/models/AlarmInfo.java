package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import org.joda.time.DateTimeZone;

import java.util.Collections;
import java.util.List;

/**
 * Created by pangwu on 9/25/14.
 */
public class AlarmInfo {
    public final String deviceId;
    public final long accountId;
    public final Optional<List<Alarm>> alarmList;
    public final Optional<RingTime> ringTime;
    public final Optional<DateTimeZone> timeZone;


    public AlarmInfo(final String deviceId, final long accountId,
                     final Optional<List<Alarm>> alarmList,
                     final Optional<RingTime> ringTime,
                     final Optional<DateTimeZone> timeZone){
        if(deviceId == null || alarmList == null || timeZone == null || ringTime == null){
            throw new IllegalArgumentException("Device Id can't be null");
        }

        this.deviceId = deviceId;
        this.accountId = accountId;
        this.alarmList = alarmList;
        this.ringTime  = ringTime;
        this.timeZone = timeZone;
    }

    public static AlarmInfo createEmpty(final String deviceId, final long accountId){


        return new AlarmInfo(deviceId, accountId,
                Optional.of(Collections.<Alarm>emptyList()),
                Optional.of(RingTime.createEmpty()),
                Optional.<DateTimeZone>absent());
    }

    public boolean isEmpty(){
        return this.alarmList.isPresent() == false && this.ringTime.isPresent() == false
                && this.timeZone.isPresent() == false;
    }
}
