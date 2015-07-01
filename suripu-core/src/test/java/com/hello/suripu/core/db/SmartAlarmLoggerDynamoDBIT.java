package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.SmartAlarmHistory;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 5/28/15.
 */
public class SmartAlarmLoggerDynamoDBIT {
    private final static Logger LOGGER = LoggerFactory.getLogger(SmartAlarmLoggerDynamoDBIT.class);

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private SmartAlarmLoggerDynamoDB smartAlarmLoggerDynamoDB;
    private final String tableName = "smart_alarm_history_test";


    @Before
    public void setUp(){

        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        cleanUp();

        try {
            SmartAlarmLoggerDynamoDB.createTable(tableName, this.amazonDynamoDBClient);
            this.smartAlarmLoggerDynamoDB = new SmartAlarmLoggerDynamoDB(
                    this.amazonDynamoDBClient,
                    tableName
            );


        }catch (ResourceInUseException rie){
            LOGGER.warn("Can't create existing table");
        }
    }

    @After
    public void cleanUp(){
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(tableName);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            LOGGER.error("Can't delete non existing table");
        }
    }

    @Test
    public void testUpdateAndGetLog(){
        final DateTimeZone dateTimeZone = DateTimeZone.forID("America/Los_Angeles");
        final DateTime now = DateTime.now().withZone(dateTimeZone);
        final DateTime lastSleepCycleEnd = now.minusMinutes(2);
        final DateTime expectedRingTime = now.plusMinutes(30);
        final DateTime actualRingTime = now.plusMinutes(25);
        final long accountId = 1L;

        this.smartAlarmLoggerDynamoDB.log(accountId, now, lastSleepCycleEnd, actualRingTime, expectedRingTime, Optional.<DateTime>absent(), dateTimeZone);
        final List<SmartAlarmHistory> history = this.smartAlarmLoggerDynamoDB.getSmartAlarmHistoryByScheduleTime(accountId, now.withTimeAtStartOfDay(), now);
        assertThat(history.isEmpty(), is(false));
        assertThat(history.get(0).actualRingTimeLocal, is(actualRingTime.toString(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT)));
        assertThat(history.get(0).expectedRingTimeLocal, is(expectedRingTime.toString(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT)));

    }
}
