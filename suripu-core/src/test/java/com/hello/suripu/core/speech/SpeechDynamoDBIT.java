package com.hello.suripu.core.speech;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.hello.suripu.core.speech.interfaces.SpeechResultIngestDAO;
import com.hello.suripu.core.speech.interfaces.SpeechResultReadDAO;
import com.hello.suripu.core.speech.models.Result;
import com.hello.suripu.core.speech.models.SpeechResult;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SpeechDynamoDBIT {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SpeechDynamoDBIT.class);
    private static final Long accountId = 1L;
    private static final String senseId = "sleepbetterer";


    private AmazonDynamoDB amazonDynamoDB;
    private SpeechResultReadDAO speechDAO;
    private SpeechResultIngestDAO speechIngestDAO;
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
            SpeechResultIngestDAODynamoDB.createTable(this.amazonDynamoDB, tableName);
        } catch (InterruptedException ie) {
            LOGGER.warn("Table already exists");
        }
        speechDAO = SpeechResultReadDAODynamoDB.create(amazonDynamoDB, tableName);
        speechIngestDAO = SpeechResultIngestDAODynamoDB.create(amazonDynamoDB, tableName);
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

        final DateTime dateTime = new DateTime().withYear(2016).withMonthOfYear(7).withDayOfMonth(22).withTimeAtStartOfDay();
        final String text = "what is the meaning of life";
        final String uuid = UUID.randomUUID().toString();
        final Map<String, Float> confidences = Maps.newHashMap();
        confidences.put("okay sense", 0.5f);

        final SpeechResult speechResult = new SpeechResult.Builder()
                .withDateTimeUTC(dateTime)
                .withAudioIndentifier(uuid)
                .withText(text)
                .withResponseText("22")
                .withCommand("as-you-wish")
                .withConfidence(1.0f)
                .withWakeWordsConfidence(confidences)
                .build();

        boolean putRes = speechIngestDAO.putItem(speechResult);
        assertThat(putRes, is(true));

        final Optional<SpeechResult> optionalResult = speechDAO.getItem(uuid);
        assertThat(optionalResult.isPresent(), is(true));

        if (optionalResult.isPresent()) {
            final SpeechResult result = optionalResult.get();

            assertThat(result.text.isPresent(), is(true));
            if (result.text.isPresent()) {
                assertThat(result.text.get().equals(text), is(true));
            }

            assertThat(result.confidence.isPresent(), is(true));
            if (result.confidence.isPresent()) {
                assertThat(result.confidence.get(), is(1.0f));
            }

            assertThat(result.responseText.isPresent(), is(true));
            if (result.responseText.isPresent()) {
                assertThat(result.responseText.get().equals("22"), is(true));
            }

            assertThat(result.audioIdentifier.equalsIgnoreCase(uuid), is(true));
        }
    }

    @Test
    public void testGetBatchItems() {

        final DateTime dateTime = DateTime.now(DateTimeZone.UTC).minusMinutes(5);
        final String uuid1 = UUID.randomUUID().toString();
        final String text1 = "what is the meaning of life";
        final String command1 = "as-you-wish";
        final float conf1 = 1.0f;
        final Map<String, Float> confidences1 = Maps.newHashMap();
        confidences1.put("okay sense", 0.5f);

        final SpeechResult speechResult1 = new SpeechResult.Builder()
                .withDateTimeUTC(dateTime)
                .withAudioIndentifier(uuid1)
                .withText(text1)
                .withResponseText("stfu")
                .withCommand(command1)
                .withConfidence(conf1)
                .withWakeWordsConfidence(confidences1)
                .build();

        boolean putRes = speechIngestDAO.putItem(speechResult1);
        assertThat(putRes, is(true));

        final DateTime now = DateTime.now(DateTimeZone.UTC).minusMinutes(1);
        final String uuid2 = UUID.randomUUID().toString();
        final String text2 = "what is the meaning of life";
        final String command2 = "you-decider";
        final float conf2 = 1.0f;
        final Map<String, Float> confidences2 = Maps.newHashMap();
        confidences2.put("okay sense", 0.8f);

        final SpeechResult speechResult2 = new SpeechResult.Builder()
                .withDateTimeUTC(now)
                .withAudioIndentifier(uuid2)
                .withText(text2)
                .withResponseText("effoff")
                .withCommand(command2)
                .withConfidence(conf2)
                .withWakeWordsConfidence(confidences2)
                .build();

        putRes = speechIngestDAO.putItem(speechResult2);
        assertThat(putRes, is(true));

        final List<SpeechResult> speechResults = speechDAO.getItems(Arrays.asList(uuid1, uuid2));
        assertThat(speechResults.size(), is(2));

        int found = 0;
        for (final SpeechResult result: speechResults) {
            final Float confidence = (result.confidence.isPresent()) ? result.confidence.get() : 0.0f;
            final String text = (result.text.isPresent()) ? result.text.get() : "";
            final String command = (result.command.isPresent()) ? result.command.get() : "";

            if (result.audioIdentifier.equalsIgnoreCase(uuid1)) {
                found++;
                assertThat(text.equals(text1), is(true));
                assertThat(confidence, is(conf1));
                assertThat(command.equals(command1), is(true));
            } else if (result.audioIdentifier.equalsIgnoreCase(uuid2)) {
                found++;
                assertThat(text.equals(text2), is(true));
                assertThat(confidence, is(conf2));
                assertThat(command.equals(command2), is(true));
            }
        }
        assertThat(found, is(2));
    }

    @Test
    public void testUpdateCommand() {

        final DateTime dateTime = new DateTime(DateTimeZone.UTC).withYear(2016).withMonthOfYear(7).withDayOfMonth(22).withTimeAtStartOfDay();
        final String text = "what is the meaning of life";
        final String uuid = UUID.randomUUID().toString();
        final Map<String, Float> confidences = Maps.newHashMap();
        confidences.put("okay sense", 0.5f);

        final SpeechResult speechResult = new SpeechResult.Builder()
                .withDateTimeUTC(dateTime)
                .withAudioIndentifier(uuid)
                .withText(text)
                .withConfidence(1.0f)
                .withWakeWordsConfidence(confidences)
                .build();

        boolean putRes = speechIngestDAO.putItem(speechResult);
        assertThat(putRes, is(true));

        final Optional<SpeechResult> optionalResult = speechDAO.getItem(uuid);
        assertThat(optionalResult.isPresent(), is(true));

        if (optionalResult.isPresent()) {
            final SpeechResult result = optionalResult.get();
            assertThat(result.text.get().equals(text), is(true));
            assertThat(result.confidence.get(), is(1.0f));
            assertThat(result.audioIdentifier.equalsIgnoreCase(uuid), is(true));
            assertThat(result.handlerType.isPresent(), is(false));
            assertThat(result.s3ResponseKeyname.isPresent(), is(false));
            assertThat(result.command.isPresent(), is(false));
        }

        final String newCommand = "get-lost";
        final String handlerType = "handle-this!";
        final Result newResult = Result.REJECTED;
        final String responseText = "you suck";
        final DateTime updated = dateTime.plusMinutes(1);
        final SpeechResult speechResult2 = new SpeechResult.Builder()
                .withAudioIndentifier(uuid)
                .withUpdatedUTC(updated)
                .withHandlerType(handlerType)
                .withCommand(newCommand)
                .withResult(newResult)
                .withResponseText(responseText)
                .build();

        putRes = speechIngestDAO.updateItem(speechResult2);
        assertThat(putRes, is(true));

        final Optional<SpeechResult> optionalResult2 = speechDAO.getItem(uuid);
        assertThat(optionalResult2.isPresent(), is(true));
        if (optionalResult2.isPresent()) {
            final SpeechResult result = optionalResult2.get();
            assertThat(result.handlerType.get().equals(handlerType), is(true));
            assertThat(result.s3ResponseKeyname.isPresent(), is(false));
            assertThat(result.command.get().equals(newCommand), is(true));
            assertThat(result.result.equals(newResult), is(true));
            assertThat(result.responseText.get().equals(responseText), is(true));
            assertThat(result.updatedUTC.equals(updated), is(true));
        }

    }

}
