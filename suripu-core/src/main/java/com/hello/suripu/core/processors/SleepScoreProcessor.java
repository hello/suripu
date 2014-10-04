package com.hello.suripu.core.processors;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.SleepLabelDAO;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.AggregateScore;
import com.hello.suripu.core.util.DateTimeUtil;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kingshy on 10/3/14.
 */

public class SleepScoreProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepScoreProcessor.class);

    @Timed
    public static List<AggregateScore> getSleepScore(final Long accountId, final DateTime targetDate, final int days,
                                final TrackerMotionDAO trackerMotionDAO,
                                final SleepLabelDAO sleepLabelDAO,
                                final SleepScoreDAO sleepScoreDAO,
                                final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB,
                                final int dateBucketPeriod,
                                final String version) {

        // try getting scores from dynamoDB
        final ImmutableList<AggregateScore> dynamoScores = aggregateSleepScoreDAODynamoDB.getBatchScores(
                accountId,
                DateTimeUtil.dateToYmdString(targetDate.minusDays(days - 1)),
                DateTimeUtil.dateToYmdString(targetDate),
                days);

        if (dynamoScores.size() == days) {
            LOGGER.debug("All scores in dynamo");
            return dynamoScores; // found all scores
        }

        // scores not available for all days, recompute from raw data


        if (dynamoScores.size() > 0) {
            LOGGER.debug("Some scores in DynamoDB {}", dynamoScores.size());
            final List<AggregateScore> scores = new ArrayList<>();
            final List<String> retrievedDates = new ArrayList<>();
            for (final AggregateScore score : dynamoScores) {
                retrievedDates.add(score.date); // track which dates have scores
                scores.add(score);
            }

            for (int i = 0; i < days; i++) {
                final DateTime nightDate = targetDate.minusDays(i);
                final String nightDateString = DateTimeUtil.dateToYmdString(nightDate);

                if (retrievedDates.contains(nightDateString)) {
                    continue;
                }

                LOGGER.debug("get {} score from raw data", nightDateString);

                final AggregateScore score = sleepScoreDAO.getSingleSleepScore(accountId, nightDate,
                        dateBucketPeriod, trackerMotionDAO, sleepLabelDAO, version);

                // save scores to DynamoDB, but not for the current day
                if (!nightDate.equals(targetDate)) {
                    LOGGER.debug("write computed score to dynamo {}, {}", score.date, score.score);
                    aggregateSleepScoreDAODynamoDB.writeSingleScore(score);
                }
                scores.add(score);
            }

            // TODO: return sorted list of scores??
            return scores;
        }

        // no scores in dynamoDB, recompute from raw data
        LOGGER.debug("No scores in Dynamo, recompute!");
        final List<AggregateScore> scores = sleepScoreDAO.getSleepScores(accountId, targetDate, days, dateBucketPeriod, trackerMotionDAO, sleepLabelDAO, version);

        final List<AggregateScore> saveScores = new ArrayList<>();
        final String targetDateString = DateTimeUtil.dateToYmdString(targetDate);
        for (final AggregateScore score : scores) {
            if (!score.date.equals(targetDateString)) {
                saveScores.add(score);
            }
        }

        LOGGER.debug("write recomputed score to DB");
        aggregateSleepScoreDAODynamoDB.writeBatchScores(saveScores);

        return scores;
    }
}