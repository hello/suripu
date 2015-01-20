package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.hello.suripu.api.output.OutputProtos;
import org.joda.time.DateTimeZone;

import java.util.Collections;
import java.util.List;

/**
 * Created by pangwu on 9/25/14.
 */
public class UserInfo {
    public final String deviceId;
    public final long accountId;
    public final List<Alarm> alarmList;
    public final Optional<RingTime> ringTime;
    public final Optional<DateTimeZone> timeZone;
    public final Optional<OutputProtos.SyncResponse.PillSettings> pillColor;
    public final long lastUpdatedAt;


    public UserInfo(final String deviceId, final long accountId,
                    final List<Alarm> alarmList,
                    final Optional<RingTime> ringTime,
                    final Optional<DateTimeZone> timeZone,
                    final Optional<OutputProtos.SyncResponse.PillSettings> pillColor,
                    final long lastUpdatedAt){
        if(deviceId == null || alarmList == null || timeZone == null || ringTime == null){
            throw new IllegalArgumentException("Device Id can't be null");
        }

        this.deviceId = deviceId;
        this.accountId = accountId;
        this.alarmList = alarmList;
        this.ringTime  = ringTime;
        this.timeZone = timeZone;
        this.pillColor = pillColor;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public static UserInfo createEmpty(final String deviceId, final long accountId){


        return new UserInfo(deviceId, accountId,
                Collections.<Alarm>emptyList(),
                Optional.of(RingTime.createEmpty()),
                Optional.<DateTimeZone>absent(),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                0);
    }

    public boolean isEmpty(){
        return this.alarmList.size() == 0 &&
                this.ringTime.isPresent() == false &&
                this.timeZone.isPresent() == false &&
                this.pillColor.isPresent() == false;
    }
}
