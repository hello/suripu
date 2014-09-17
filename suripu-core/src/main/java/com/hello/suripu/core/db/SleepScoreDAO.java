package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.binders.BindSleepScore;
import com.hello.suripu.core.db.mappers.SleepScoreMapper;
import com.hello.suripu.core.models.SleepScore;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SleepScoreDAO  {

    private static final Logger LOGGER = LoggerFactory.getLogger(SleepScoreDAO.class);
    private static final Pattern PG_UNIQ_PATTERN = Pattern.compile("ERROR: duplicate key value violates unique constraint \"(\\w+)\"");

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO sleep_score " +
            "(account_id, date_bucket_utc, pill_id, offset_millis, sleep_duration, custom, " +
            "bucket_score, agitation_num, agitation_tot, updated) " +
            "VALUES(:account_id, :date_bucket_utc, :pill_id, :offset_millis, :sleep_duration, :custom, " +
            ":bucket_score, :agitation_num, :agitation_tot, :updated)")
    public abstract long insert(@BindSleepScore SleepScore sleepScore);

    @RegisterMapper(SleepScoreMapper.class)
    @SqlQuery("SELECT * FROM sleep_score " +
            "WHERE account_id = :account_id AND " +
            "date_bucket_utc >= :sleep_utc AND date_bucket_utc < :awake_utc")
    public abstract ImmutableList<SleepScore> getByAccountBetweenDateBucket(@Bind("account_id") Long account_id,
                                                 @Bind("sleep_utc") DateTime sleepUTC,
                                                 @Bind("awake_utc") DateTime awakeUTC);

    @SqlUpdate("UPDATE sleep_score " +
            "SET bucket_score = :bucket_score " +
            "sleep_duration = :sleep_duration, " +
            "agitation_num = :agitation_num, " +
            "agitation_tot = :agitation_tot " +
            "WHERE pill_id = :pill_id AND date_bucket_utc = :date_bucket_utc")
    public abstract long updateSleepScoreByDateBucket(@Bind("pill_id") long pillID,
                                      @Bind("bucket_score") int bucketScore,
                                      @Bind("sleep_duration") int sleepDuration,
                                      @Bind("agitation_num") int agitationNum,
                                      @Bind("agitation_tot") long agitationTot,
                                      @Bind("date_bucket_utc") DateTime dateBucketUTC);

    @SqlUpdate("UPDATE sleep_score SET " +
            "bucket_score = bucket_score + :bucket_score, sleep_duration = sleep_duration + :sleep_duration, " +
            "agitation_num = agitation_num + :agitation_num, " +
            "agitation_tot = agitation_tot + :agitation_tot, updated = :updated " +
            "WHERE pill_id = :pill_id AND date_bucket_utc = :date_bucket_utc")
    public abstract long incrementSleepScoreByPillDateBucket(
            @Bind("pill_id") Long pillID,
            @Bind("date_bucket_utc") DateTime dateBucketUTC,
            @Bind("bucket_score") int bucketScore,
            @Bind("sleep_duration") int sleepDuration,
            @Bind("agitation_num") int agitationNum,
            @Bind("agitation_tot") long agitationTot,
            @Bind("updated") DateTime updated);


    @Timed
    public Map<String, Integer> saveScores(final List<SleepScore> scores) {
        int numInserts = 0;
        int numUpdates = 0;
        for (final SleepScore score : scores) {
            LOGGER.debug("Saving Score: {}", score.toString());

            try {
                // try inserting first as this should be more common
                final long rowID = this.insert(score);
                if (rowID > 0) {
                    LOGGER.debug("INSERTED");
                    numInserts++;
                }
            } catch (UnableToExecuteStatementException exception) {
                Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
                if (matcher.find()) {
                    // row  exist, try updating
                    LOGGER.debug("Score exist, try updating");
                    final long updated = this.incrementSleepScoreByPillDateBucket(
                            score.pillID,
                            score.dateBucketUTC,
                            score.bucketScore,
                            score.sleepDuration,
                            score.agitationNum,
                            score.agitationTot,
                            score.updated
                    );
                    if (updated > 0) {
                        LOGGER.debug("UPDATED");
                        numUpdates++;
                    }
                } else {
                    LOGGER.error(exception.getMessage());
                }
            }

        }
        final Map<String, Integer> stats = new HashMap<>();
        stats.put("total", scores.size());
        stats.put("updated", numUpdates);
        stats.put("inserted", numInserts);

        return stats;
    }
}
