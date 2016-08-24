package com.hello.suripu.core.speech;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.kms.AWSKMSClient;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SpeechTimelineDynamoDBIT {

    private final static Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SpeechTimelineDynamoDBIT.class);

    private static final Long accountId = 1L;
    private static final String senseId = "sleepbetterer";
    private static final String uuid1 = "encrypted_uuid_1";
    private static final String uuid2 = "encrypted_uuid_2";

    private AmazonDynamoDB amazonDynamoDB;
    private SpeechTimelineDAODynamoDB speechDAO;
    private String tableName = "test_speech";

    @Before
    public void setUp(){
        final BasicAWSCredentials awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);

        tearDown();
        this.amazonDynamoDB = new AmazonDynamoDBClient(awsCredentials, clientConfiguration);
        this.amazonDynamoDB.setEndpoint("http://localhost:7777");

        try {
            SpeechTimelineDAODynamoDB.createTable(this.amazonDynamoDB, tableName);
        } catch (InterruptedException ie) {
            LOGGER.warn("Table already exists");
        }

        final String keyid = "xxx"
        final BasicAWSCredentials kmsCredentials = new BasicAWSCredentials("xxx", "xxx");
        final AWSKMSClient awskmsClient = new AWSKMSClient(kmsCredentials);
        awskmsClient.setEndpoint("https://kms.us-east-1.amazonaws.com");

        speechDAO = SpeechTimelineDAODynamoDB.create(amazonDynamoDB, tableName, awskmsClient, keyid);
    }

    @After
    public void tearDown(){
        try {
            amazonDynamoDB.deleteTable(tableName);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Test
    public void testPut() {

        final DateTime dateTime = new DateTime().withYear(2016).withMonthOfYear(8).withDayOfMonth(22).withTimeAtStartOfDay();
        final SpeechTimeline speechTimeline = new SpeechTimeline(accountId, dateTime, senseId, uuid1);

        final boolean putRes = speechDAO.putItem(speechTimeline);

        assertThat(putRes, is(true));

        final Optional<SpeechTimeline> optionalResult = speechDAO.getItem(accountId, dateTime);
        assertThat(optionalResult.isPresent(), is(true));

        if (optionalResult.isPresent()) {
            final SpeechTimeline result = optionalResult.get();
            assertThat(result.accountId.equals(accountId), is(true));
            assertThat(result.audioUUID.equals(uuid1), is(true));
        }
    }

    @Test
    public void testGetLatest() {
        final DateTime dateTime1 = DateTime.now(DateTimeZone.UTC).minusMinutes(5);
        final SpeechTimeline speechTimeline = new SpeechTimeline(accountId, dateTime1, senseId, uuid1);
        boolean putRes = speechDAO.putItem(speechTimeline);
        assertThat(putRes, is(true));

        final DateTime dateTime2 = DateTime.now(DateTimeZone.UTC).minusMinutes(1);
        final SpeechTimeline speechTimeline2 = new SpeechTimeline(accountId, dateTime2, senseId, uuid2);
        putRes = speechDAO.putItem(speechTimeline2);
        assertThat(putRes, is(true));

        final Optional<SpeechTimeline> optionalResult = speechDAO.getLatest(accountId, 3);
        assertThat(optionalResult.isPresent(), is(true));

        if (optionalResult.isPresent()) {
            final SpeechTimeline result = optionalResult.get();
            assertThat(result.accountId.equals(accountId), is(true));
            assertThat(result.audioUUID.equals(uuid2), is(true));
        }
    }

    @Test
    public void testGetBetween() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);

        final DateTime dateTime1 = now.minusMinutes(10);
        final SpeechTimeline speechTimeline1 = new SpeechTimeline(accountId, dateTime1, senseId, uuid1);
        boolean putRes = speechDAO.putItem(speechTimeline1);
        assertThat(putRes, is(true));

        final DateTime dateTime2 = now.minusMinutes(5);
        final SpeechTimeline speechTimeline2 = new SpeechTimeline(accountId, dateTime2, senseId, uuid2);
        putRes = speechDAO.putItem(speechTimeline2);
        assertThat(putRes, is(true));

        final DateTime dateTime3 = now.minusMinutes(3);
        final String uuid3 = "encrypted_uuid_3";
        final SpeechTimeline speechTimeline3 = new SpeechTimeline(accountId, dateTime3, senseId, uuid3);
        putRes = speechDAO.putItem(speechTimeline3);
        assertThat(putRes, is(true));

        final DateTime dateTime4 = now.minusMinutes(2);
        final String uuid4 = "encrypted_uuid_4";
        final SpeechTimeline speechTimeline4 = new SpeechTimeline(accountId, dateTime4, senseId, uuid4);
        putRes = speechDAO.putItem(speechTimeline4);
        assertThat(putRes, is(true));

        final DateTime queryStart = now.minusMinutes(5);
        final DateTime queryEnd = now.minusMinutes(2);

        final List<SpeechTimeline> results = speechDAO.getItemsByDate(accountId, queryStart, queryEnd, 10);
        assertThat(results.size(), is (3));
        assertThat(results.get(0).audioUUID.equals(uuid4), is(true));
        assertThat(results.get(1).audioUUID.equals(uuid3), is(true));
        assertThat(results.get(2).audioUUID.equals(uuid2), is(true));
    }
}
