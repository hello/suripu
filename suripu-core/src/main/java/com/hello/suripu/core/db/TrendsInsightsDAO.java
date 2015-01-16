package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.DowSampleMapper;
import com.hello.suripu.core.db.mappers.GenericInsightCardsMapper;
import com.hello.suripu.core.db.mappers.SleepStatsSampleMapper;
import com.hello.suripu.core.db.util.MatcherPatternsDB;
import com.hello.suripu.core.models.Insights.DowSample;
import com.hello.suripu.core.models.Insights.GenericInsightCards;
import com.hello.suripu.core.models.Insights.SleepStatsSample;
import com.hello.suripu.core.models.Insights.TrendGraph;
import com.hello.suripu.core.models.SleepStats;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Created by kingshy on 12/17/14.
 * Assumption:
 * 1. sleep score and duration data are updated once a day, assume that it's always for the previous night
 */
public abstract class TrendsInsightsDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrendsInsightsDAO.class);
    private static final String SLEEP_DURATION_DOW_TABLE = "sleep_duration_dow";
    private static final String SLEEP_SCORE_DOW_TABLE = "sleep_score_dow";

    // Sleep Score by Day of Week
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
    public abstract int insertAccountScoreRow(@Bind("account_id") Long accountId,
                                               @Bind("day_of_week") int dayOfWeek,
                                               @Bind("score") long score,
                                               @Bind("count") int count,
                                               @Bind("updated") DateTime updated);

    @SqlUpdate("UPDATE sleep_score_dow SET score_sum = score_sum + :score, " +
            "score_count = score_count + 1, local_utc_updated = :updated " +
            "WHERE account_id = :account_id AND day_of_week = :day_of_week AND " +
            "local_utc_updated < :updated")
    public abstract int updateAccountScoreDayOfWeek(@Bind("account_id") Long accountId,
                                                     @Bind("score") long score,
                                                     @Bind("day_of_week") int dayOfWeek,
                                                     @Bind("updated") DateTime updated);


    // Sleep Duration by Day of Week
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
    public abstract int insertAccountDurationRow(@Bind("account_id") Long accountId,
                                                  @Bind("day_of_week") int dayOfWeek,
                                                  @Bind("duration") long duration,
                                                  @Bind("count") int count,
                                                  @Bind("updated") DateTime updated);

    @SqlUpdate("UPDATE sleep_duration_dow SET duration_sum = duration_sum + :duration, " +
            "duration_count = duration_count + 1, local_utc_updated = :updated " +
            "WHERE account_id = :account_id AND day_of_week = :day_of_week AND " +
            "local_utc_updated < :updated")
    public abstract int updateAccountDurationDayOfWeek(@Bind("account_id") Long accountId,
                                                        @Bind("duration") long duration,
                                                        @Bind("day_of_week") int dayOfWeek,
                                                        @Bind("updated") DateTime updated);

    // Sleep Duration Over Time

    @RegisterMapper(SleepStatsSampleMapper.class)
    @SqlQuery("SELECT * FROM sleep_stats_time WHERE account_id = :account_id ORDER BY local_utc_date")
    public abstract ImmutableList<SleepStatsSample> getAccountSleepStatsAll(@Bind("account_id") Long accountId);

    @RegisterMapper(SleepStatsSampleMapper.class)
    @SingleValueResult(SleepStatsSample.class)
    @SqlQuery("SELECT * FROM sleep_stats_time WHERE account_id = :account_id AND local_utc_date = :date")
    public abstract Optional<SleepStatsSample> getAccountSleepStatsDate(@Bind("account_id") Long accountId,
                                                                                   @Bind("date") DateTime date);

    @RegisterMapper(SleepStatsSampleMapper.class)
    @SqlQuery("SELECT * FROM sleep_stats_time WHERE account_id = :account_id AND " +
            "local_utc_date >= :start_date AND local_utc_date < :end_date ORDER BY local_utc_date")
    public abstract ImmutableList<SleepStatsSample> getAccountSleepStatsBetweenDates(@Bind("account_id") Long accountId,
                                                                                   @Bind("start_date") DateTime startDate,
                                                                                   @Bind("end_date") DateTime endDate);

    @SqlUpdate("INSERT INTO sleep_stats_time (account_id, duration, sound_sleep, light_sleep, motion, " +
            "sleep_time_utc, wake_time_utc, fall_asleep_time, " +
            "offset_millis, local_utc_date) VALUES (:account_id, :duration, :sound_sleep, :light_sleep, " +
            ":motion, :sleep_time, :wake_time, :asleep_time, :offset_millis, :local_utc_date)")
    public abstract int insertSleepStats(@Bind("account_id") Long accountId,
                                          @Bind("duration") Integer duration,
                                          @Bind("sound_sleep") Integer soundSleep,
                                          @Bind("light_sleep") Integer lightSleep,
                                          @Bind("motion") Integer motion,
                                          @Bind("sleep_time") DateTime sleepTime,
                                          @Bind("wake_time") DateTime wakeTime,
                                          @Bind("asleep_time") Integer fallAsleepTime,
                                          @Bind("offset_millis") int offsetMillis,
                                          @Bind("local_utc_date") DateTime localUTCDate);


    public int updateDayOfWeekData(final long accountId, final long dataValue, final DateTime targetDate, final int offsetMillis, final TrendGraph.DataType dataType) {
        final DateTime updated = new DateTime(DateTime.now(), DateTimeZone.UTC).plusMillis(offsetMillis).withTimeAtStartOfDay();
        final int dayOfWeek = targetDate.getDayOfWeek();

        // update row
        int rowCount;
        if (dataType == TrendGraph.DataType.SLEEP_SCORE) {
            rowCount = this.updateAccountScoreDayOfWeek(accountId, dataValue, dayOfWeek, updated);
        } else if (dataType == TrendGraph.DataType.SLEEP_DURATION) {
            rowCount = this.updateAccountDurationDayOfWeek(accountId, dataValue, dayOfWeek, updated);
        } else {
            LOGGER.warn("Invalid DataType for Trends {}, account {}", dataType, accountId);
            return 0;
        }

        if (rowCount > 0) {
            return rowCount;
        }

        // not updated, row may not exist
        try {
            int insertCount;
            if (dataType == TrendGraph.DataType.SLEEP_SCORE) {
                insertCount = this.insertAccountScoreRow(accountId, dayOfWeek, dataValue, 1, updated);
            } else {
                insertCount = this.insertAccountDurationRow(accountId, dayOfWeek, dataValue, 1, updated);
            }
            return insertCount;

        } catch (UnableToExecuteStatementException exception) {
            LOGGER.warn("Cannot insert day of week {} for account {}, day of week {}", dataType.toString(), accountId, dayOfWeek);
            LOGGER.warn("sleep_score_dow insert fail: {}", exception.getMessage());
            return 0;
        }

    }

    public int updateSleepStats(final long accountId, final int offsetMillis, final DateTime targetDate, final SleepStats stats) {
        int rowCount = 0;
        try {
             rowCount += insertSleepStats(accountId,
                     stats.sleepDurationInMinutes,
                     stats.soundSleepDurationInMinutes,
                     stats.lightSleepDurationInMinutes,
                     stats.numberOfMotionEvents,
                     new DateTime(stats.sleepTime, DateTimeZone.UTC),
                     new DateTime(stats.wakeTime, DateTimeZone.UTC),
                     stats.fallAsleepTime,
                     offsetMillis, targetDate);
            rowCount += updateDayOfWeekData(accountId, stats.sleepDurationInMinutes, targetDate, offsetMillis, TrendGraph.DataType.SLEEP_DURATION);
        } catch (UnableToExecuteStatementException exception) {
            final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(exception.getMessage());
            if (matcher.find()) {
                LOGGER.warn("Duplicate sleep stats for account {}, date {}", accountId, targetDate);
            } else {
                LOGGER.error("Cannot update sleep stats for account {}, date{}", accountId, targetDate);
            }
        }
        return rowCount;
    }

    // Insights Stuff

    @RegisterMapper(GenericInsightCardsMapper.class)
    @SqlQuery("SELECT * FROM generic_insight_cards WHERE category = :category ORDER BY id")
    public abstract List<GenericInsightCards> getGenericInsightCardsByCategory(@Bind("category") final int category);
}
