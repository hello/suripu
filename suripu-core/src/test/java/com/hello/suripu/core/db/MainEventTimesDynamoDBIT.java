package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.MainEventTimes;
import com.hello.suripu.core.util.AlgorithmType;
import com.hello.suripu.core.util.TimelineError;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Created by jarredheinrich on 2/8/17.
 */
public class MainEventTimesDynamoDBIT {
    private final static Logger LOGGER = LoggerFactory.getLogger(MainEventTimesDynamoDBIT.class);

    private BasicAWSCredentials awsCredentials;
    private final ClientConfiguration clientConfiguration = new ClientConfiguration();
    private final String endpoint = "http://localhost:7777";
    private AmazonDynamoDB amazonDynamoDBClient;
    private MainEventTimesDynamoDB mainEventTimesDynamoDB;

    private static final String TABLE_NAME= "integration_test_main_events_table";



    //region setUp/tearDown

    @Before
    public void setUp() throws Exception {
        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");
        this.mainEventTimesDynamoDB = new MainEventTimesDynamoDB(amazonDynamoDBClient, TABLE_NAME);

        tearDown();
        final List<String> tables = ImmutableList.of(TABLE_NAME);

        try {
            for (final String tableName : tables) {
                LOGGER.debug("-------- Creating Table {} ---------", tableName);
                final CreateTableResult result = mainEventTimesDynamoDB.createTable(tableName);
                LOGGER.debug("Created dynamoDB table {}", result.getTableDescription());
            }
        } catch (ResourceInUseException rie){
            LOGGER.warn("Problem creating table");
        }
    }

    @After
    public void tearDown() throws Exception {
        final List<String> tables = ImmutableList.of(TABLE_NAME);
        for (final String name: tables) {
            final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                    .withTableName(name);
            try {
                this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
            }catch (ResourceNotFoundException ex){
                LOGGER.warn("Can not delete non existing table {}", name);
            }
        }
    }

    //endregion setUp/tearDown


    @Test
    public void testUpdateMainEventTimes() throws Exception {

        final Long accountId = 0L;
        DateTime inBedDateTime = new DateTime(2017, 1,1, 5, 15, 00, DateTimeZone.forID("America/Los_Angeles"));
        DateTime sleepDateTime = inBedDateTime.plusMinutes(22);
        DateTime wakeUpDateTime = inBedDateTime.plusMinutes(432);
        DateTime outOfBedDateTime = wakeUpDateTime.plusMinutes(13);
        DateTime createdAtDateTime = outOfBedDateTime.plusMinutes(60);
        DateTime targetDate = inBedDateTime.withTimeAtStartOfDay();
        final int offset = -28800000;

        final MainEventTimes mainEventTimesMorning = MainEventTimes.createMainEventTimes(accountId, inBedDateTime.getMillis(), offset,
                sleepDateTime.getMillis(), offset, wakeUpDateTime.getMillis(), offset, outOfBedDateTime.getMillis(), offset, createdAtDateTime.getMillis(), offset, AlgorithmType.NONE, TimelineError.NO_ERROR);

        boolean updateSuccessful = mainEventTimesDynamoDB.updateEventTimes(mainEventTimesMorning);
        assert(updateSuccessful);

        inBedDateTime = inBedDateTime.plusHours(15);
        sleepDateTime = sleepDateTime.plusHours(15);
        wakeUpDateTime = wakeUpDateTime.plusHours(15);
        outOfBedDateTime = outOfBedDateTime.plusHours(14);
        createdAtDateTime = createdAtDateTime.plusHours(14);

        final MainEventTimes mainEventTimesNight = MainEventTimes.createMainEventTimes(accountId, inBedDateTime.getMillis(), offset,
                sleepDateTime.getMillis(), offset, wakeUpDateTime.getMillis(), offset, outOfBedDateTime.getMillis(), offset, createdAtDateTime.getMillis(), offset, AlgorithmType.NONE, TimelineError.NO_ERROR);

        updateSuccessful = mainEventTimesDynamoDB.updateEventTimes(mainEventTimesNight);
        assert(updateSuccessful);


        outOfBedDateTime = outOfBedDateTime.plusHours(15);
        createdAtDateTime = createdAtDateTime.plusHours(15);

        final MainEventTimes mainEventTimesNight2 = MainEventTimes.createMainEventTimes(accountId, inBedDateTime.getMillis(), offset,
                sleepDateTime.getMillis(), offset, wakeUpDateTime.getMillis(), offset, outOfBedDateTime.getMillis(), offset, createdAtDateTime.getMillis(), offset, AlgorithmType.NONE, TimelineError.NO_ERROR);

        updateSuccessful = mainEventTimesDynamoDB.updateEventTimes(mainEventTimesNight2);
        assert(updateSuccessful);


    }

    @Test
    public void testGetMainEventTimes() throws Exception{

        final Long accountId = 1L;
        DateTime inBedDateTime = new DateTime(2017, 1,1, 5, 15, 00, DateTimeZone.forID("America/Los_Angeles"));
        DateTime sleepDateTime = inBedDateTime.plusMinutes(22);
        DateTime wakeUpDateTime = inBedDateTime.plusMinutes(432);
        DateTime outOfBedDateTime = wakeUpDateTime.plusMinutes(13);
        DateTime createdAtDateTime = outOfBedDateTime.plusMinutes(60);
        DateTime targetDate = inBedDateTime.withTimeAtStartOfDay();
        final int offset = -28800000;

        final List<MainEventTimes> sleepPeriodMainEventsEmptyList = mainEventTimesDynamoDB.getEventTimes(accountId,targetDate);
        assert(sleepPeriodMainEventsEmptyList.isEmpty());

        final MainEventTimes mainEventTimesMorning = MainEventTimes.createMainEventTimes(accountId, inBedDateTime.getMillis(), offset,
                sleepDateTime.getMillis(), offset, wakeUpDateTime.getMillis(), offset, outOfBedDateTime.getMillis(), offset, createdAtDateTime.getMillis(), offset, AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.NO_ERROR);

        mainEventTimesDynamoDB.updateEventTimes(mainEventTimesMorning);

        inBedDateTime = inBedDateTime.plusHours(15);
        sleepDateTime = sleepDateTime.plusHours(15);
        wakeUpDateTime = wakeUpDateTime.plusHours(15);
        outOfBedDateTime = outOfBedDateTime.plusHours(15);
        createdAtDateTime = createdAtDateTime.plusHours(15);

        final MainEventTimes mainEventTimesNight = MainEventTimes.createMainEventTimes(accountId, inBedDateTime.getMillis(), offset,
                sleepDateTime.getMillis(), offset, wakeUpDateTime.getMillis(), offset, outOfBedDateTime.getMillis(), offset, createdAtDateTime.getMillis(), offset,AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.NO_ERROR);

        mainEventTimesDynamoDB.updateEventTimes(mainEventTimesNight);


        inBedDateTime =  new DateTime(2017, 1,2, 5, 15, 00, DateTimeZone.forID("America/Los_Angeles"));
        sleepDateTime = sleepDateTime.plusHours(15);
        wakeUpDateTime = wakeUpDateTime.plusHours(15);
        outOfBedDateTime = outOfBedDateTime.plusHours(15);
        createdAtDateTime = createdAtDateTime.plusHours(15);

        final MainEventTimes mainEventTimesNextMorning = MainEventTimes.createMainEventTimes(accountId, inBedDateTime.getMillis(), offset,
                sleepDateTime.getMillis(), offset, wakeUpDateTime.getMillis(), offset, outOfBedDateTime.getMillis(), offset, createdAtDateTime.getMillis(), offset, AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.NO_ERROR);

        mainEventTimesDynamoDB.updateEventTimes(mainEventTimesNextMorning);


        final List<MainEventTimes> sleepPeriodMainEventsList = mainEventTimesDynamoDB.getEventTimes(accountId,targetDate);
        assert(sleepPeriodMainEventsList.size() == 2);

        final MainEventTimes updatedNightEvents = sleepPeriodMainEventsList.get(1);
        final MainEventTimes updatedMorningEvents = sleepPeriodMainEventsList.get(0);

        assert(updatedNightEvents.createdAt.time.longValue() == mainEventTimesNight.createdAt.time.longValue());
        assert(updatedMorningEvents.createdAt.time.longValue() == mainEventTimesMorning.createdAt.time.longValue());
        final List<Event.Type> mainEventTypes = Arrays.asList(Event.Type.IN_BED, Event.Type.SLEEP,Event.Type.WAKE_UP,Event.Type.OUT_OF_BED);
        for (final Event.Type mainEventType : mainEventTypes) {

            assert (updatedMorningEvents.eventTimeMap.get(mainEventType).time.longValue() == mainEventTimesMorning.eventTimeMap.get(mainEventType).time.longValue());
            assert (updatedMorningEvents.eventTimeMap.get(mainEventType).offset.intValue() == mainEventTimesMorning.eventTimeMap.get(mainEventType).offset.intValue());
            assert (updatedNightEvents.eventTimeMap.get(mainEventType).time.longValue() == mainEventTimesNight.eventTimeMap.get(mainEventType).time.longValue());
            assert (updatedNightEvents.eventTimeMap.get(mainEventType).offset.intValue() == mainEventTimesNight.eventTimeMap.get(mainEventType).offset.intValue());
        }


        inBedDateTime = new DateTime(2017, 1,1, 5, 15, 00, DateTimeZone.forID("America/Los_Angeles"));
        sleepDateTime = inBedDateTime.plusMinutes(22);
        wakeUpDateTime = inBedDateTime.plusMinutes(432);
        outOfBedDateTime = wakeUpDateTime.plusMinutes(17);
        createdAtDateTime = outOfBedDateTime.plusMinutes(90);
        targetDate = inBedDateTime.withTimeAtStartOfDay();

        final MainEventTimes mainEventTimesMorning2 = MainEventTimes.createMainEventTimes(accountId, inBedDateTime.getMillis(), offset,
                sleepDateTime.getMillis(), offset, wakeUpDateTime.getMillis(), offset, outOfBedDateTime.getMillis(), offset, createdAtDateTime.getMillis(), offset,AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.NO_ERROR);

        mainEventTimesDynamoDB.updateEventTimes(mainEventTimesMorning2);

        final List<MainEventTimes> sleepPeriodMainEventsList2 = mainEventTimesDynamoDB.getEventTimes(accountId,targetDate);
        assert(sleepPeriodMainEventsList.size() == 2);

        final MainEventTimes updatedMorningEvents2 = sleepPeriodMainEventsList2.get(0);


        assert(updatedMorningEvents2.createdAt.time.longValue() == mainEventTimesMorning2.createdAt.time.longValue());
       for (final Event.Type mainEventType : mainEventTypes) {

            assert (updatedMorningEvents2.eventTimeMap.get(mainEventType).time.longValue() == mainEventTimesMorning2.eventTimeMap.get(mainEventType).time.longValue());
            assert (updatedMorningEvents2.eventTimeMap.get(mainEventType).offset.intValue() == mainEventTimesMorning2.eventTimeMap.get(mainEventType).offset.intValue());

        }
    }



}
