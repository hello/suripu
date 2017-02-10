package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.MainEventTimes;
import com.hello.suripu.core.models.SleepPeriod;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by jarredheinrich on 2/8/17.
 */
public class MainEventTimesDynamoDBIT {
    private final static Logger LOGGER = LoggerFactory.getLogger(MainEventTimesDynamoDBIT.class);

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private MainEventTimesDynamoDB mainEventTimesDynamoDB;
    private final String tableName = "test_main_event_times";

    @Before
    public void setUp(){
        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        cleanUp();

        try {
            LOGGER.debug("-------- Creating Table {} ---------", tableName);
            final TableDescription result = MainEventTimesDynamoDB.createTable(this.amazonDynamoDBClient, tableName);
            LOGGER.debug("Created dynamoDB table {}", result);
            this.mainEventTimesDynamoDB = MainEventTimesDynamoDB.create(this.amazonDynamoDBClient, tableName);
        }catch (ResourceInUseException rie){
            LOGGER.warn("Problem creating table");
        }
    }

    @After
    public void cleanUp(){
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(this.tableName);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            LOGGER.warn("Can not delete non existing table");
        }
    }

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

        final MainEventTimes mainEventTimesMorning = MainEventTimes.create(inBedDateTime.getMillis(), offset,
                sleepDateTime.getMillis(), offset, wakeUpDateTime.getMillis(), offset, outOfBedDateTime.getMillis(), offset, createdAtDateTime.getMillis());

        boolean updateSuccessful = mainEventTimesDynamoDB.updateEventTimes(accountId,targetDate, mainEventTimesMorning);
        assert(updateSuccessful);

        inBedDateTime = inBedDateTime.plusHours(15);
        sleepDateTime = sleepDateTime.plusHours(15);
        wakeUpDateTime = wakeUpDateTime.plusHours(15);
        outOfBedDateTime = outOfBedDateTime.plusHours(15);
        createdAtDateTime = createdAtDateTime.plusHours(15);

        final MainEventTimes mainEventTimesNight = MainEventTimes.create(inBedDateTime.getMillis(), offset,
                sleepDateTime.getMillis(), offset, wakeUpDateTime.getMillis(), offset, outOfBedDateTime.getMillis(), offset, createdAtDateTime.getMillis());

        updateSuccessful = mainEventTimesDynamoDB.updateEventTimes(accountId,targetDate, mainEventTimesNight);
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

        final Map<SleepPeriod.Period, MainEventTimes> sleepPeriodMainEventsEmpyMap = mainEventTimesDynamoDB.getEventTimes(accountId,targetDate);
        assert(sleepPeriodMainEventsEmpyMap .isEmpty());

        final MainEventTimes mainEventTimesMorning = MainEventTimes.create(inBedDateTime.getMillis(), offset,
                sleepDateTime.getMillis(), offset, wakeUpDateTime.getMillis(), offset, outOfBedDateTime.getMillis(), offset, createdAtDateTime.getMillis());

        mainEventTimesDynamoDB.updateEventTimes(accountId,targetDate, mainEventTimesMorning);

        inBedDateTime = inBedDateTime.plusHours(15);
        sleepDateTime = sleepDateTime.plusHours(15);
        wakeUpDateTime = wakeUpDateTime.plusHours(15);
        outOfBedDateTime = outOfBedDateTime.plusHours(15);
        createdAtDateTime = createdAtDateTime.plusHours(15);

        final MainEventTimes mainEventTimesNight = MainEventTimes.create(inBedDateTime.getMillis(), offset,
                sleepDateTime.getMillis(), offset, wakeUpDateTime.getMillis(), offset, outOfBedDateTime.getMillis(), offset, createdAtDateTime.getMillis());

        mainEventTimesDynamoDB.updateEventTimes(accountId,targetDate, mainEventTimesNight);

        final Map<SleepPeriod.Period, MainEventTimes> sleepPeriodMainEventsMap = mainEventTimesDynamoDB.getEventTimes(accountId,targetDate);
        assert(sleepPeriodMainEventsMap.containsKey(SleepPeriod.Period.MORNING));
        assert(sleepPeriodMainEventsMap.containsKey(SleepPeriod.Period.NIGHT));

        final MainEventTimes updatedNightEvents = sleepPeriodMainEventsMap.get(SleepPeriod.Period.NIGHT);
        final MainEventTimes updatedMorningEvents = sleepPeriodMainEventsMap.get(SleepPeriod.Period.MORNING);

        assert(updatedNightEvents.createdAt == mainEventTimesNight.createdAt);
        assert(updatedMorningEvents.createdAt == mainEventTimesMorning.createdAt);
        final List<Event.Type> mainEventTypes = Arrays.asList(Event.Type.IN_BED, Event.Type.SLEEP,Event.Type.WAKE_UP,Event.Type.OUT_OF_BED);
        for (final Event.Type mainEventType : mainEventTypes) {

            assert (updatedMorningEvents.eventTimeMap.get(mainEventType).TIME.longValue() == mainEventTimesMorning.eventTimeMap.get(mainEventType).TIME.longValue());
            assert (updatedMorningEvents.eventTimeMap.get(mainEventType).OFFSET.intValue() == mainEventTimesMorning.eventTimeMap.get(mainEventType).OFFSET.intValue());
            assert (updatedNightEvents.eventTimeMap.get(mainEventType).TIME.longValue() == mainEventTimesNight.eventTimeMap.get(mainEventType).TIME.longValue());
            assert (updatedNightEvents.eventTimeMap.get(mainEventType).OFFSET.intValue() == mainEventTimesNight.eventTimeMap.get(mainEventType).OFFSET.intValue());
        }
    }



}
