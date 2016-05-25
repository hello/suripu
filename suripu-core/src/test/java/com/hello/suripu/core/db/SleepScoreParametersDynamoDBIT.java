package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.hello.suripu.core.models.SleepScoreParameters;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by ksg on 5/24/16
 */
public class SleepScoreParametersDynamoDBIT extends DynamoDBIT<SleepScoreParametersDynamoDB> {

    @Override
    protected CreateTableResult createTable() {
        return dao.createTable(2L, 2L);
    }

    @Override
    protected SleepScoreParametersDynamoDB createDAO() {
        return new SleepScoreParametersDynamoDB(amazonDynamoDBClient, TABLE_NAME);
    }

    @Test
    public void testUpdateAndGetParameters() throws Exception {
        final Long accoundId = 1L;
        final DateTime dateTime = DateTime.now(DateTimeZone.UTC);
        final SleepScoreParameters parameter = new SleepScoreParameters(accoundId, dateTime, 100);

        Boolean insertResult = dao.upsertSleepScoreParameters(accoundId, parameter);
        assertThat(insertResult, is(true));

        SleepScoreParameters retrievedParameter = dao.getSleepScoreParametersByDate(accoundId, dateTime);
        assertThat(retrievedParameter.durationThreshold, is(100));
        assertThat(retrievedParameter.accountId, is(accoundId));

        insertResult = dao.upsertSleepScoreParameters(accoundId, new SleepScoreParameters(accoundId, dateTime, 200));
        assertThat(insertResult, is(true));

        retrievedParameter = dao.getSleepScoreParametersByDate(accoundId, dateTime);
        assertThat(retrievedParameter.durationThreshold, is(200));
    }

    @Test
    public void testGetLatestParameters() throws Exception {
        final Long accoundId = 1L;
        final DateTime dateTime = DateTime.now(DateTimeZone.UTC);

        Boolean insertResult;
        insertResult = dao.upsertSleepScoreParameters(accoundId, new SleepScoreParameters(accoundId, dateTime.minusDays(2), 500));
        assertThat(insertResult, is(true));

        insertResult = dao.upsertSleepScoreParameters(accoundId, new SleepScoreParameters(accoundId, dateTime.minusDays(10), 1000));
        assertThat(insertResult, is(true));

        // this should get the 1000 threshold
        SleepScoreParameters retrievedParameter = dao.getSleepScoreParametersByDate(accoundId, dateTime.minusDays(5));
        assertThat(retrievedParameter.durationThreshold, is(1000));
        assertThat(retrievedParameter.accountId, is(accoundId));

        // get 500 threshold
        retrievedParameter = dao.getSleepScoreParametersByDate(accoundId, dateTime.minusDays(1));
        assertThat(retrievedParameter.durationThreshold, is(500));
        assertThat(retrievedParameter.accountId, is(accoundId));

        // no data for this date, should get default
        retrievedParameter = dao.getSleepScoreParametersByDate(accoundId, dateTime.minusDays(20));
        assertThat(retrievedParameter.durationThreshold, is(SleepScoreParameters.MISSING_DURATION_THRESHOLD));

    }
}