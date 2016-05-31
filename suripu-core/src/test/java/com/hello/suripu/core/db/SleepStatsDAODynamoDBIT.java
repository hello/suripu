package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.SleepScore;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by ksg on 5/24/16
 */
public class SleepStatsDAODynamoDBIT extends DynamoDBIT<SleepStatsDAODynamoDB> {

    @Override
    protected CreateTableResult createTable() {
        return dao.createTable(2L, 2L);
    }

    @Override
    protected SleepStatsDAODynamoDB createDAO() {
        return new SleepStatsDAODynamoDB(amazonDynamoDBClient, TABLE_NAME, "1");
    }

    @Test
    public void testBatchGetByDates() throws Exception {

        // insert data for the past 600 days for the past 2 years
        final Long accoundId = 1L;
        final DateTime dateTime = new DateTime(DateTimeZone.UTC).withYear(2016).withMonthOfYear(5).withDayOfMonth(31).withTimeAtStartOfDay();
        final int overallSleepScore = 90;

        for (int i = 0; i < 600; i++) {

            final MotionScore motionScore = new MotionScore(10, 20, 30.0f, 40, 80);
            final SleepScore sleepScore = new SleepScore(90, motionScore, 90, 10, 1);
            final SleepStats stats = new SleepStats(100, 200, 300, 600, false, 19, 1L, 2L, 3);
            final int offsetMillis = -288000;

            final Boolean insertResult = dao.updateStat(accoundId, dateTime.minusDays(i), Math.max(overallSleepScore - i, 10), sleepScore, stats, offsetMillis);

            assertThat(insertResult, is(true));
        }

        final DateTime zeroDate = dateTime.minusDays(300);
        final int zeroScore = 10;
        final DateTime firstDate = dateTime.minusDays(6);
        final int firstScore = overallSleepScore - 6;
        final DateTime secondDate = dateTime.minusDays(3);
        final int secondScore = overallSleepScore - 3;

        final Set<String> datesToGet = Sets.newHashSet();
        datesToGet.add(DateTimeUtil.dateToYmdString(zeroDate));
        datesToGet.add(DateTimeUtil.dateToYmdString(firstDate));
        datesToGet.add(DateTimeUtil.dateToYmdString(secondDate));

        final ImmutableList<AggregateSleepStats> results = dao.getBatchStatsFilterByDates(accoundId,
                DateTimeUtil.dateToYmdString(dateTime.minusDays(400)),
                DateTimeUtil.dateToYmdString(dateTime), datesToGet);
        assertThat(results.size(), is(3));

        assertThat(results.get(0).dateTime.equals(zeroDate), is(true));
        assertThat(results.get(0).sleepScore, is(zeroScore));

        assertThat(results.get(1).dateTime.equals(firstDate), is(true));
        assertThat(results.get(1).sleepScore, is(firstScore));

        assertThat(results.get(2).dateTime.equals(secondDate), is(true));
        assertThat(results.get(2).sleepScore, is(secondScore));

    }

}