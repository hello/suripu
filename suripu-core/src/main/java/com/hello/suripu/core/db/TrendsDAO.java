package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.DowSampleMapper;
import com.hello.suripu.core.models.Insights.DowSample;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kingshy on 12/17/14.
 */
public abstract class TrendsDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrendsDAO.class);

    // Sleep Score
    @RegisterMapper(DowSampleMapper.class)
    @SqlQuery("SELECT day_of_week, CAST(score_sum AS FLOAT)/ score_count AS value " +
            "FROM sleep_score_dow WHERE account_id = :account_id AND score_count > 0 ORDER BY day_of_week")
    public abstract ImmutableList<DowSample> getSleepScoreDow (@Bind("account_id") Long accountId);

//    @SqlUpdate("INSERT INTO sleep_score_dow (account_id, day_of_week, local_utc_updated) VALUES " +
//            "(:account_id, 1, :updated), (:account_id, 2, :updated), (:account_id, 3, :updated), " +
//            "(:account_id, 4, :updated), (:account_id, 5, :updated), (:account_id, 6, :updated), " +
//            "(:account_id, 7, :updated)")
//    public abstract long insertAccountScoreRows(@Bind("account_id") Long accountId,
//                                                @Bind("datetime") DateTime updated);

    @SqlUpdate("INSERT INTO sleep_score_dow " +
            "(account_id, day_of_week, score_sum, score_count, local_utc_updated) VALUES " +
            "(:account_id, :day_of_week, :score, :count, :updated)")
    public abstract long insertAccountScoreRow(@Bind("account_id") Long accountId,
                                               @Bind("day_of_week") int dayOfWeek,
                                               @Bind("score") long score,
                                               @Bind("count") int count,
                                               @Bind("datetime") DateTime updated);

    @SqlUpdate("UPDATE sleep_score_dow SET score_sum = score_sum + :score, " +
            "score_count = score_count + 1, local_utc_updated = :updated " +
            "WHERE account_id = :account_id AND day_of_week = :day_of_week AND " +
            "local_utc_updated < :updated")
    public abstract long updateAccountScoreDayOfWeek(@Bind("account_id") Long accountId,
                                                     @Bind("score") Long score,
                                                     @Bind("day_of_week") int dayOfWeek,
                                                     @Bind("updated") DateTime updated);


    // Sleep Duration
    @RegisterMapper(DowSampleMapper.class)
    @SqlQuery("SELECT day_of_week, CAST(duration_sum AS FLOAT)/ duration_count AS value " +
            "FROM sleep_duration_dow WHERE account_id = :account_id AND duration_count > 0 ORDER BY day_of_week")
    public abstract ImmutableList<DowSample> getSleepDurationDow (@Bind("account_id") Long accountId);


//    @SqlUpdate("INSERT INTO sleep_duration_dow (account_id, day_of_week, local_utc_updated) VALUES " +
//            "(:account_id, 1, :updated), (:account_id, 2, :updated), (:account_id, 3, :updated), " +
//            "(:account_id, 4, :updated), (:account_id, 5, :updated), (:account_id, 6, :updated), " +
//            "(:account_id, 7, :updated)")
//    public abstract long insertAccountDurationRows(@Bind("account_id") Long accountId,
//                                                   @Bind("datetime") DateTime updated);

    @SqlUpdate("INSERT INTO sleep_duration_dow " +
            "(account_id, day_of_week, duration_sum, duration_count, local_utc_updated) VALUES " +
            "(:account_id, :day_of_week, :duration, :count, :updated)")
    public abstract long insertAccountDurationRow(@Bind("account_id") Long accountId,
                                                  @Bind("day_of_week") int dayOfWeek,
                                                  @Bind("duration") long duration,
                                                  @Bind("count") int count,
                                                  @Bind("datetime") DateTime updated);

    @SqlUpdate("UPDATE sleep_duration_dow SET duration_sum = duration_sum + :duration, " +
            "duration_count = duration_count + 1, local_utc_updated = :updated " +
            "WHERE account_id = :account_id AND day_of_week = :day_of_week AND " +
            "local_utc_updated < :updated")
    public abstract long updateAccountDurationDayOfWeek(@Bind("account_id") Long accountId,
                                                        @Bind("duration") Long duration,
                                                        @Bind("day_of_week") int dayOfWeek,
                                                        @Bind("updated") DateTime updated);


    public long updateDayOfWeekScore(final long accountId, final long score, final int offsetMillis) {
        final DateTime updated = new DateTime(DateTime.now(), DateTimeZone.UTC).plusMillis(offsetMillis).withTimeAtStartOfDay();
        final int dayOfWeek = updated.getDayOfWeek();

        // update row
        long rowCount = this.updateAccountScoreDayOfWeek(accountId, score, dayOfWeek, updated);

        if (rowCount > 0L) {
            return rowCount;
        }

        // not updated, row may not exist
        try {
            final long insertCount = this.insertAccountScoreRow(accountId, dayOfWeek, score, 1, updated);
            return insertCount;

        } catch (UnableToExecuteStatementException exception) {
            LOGGER.warn("Cannot insert score_dow for account {}, day of week {}", accountId, dayOfWeek);
            LOGGER.warn("sleep_score_dow insert fail: {}", exception.getMessage());
            return 0L;
        }
    }

    public long updateDayOfWeekDuration(final long accountId, final long duration, final int offsetMillis) {
        final DateTime updated = new DateTime(DateTime.now(), DateTimeZone.UTC).plusMillis(offsetMillis).withTimeAtStartOfDay();
        final int dayOfWeek = updated.getDayOfWeek();

        // update row
        long rowCount = this.updateAccountDurationDayOfWeek(accountId, duration, dayOfWeek, updated);
        if (rowCount > 0L) {
            return rowCount;
        }

        // not updated, row may not exist, try inserting
        try {
            final long insertCount = this.insertAccountDurationRow(accountId, dayOfWeek, duration, 1, updated);
            return insertCount;

        } catch (UnableToExecuteStatementException exception) {
            LOGGER.warn("Cannot insert duration for account {}, day of week {}", accountId, dayOfWeek);
            LOGGER.warn("sleep_duration_dow insert fail: {}", exception.getMessage());
            return 0L;
        }
    }

}
