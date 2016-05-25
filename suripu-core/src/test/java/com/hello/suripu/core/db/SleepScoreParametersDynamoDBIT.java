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

        insertResult = dao.upsertSleepScoreParameters(accoundId, new SleepScoreParameters(accoundId, dateTime, 200));
        assertThat(insertResult, is(true));

        retrievedParameter = dao.getSleepScoreParametersByDate(accoundId, dateTime);
        assertThat(retrievedParameter.durationThreshold, is(200));
    }
}