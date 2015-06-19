package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.SmartAlarmHistory;
import com.hello.suripu.core.models.TimeZoneHistory;

import java.util.List;

/**
 * Created by pangwu on 5/27/15.
 */
public class TimeHistory {
    @JsonProperty("timezone_history")
    final List<TimeZoneHistory> timeZoneHistory;

    @JsonProperty("smart_alarm_history")
    final List<SmartAlarmHistory> smartAlarmHistory;

    @JsonProperty("ringtime_history")
    final List<RingTime> ringTimeHistory;

    public TimeHistory(final List<TimeZoneHistory> timeZoneHistory,
                           final List<SmartAlarmHistory> smartAlarmHistory,
                           final List<RingTime> ringTimeHistory){
        this.timeZoneHistory = timeZoneHistory;
        this.smartAlarmHistory = smartAlarmHistory;
        this.ringTimeHistory = ringTimeHistory;
    }

}
