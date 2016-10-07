package com.hello.suripu.core.speech;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.hello.suripu.core.speech.interfaces.SpeechTimelineIngestDAO;
import com.hello.suripu.core.speech.interfaces.SpeechTimelineReadDAO;
import com.hello.suripu.core.speech.interfaces.Vault;
import com.hello.suripu.core.speech.models.SpeechTimeline;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpeechTimelineDynamoDBIT {

    private final static Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SpeechTimelineDynamoDBIT.class);

    private static final Long accountId = 1L;
    private static final String senseId = "sleepbetterer";
    private static final String uuid1 = "uuid_1";
    private static final String uuid2 = "uuid_2";
    private static final String uuid3 = "uuid_3";
    private static final String uuid4 = "uuid_4";


    private AmazonDynamoDB amazonDynamoDB;
    private SpeechTimelineReadDAO speechReadDAO;
    private SpeechTimelineIngestDAO speechWriteDAO;
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
            SpeechTimelineReadDAODynamoDB.createTable(this.amazonDynamoDB, tableName);
        } catch (InterruptedException ie) {
            LOGGER.warn("Table already exists");
        }

        final Vault vault = mock(Vault.class);
        speechReadDAO = SpeechTimelineReadDAODynamoDB.create(amazonDynamoDB, tableName, vault);
        speechWriteDAO = SpeechTimelineIngestDAODynamoDB.create(amazonDynamoDB, tableName, vault);

        // mock kms operations
        final Map<String, String> ec = Maps.newHashMap();
        ec.put("account_id", accountId.toString());

        when(vault.encrypt(uuid1, ec)).thenReturn(Optional.of("encrypted_" + uuid1));
        when(vault.decrypt("encrypted_" + uuid1, ec)).thenReturn(Optional.of(uuid1));

        when(vault.encrypt(uuid2, ec)).thenReturn(Optional.of("encrypted_" + uuid2));
        when(vault.decrypt("encrypted_" + uuid2, ec)).thenReturn(Optional.of(uuid2));

        when(vault.encrypt(uuid3, ec)).thenReturn(Optional.of("encrypted_" + uuid3));
        when(vault.decrypt("encrypted_" + uuid3, ec)).thenReturn(Optional.of(uuid3));

        when(vault.encrypt(uuid4, ec)).thenReturn(Optional.of("encrypted_" + uuid4));
        when(vault.decrypt("encrypted_" + uuid4, ec)).thenReturn(Optional.of(uuid4));
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

        final boolean putRes = speechWriteDAO.putItem(speechTimeline);

        assertThat(putRes, is(true));

        final Optional<SpeechTimeline> optionalResult = speechReadDAO.getItem(accountId, dateTime);
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
        boolean putRes = speechWriteDAO.putItem(speechTimeline);
        assertThat(putRes, is(true));

        final DateTime dateTime2 = DateTime.now(DateTimeZone.UTC).minusMinutes(1);
        final SpeechTimeline speechTimeline2 = new SpeechTimeline(accountId, dateTime2, senseId, uuid2);
        putRes = speechWriteDAO.putItem(speechTimeline2);
        assertThat(putRes, is(true));

        final Optional<SpeechTimeline> optionalResult = speechReadDAO.getLatest(accountId, 3);
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
        boolean putRes = speechWriteDAO.putItem(speechTimeline1);
        assertThat(putRes, is(true));

        final DateTime dateTime2 = now.minusMinutes(5);
        final SpeechTimeline speechTimeline2 = new SpeechTimeline(accountId, dateTime2, senseId, uuid2);
        putRes = speechWriteDAO.putItem(speechTimeline2);
        assertThat(putRes, is(true));

        final DateTime dateTime3 = now.minusMinutes(3);
        final SpeechTimeline speechTimeline3 = new SpeechTimeline(accountId, dateTime3, senseId, uuid3);
        putRes = speechWriteDAO.putItem(speechTimeline3);
        assertThat(putRes, is(true));

        final DateTime dateTime4 = now.minusMinutes(2);
        final SpeechTimeline speechTimeline4 = new SpeechTimeline(accountId, dateTime4, senseId, uuid4);
        putRes = speechWriteDAO.putItem(speechTimeline4);
        assertThat(putRes, is(true));

        final DateTime queryStart = now.minusMinutes(5);
        final DateTime queryEnd = now.minusMinutes(2);

        final List<SpeechTimeline> results = speechReadDAO.getItemsByDate(accountId, queryStart, queryEnd, 10);
        assertThat(results.size(), is (3));
        assertThat(results.get(0).audioUUID.equals(uuid4), is(true));
        assertThat(results.get(1).audioUUID.equals(uuid3), is(true));
        assertThat(results.get(2).audioUUID.equals(uuid2), is(true));
    }
}
