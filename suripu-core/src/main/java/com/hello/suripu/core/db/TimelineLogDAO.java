package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;


/**
 * Created by benjo on 4/6/15.
 */
public interface TimelineLogDAO {

    static public class TimelineLog {
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

    public ImmutableList<TimelineLog> getLogsForUserAndDay(long accountId, DateTime day,Optional<Integer> numDaysAfterday);

    public boolean putTimelineLog(final TimelineLog logdata);
}
