package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.binders.BindSleepScore;
import com.hello.suripu.core.db.mappers.SleepScoreMapper;
import com.hello.suripu.core.models.AggregateScore;
import com.hello.suripu.core.models.SleepLabel;
import com.hello.suripu.core.models.SleepScore;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.TimelineUtils;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SleepScoreDAO  {

    private static final Logger LOGGER = LoggerFactory.getLogger(SleepScoreDAO.class);
    private static final Pattern PG_UNIQ_PATTERN = Pattern.compile("ERROR: duplicate key value violates unique constraint \"(\\w+)\"");

    private static float SCORE_MIN = 10.0f;
    private static float SCORE_RANGE = 80.0f; // max score is 90

    private static String SCORE_TYPE = "sleep";

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
                    // TODO: decide what to do when updating row fails
                }
            }

        }
        final Map<String, Integer> stats = new HashMap<>();
        stats.put("total", scores.size());
        stats.put("updated", numUpdates);
        stats.put("inserted", numInserts);

        return stats;
    }

    @Timed
    public int getSleepScoreForNight(final Long accountID, final DateTime nightDate, final int offsetMillis, final int dateBucketPeriod, final SleepLabelDAO sleepLabelDAO) {

        // get sleep and wakeup time from sleep_labels or use default
        final DateTimeZone userLocalTimeZone = DateTimeZone.forOffsetMillis(offsetMillis);
        final DateTime userLocalDateTime = new DateTime(nightDate.getYear(),
                nightDate.getMonthOfYear(),
                nightDate.getDayOfMonth(), 0, 0,
                userLocalTimeZone).withTimeAtStartOfDay();
        final DateTime roundedUserLocalTimeInUTC = new DateTime(userLocalDateTime.getMillis(), DateTimeZone.UTC);
        LOGGER.debug("Score for night of : {}, {}, {}", nightDate, userLocalDateTime, roundedUserLocalTimeInUTC);

        final Optional<SleepLabel> sleepLabelOptional = sleepLabelDAO.getByAccountAndDate(accountID, roundedUserLocalTimeInUTC, offsetMillis);
        DateTime sleepUTC, wakeUTC;
        if (sleepLabelOptional.isPresent()) {
            sleepUTC = sleepLabelOptional.get().sleepTimeUTC;
            wakeUTC = sleepLabelOptional.get().wakeUpTimeUTC;
        } else {
            sleepUTC = nightDate.withHourOfDay(22); // default starts at 10pm
            wakeUTC = sleepUTC.plusHours(12); // next morning 10am
        }
        LOGGER.debug("User {} sleeps at {}, wakes at {}", accountID, sleepUTC, wakeUTC);

        // set minute values to datetime bucket boundaries
        // sleep = 1:08am, query starts at 1:00am
        // wake = 7:55am, query ends at 8:00am
        final int sleepMinute = (sleepUTC.getMinuteOfHour() / dateBucketPeriod) * dateBucketPeriod;
        final int wakeMinutes = ((wakeUTC.getMinuteOfHour() / dateBucketPeriod) + 1) * dateBucketPeriod;
        final List<SleepScore> scores = this.getByAccountBetweenDateBucket(accountID,
                sleepUTC.withMinuteOfHour(sleepMinute),
                wakeUTC.withMinuteOfHour(0).plusMinutes(wakeMinutes));
        LOGGER.debug("Length of scores: {}", scores.size());

        if (scores.size() == 0) {
            // for now, shouldn't happen IRL
            LOGGER.warn("No scores for this night {} for account_id = {}", nightDate, accountID);
//            return  new Random().nextInt(100);
            return 0;
        }

        // TODO: continue to work on actual scoring
        float totalScore = 0.0f;
        for (final SleepScore score: scores) {
            totalScore += score.bucketScore;
        }
        final float score =  100.0f - (totalScore / (float) scores.size());
        LOGGER.debug("TOTAL score: {}, {}", String.valueOf(totalScore), score);

        return Math.round((score / 100.0f) * this.SCORE_RANGE + this.SCORE_MIN);
    }

    @Timed
    public List<AggregateScore> getSleepScores(final Long accountID, final DateTime endDate, final int numDays,
                                               final int dateBucketPeriod,
                                               final TrackerMotionDAO trackerMotionDAO,
                                               final SleepLabelDAO sleepLabelDAO,
                                               final String version) {

        final List<AggregateScore> scores = new ArrayList<>();

        // TODO: check if we already have scores in persistent storage. If not, compute and save.

        final int groupBy = 5; // group by 15 mins
        final int threshold = 10;

        for (int i = 0; i < numDays; i++) {
            final DateTime targetDate = endDate.minusDays(i).withTimeAtStartOfDay();
            final DateTime queryStartDate = targetDate.withHourOfDay(22);
            final DateTime queryEndDate = queryStartDate.plusHours(12);

            LOGGER.debug("Dates {}, {}, {}", targetDate, queryEndDate, queryStartDate);

            final List<TrackerMotion> trackerMotions = trackerMotionDAO.getBetweenGrouped(accountID, queryStartDate, queryEndDate, groupBy);

            LOGGER.debug("tracker motion size {}", trackerMotions.size());

            Integer score = 0;
            String message = "You haven't been sleeping";

            if (trackerMotions.size() > 0) {
                final int offsetMillis = trackerMotions.get(0).offsetMillis;
                score = this.getSleepScoreForNight(accountID, targetDate, offsetMillis, dateBucketPeriod, sleepLabelDAO);

                // TODO: find a better way, do these just to get message....
                final List<SleepSegment> segments = TimelineUtils.generateSleepSegments(trackerMotions, threshold, groupBy);
                final List<SleepSegment> normalized = TimelineUtils.categorizeSleepDepth(segments);
                final List<SleepSegment> mergedSegments = TimelineUtils.mergeConsecutiveSleepSegments(normalized, threshold);
                final SleepStats sleepStats = TimelineUtils.computeStats(mergedSegments);
                message = TimelineUtils.generateMessage(sleepStats);
            }

            final String dateString = DateTimeUtil.dateToYmdString(targetDate);
            final AggregateScore aggregateScore = new AggregateScore(accountID, score, message, dateString, this.SCORE_TYPE, version);

            // TODO: save aggregateScore to DynamoDB

            scores.add(aggregateScore);
        }

        return scores;
    }
}
