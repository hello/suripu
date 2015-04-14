package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.TimelineLog;
import org.joda.time.DateTime;


/**
 * Created by benjo on 4/6/15.
 */
public interface TimelineLogDAO {

    public ImmutableList<TimelineLog> getLogsForUserAndDay(long accountId, DateTime day,Optional<Integer> numDaysAfterday);

    public boolean putTimelineLog(final long accountId,final TimelineLog logdata);
}
