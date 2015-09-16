package com.hello.suripu.workers.logs.timeline;

import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.db.util.MatcherPatternsDB;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;

public abstract class TimelineAnalytics {

    private final static Logger LOGGER = LoggerFactory.getLogger(TimelineAnalytics.class);

    @SqlUpdate("INSERT INTO timeline_analytics (account_id, date_of_night, algorithm, error) VALUES (:account_id, :date_of_night, :algorithm, :error);")
    abstract void insert(@BindTimelineLog LoggingProtos.TimelineLog timelineLog);

    @BatchChunkSize(100)
    @SqlBatch
    abstract void insert(@BindTimelineLog List<LoggingProtos.TimelineLog> timelineLogs);


    int insertBatchWithIndividualRetry(final List<LoggingProtos.TimelineLog> timelineLogs) {
        int i = 0;
        if(timelineLogs.isEmpty()) {
            return 0;
        }

        try {
            insert(timelineLogs);
            return timelineLogs.size();
        } catch (UnableToExecuteStatementException exception) {

            for(final LoggingProtos.TimelineLog timelineLog : timelineLogs) {

                try {
                    insert(timelineLog);
                    i++;
                } catch (UnableToExecuteStatementException e) {
                    final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(e.getMessage());
                    if(!matcher.find()) {
                        LOGGER.error(e.getMessage());
                    }
                }
            }
        }
        return i;
    }
}
