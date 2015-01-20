package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.binders.BindSleepScore;
import com.hello.suripu.core.db.mappers.SleepScoreMapper;
import com.hello.suripu.core.models.AggregateScore;
import com.hello.suripu.core.models.SleepLabel;
import com.hello.suripu.core.models.SleepScore;
import com.hello.suripu.core.util.DateTimeUtil;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SleepScoreDAO {

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
            "date_bucket_utc >= :sleep_utc AND date_bucket_utc < :awake_utc ORDER BY data_bucket_utc")
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
    public int getSleepScoreForNight(final Long accountID, final DateTime nightDate, final Integer userOffsetMillis, final int dateBucketPeriod, final SleepLabelDAO sleepLabelDAO) {

        // get sleep and wakeup time from sleep_labels or use default
        LOGGER.debug("Score for night of : {}", nightDate);

        final DateTime startDate = nightDate.withTimeAtStartOfDay().minusHours(5);
        final DateTime endDate = nightDate.withTimeAtStartOfDay().plusDays(1);
        final Optional<SleepLabel> sleepLabelOptional = sleepLabelDAO.getByAccountAndDate(accountID, startDate, endDate);
        DateTime sleepUTC, wakeUTC;
        if (sleepLabelOptional.isPresent()) {
            sleepUTC = sleepLabelOptional.get().sleepTimeUTC;
            wakeUTC = sleepLabelOptional.get().wakeUpTimeUTC;
        } else {
            if (userOffsetMillis == null) {
                LOGGER.warn("No sleep label and no offset for this night {} for account_id = {}", nightDate, accountID);
                return 0;
            }
            sleepUTC = nightDate.withHourOfDay(22).minusMillis(userOffsetMillis); // default starts at 10pm
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
        for (final SleepScore score : scores) {
            totalScore += score.bucketScore;
        }
        final float score = 100.0f - (totalScore / (float) scores.size());
        LOGGER.debug("TOTAL score: {}, {}", String.valueOf(totalScore), score);
        LOGGER.debug("Night {} Start: {}, end {}", nightDate, scores.get(0).dateBucketUTC, scores.get(scores.size()-1).dateBucketUTC);

        return Math.round((score / 100.0f) * this.SCORE_RANGE + this.SCORE_MIN);
    }

    @Timed
    public List<AggregateScore> getSleepScoreForNights(final Long accountID,
                                               final List<DateTime> requiredDates,
                                               final int dateBucketPeriod,
                                               final TrackerMotionDAO trackerMotionDAO,
                                               final SleepLabelDAO sleepLabelDAO,
                                               final String version) {

        Collections.sort(requiredDates);

        // get timezone offsets for those dates from tracker-motion
        final Map<DateTime, Integer> userOffsets = trackerMotionDAO.getOffsetMillisForDates(accountID, requiredDates);

        // get sleep labels for sleep & wakeup times
        final DateTime startDate = requiredDates.get(0);
        final DateTime endDate = requiredDates.get(requiredDates.size() - 1).plusDays(2);

        ImmutableList<SleepLabel> sleepLabels = sleepLabelDAO.getByAccountAndDates(accountID, startDate, endDate);
        final Map<DateTime, SleepLabel> sleepWakeTimes = new HashMap<>();
        for (final SleepLabel label : sleepLabels) {
            sleepWakeTimes.put(label.dateUTC.withTimeAtStartOfDay(), label);
        }

        // get scores
        final List<SleepScore> scores = this.getByAccountBetweenDateBucket(accountID, startDate.minusDays(1), endDate.plusDays(2));

        int scoresIndex = 0;
        final int totalNumScores = scores.size();
        final List<AggregateScore> finalScores = new ArrayList<>();

        for (final DateTime date : requiredDates) {

            if (scores.size() == 0) {
                // return scores = 0 if we don't have anything
                finalScores.add(new AggregateScore(accountID, 0, DateTimeUtil.dateToYmdString(date), this.SCORE_TYPE, version));
                continue;
            }

            DateTime sleepUTC, wakeUTC;
            if (sleepWakeTimes.containsKey(date)) {
                sleepUTC = sleepWakeTimes.get(date).sleepTimeUTC;
                wakeUTC = sleepWakeTimes.get(date).wakeUpTimeUTC;
            } else {

                if (!userOffsets.containsKey(date)) {
                    // no score if we don't have offsets or sleep-wake times
                    LOGGER.warn("No offset for account {} on night {}", accountID, date);
                    finalScores.add(new AggregateScore(accountID, 0, DateTimeUtil.dateToYmdString(date), this.SCORE_TYPE, version));
                    continue;
                }
                sleepUTC = date.withHourOfDay(22).minusMillis(userOffsets.get(date));
                wakeUTC = sleepUTC.plusHours(12);
            }

            // sleep = 1:08am, query starts at 1:00am
            // wake = 7:55am, query ends at 8:00am
            final int sleepMinute = (sleepUTC.getMinuteOfHour() / dateBucketPeriod) * dateBucketPeriod;
            final int wakeMinutes = ((wakeUTC.getMinuteOfHour() / dateBucketPeriod) + 1) * dateBucketPeriod;

            sleepUTC = sleepUTC.withMinuteOfHour(sleepMinute);
            wakeUTC = wakeUTC.withMinuteOfHour(0).plusMinutes(wakeMinutes);
            LOGGER.debug("SleepUTC: {}, WakeUTC: {}", sleepUTC, wakeUTC);

            float totalScore = 0.0f;
            float scoreSize = 0.0f;

            for (int i = scoresIndex; i < totalNumScores; i++) {
                final SleepScore score = scores.get(i);
                if (score.dateBucketUTC.isBefore(sleepUTC)) {
                    // skipping over scores that are not in sleep-wake range
                    continue;
                }

                if (scoreSize < 1.0f) {
                    LOGGER.debug("Day {} start_bucket: {}", date, score.dateBucketUTC);
                }

                if (score.dateBucketUTC.isAfter(wakeUTC)) {
                    scoresIndex = i;
                    break;
                }
                totalScore += score.bucketScore;
                scoreSize++;
            }

            int finalScore = 0;
            if (scoreSize > 0.0f) {
                LOGGER.debug("Day {} end_bucket: {}", date, scores.get(scoresIndex - 1).dateBucketUTC);
                final float tmpScore = (100.0f - (totalScore / scoreSize)) / 100.0f;
                finalScore = Math.round((tmpScore * this.SCORE_RANGE) + this.SCORE_MIN);
            }

            finalScores.add(new AggregateScore(accountID, finalScore,
                    DateTimeUtil.dateToYmdString(date),
                    this.SCORE_TYPE, version));
        }

        return finalScores;
    }

}
