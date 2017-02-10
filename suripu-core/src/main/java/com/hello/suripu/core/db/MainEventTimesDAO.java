package com.hello.suripu.core.db;

import com.hello.suripu.core.models.MainEventTimes;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by jarredheinrich on 2/7/17.
 */
public interface MainEventTimesDAO {
    public boolean updateEventTimes(MainEventTimes mainEventTimes);
    public List<MainEventTimes> getEventTimes(Long accountId, DateTime date);
}
