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
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by kingshy on 10/3/14.
 */

public class SleepScoreProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepScoreProcessor.class);

    @Timed
    public static List<AggregateScore> getSleepScores(final Long accountId, final DateTime targetDate, final int days,
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

        // scores not available for all days, figure out which days we want data
        final List<DateTime> requiredDates = new ArrayList<>();
        final List<AggregateScore> finalScores = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            requiredDates.add(targetDate.minusDays(i));
        }

        final DateTime lastNight = new DateTime(DateTime.now(), DateTimeZone.UTC).withTimeAtStartOfDay().minusDays(1);
        if (dynamoScores.size() > 0) {
            LOGGER.debug("Some scores in DynamoDB {}", dynamoScores.size());
            for (final AggregateScore score : dynamoScores) {
                final DateTime date = DateTimeUtil.ymdStringToDateTime(score.date);
                if (!date.isEqual(lastNight) && requiredDates.contains(date)) {
                    requiredDates.remove(date);
                    finalScores.add(score);
                }

            }
        }

        // get all data from DB, filter in app
        if (requiredDates.size() > 0) {
            LOGGER.debug("Recompute {} scores out of {}", requiredDates.size(), days);
            final List<AggregateScore> scores = sleepScoreDAO.getSleepScoreForNights(accountId, requiredDates, dateBucketPeriod, trackerMotionDAO, sleepLabelDAO, version);

            final List<AggregateScore> saveScores = new ArrayList<>();
            final String lastNightDateString = DateTimeUtil.dateToYmdString(lastNight);

            for (final AggregateScore score : scores) {
                if (!score.date.equals(lastNightDateString)) {
                    saveScores.add(score);
                }
                finalScores.add(score);
                LOGGER.debug("Computed score: {}", score);
            }

            if (saveScores.size() > 0) {
                LOGGER.debug("write recomputed score to DB");
                aggregateSleepScoreDAODynamoDB.writeBatchScores(saveScores);
            }
        }

        Collections.sort(finalScores);
        return finalScores;
    }
}