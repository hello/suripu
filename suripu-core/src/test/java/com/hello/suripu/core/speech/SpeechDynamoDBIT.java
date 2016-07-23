package com.hello.suripu.core.speech;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SpeechDynamoDBIT {

    private final static Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SpeechDynamoDBIT.class);

    private AmazonDynamoDB amazonDynamoDB;
    private SpeechResultDynamoDBDAO speechDAO;
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
            SpeechResultDynamoDBDAO.createTable(this.amazonDynamoDB, tableName);
        } catch (InterruptedException ie) {
            LOGGER.warn("Table already exists");
        }
        speechDAO = SpeechResultDynamoDBDAO.create(amazonDynamoDB, tableName);
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
        final Long accountId = 1L;
        final DateTime dateTime = new DateTime().withYear(2016).withMonthOfYear(7).withDayOfMonth(22).withTimeAtStartOfDay();
        final String senseId = "sleepbetterer";
        final String text = "what is the meaning of life";
        final Map<String, Float> confidences = Maps.newHashMap();
        confidences.put("okay sense", 0.5f);

        final SpeechResult speechResult = new SpeechResult.Builder()
                .withAccountId(accountId)
                .withDateTimeUTC(dateTime)
                .withSenseId(senseId)
                .withAudioIndentifier("abcd")
                .withText(text)
                .withResponseText("22")
                .withCommand("as-you-wish")
                .withConfidence(1.0f)
                .withWakeWordsConfidence(confidences)
                .build();

        final boolean putRes = speechDAO.putItem(speechResult);
        assertThat(putRes, is(true));

        final Optional<SpeechResult> optionalResult = speechDAO.getItem(accountId, dateTime, senseId);
        assertThat(optionalResult.isPresent(), is(true));

        if (optionalResult.isPresent()) {
            final SpeechResult result = optionalResult.get();
            assertThat(result.text.equals(text), is(true));
            assertThat(result.confidence, is(1.0f));
            assertThat(result.responseText.equals("22"), is(true));
        }
    }

    @Test
    public void testGetLatest() {
        final Long accountId = 1L;
        final DateTime dateTime = DateTime.now(DateTimeZone.UTC).minusMinutes(5);
        final String senseId = "sleepbetterer";
        final String text = "what is the meaning of life";
        final Map<String, Float> confidences = Maps.newHashMap();
        confidences.put("okay sense", 0.5f);

        final SpeechResult speechResult = new SpeechResult.Builder()
                .withAccountId(accountId)
                .withDateTimeUTC(dateTime)
                .withSenseId(senseId)
                .withAudioIndentifier("abcd")
                .withText(text)
                .withResponseText("22")
                .withCommand("as-you-wish")
                .withConfidence(1.0f)
                .withWakeWordsConfidence(confidences)
                .build();

        boolean putRes = speechDAO.putItem(speechResult);
        assertThat(putRes, is(true));

        final DateTime now = DateTime.now(DateTimeZone.UTC).minusMinutes(1);
        final SpeechResult speechResult2 = new SpeechResult.Builder()
                .withAccountId(accountId)
                .withDateTimeUTC(now)
                .withSenseId(senseId)
                .withAudioIndentifier("abcdef")
                .withText(text)
                .withResponseText("24")
                .withCommand("stfu")
                .withConfidence(1.0f)
                .withWakeWordsConfidence(confidences)
                .build();

        putRes = speechDAO.putItem(speechResult2);
        assertThat(putRes, is(true));

        final Optional<SpeechResult> optionalResult = speechDAO.getLatest(accountId, senseId, 3);
        assertThat(optionalResult.isPresent(), is(true));

        if (optionalResult.isPresent()) {
            final SpeechResult result = optionalResult.get();
            assertThat(result.text.equals(text), is(true));
            assertThat(result.responseText.equals("24"), is(true));
            assertThat(result.command.equals("stfu"), is(true));
        }

    }
}
