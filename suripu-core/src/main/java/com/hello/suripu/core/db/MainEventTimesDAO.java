package com.hello.suripu.core.db;

import com.hello.suripu.core.models.MainEventTimes;
import com.hello.suripu.core.models.SleepPeriod;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * Created by jarredheinrich on 2/7/17.
 */
public interface MainEventTimesDAO {
    public boolean updateEventTimes(Long accountId, DateTime date, MainEventTimes mainEventTimes);
    public Map<SleepPeriod.Period, MainEventTimes> getEventTimes(Long accountId, DateTime date);
}
