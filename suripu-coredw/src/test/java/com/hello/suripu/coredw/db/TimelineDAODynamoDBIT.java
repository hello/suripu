package com.hello.suripu.coredw.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.CachedTimelines;
import com.hello.suripu.core.models.Events.FallingAsleepEvent;
import com.hello.suripu.core.models.Events.WakeupEvent;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.models.TimelineLog;
import com.hello.suripu.core.models.TimelineResult;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by pangwu on 6/5/14.
 */
public class TimelineDAODynamoDBIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(TimelineDAODynamoDBIT.class);

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private TimelineDAODynamoDB timelineDAODynamoDB;
    private Timeline timeline1;
    private Timeline timeline2;

    private final String tableName = "event_test";


    @Before
    public void setUp(){
        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        cleanUp();
        final ArrayList<SleepSegment> sleepSegments = new ArrayList<>();
        final DateTime now = DateTime.now();
        sleepSegments.add(new SleepSegment(0L,
                new FallingAsleepEvent(now.getMillis(), now.plusMinutes(1).getMillis(), 0),
                Collections.EMPTY_LIST));

        sleepSegments.add(new SleepSegment(1L,
                new WakeupEvent(now.plusHours(8).getMillis(), now.plusHours(8).plusMinutes(1).getMillis(), 0),
                Collections.EMPTY_LIST));
        this.timeline1 = Timeline.create(80, "test1",
                now.withTimeAtStartOfDay().toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT),
                sleepSegments,
                Collections.EMPTY_LIST);


        sleepSegments.clear();
        sleepSegments.add(new SleepSegment(2L,
                new FallingAsleepEvent(now.plusDays(1).getMillis(), now.plusDays(1).plusMinutes(1).getMillis(), 0),
                Collections.EMPTY_LIST));

        sleepSegments.add(new SleepSegment(3L,
                new WakeupEvent(now.plusDays(1).plusHours(8).getMillis(), now.plusDays(1).plusHours(8).plusMinutes(1).getMillis(), 0),
                Collections.EMPTY_LIST));
        this.timeline2 = Timeline.create(90, "test2",
                now.plusDays(1).withTimeAtStartOfDay().toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT),
                sleepSegments,
                Collections.EMPTY_LIST);


        try {
            TimelineDAODynamoDB.createTable(tableName, this.amazonDynamoDBClient);
            this.timelineDAODynamoDB = new TimelineDAODynamoDB(
                    this.amazonDynamoDBClient,
                    tableName,
                    20
            );


        }catch (ResourceInUseException rie){
            LOGGER.warn("Can not create existing table");
        }
    }


    @After
    public void cleanUp(){
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(tableName);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            LOGGER.warn("Can not delete non existing table");
        }
    }


    @Test
    public void testGetNoneExistingDate(){
        final DateTime startOfDay = DateTime.now().withZone(DateTimeZone.UTC).withTimeAtStartOfDay();
        final Optional<TimelineResult> actual = this.timelineDAODynamoDB.getTimelinesForDate(1L, startOfDay);
        assertThat(!actual.isPresent(), is(true));
    }

    @Test
    public void testGetTimelinesForDatesImplNoneExistingDates(){
        final DateTime startOfDay = DateTime.now().withZone(DateTimeZone.UTC).withTimeAtStartOfDay();
        final List<Long> requestDates = Lists.newArrayList(startOfDay.getMillis());
        final Map<Long, CachedTimelines> actual = this.timelineDAODynamoDB.getTimelinesForDatesImpl(1L, requestDates);
        assertThat(actual.isEmpty(), is(true));
    }

    @Test
    public void testSetAndGetEventsForDates(){
        final DateTime startOfDay1 = DateTime.now().withTimeAtStartOfDay();
        final ArrayList<Timeline> timelinesForDay1 = new ArrayList<>();

        timelinesForDay1.add(this.timeline1);

        TimelineResult resultsForDay1 = TimelineResult.create(ImmutableList.copyOf(timelinesForDay1), TimelineLog.createEmpty());

        long accountId = 1;


        final DateTime startOfDay2 = startOfDay1.plusDays(1);
        final ArrayList<Timeline> timelinesForDay2 = new ArrayList<>();
        timelinesForDay2.add(this.timeline2);
        TimelineResult resultsForDay2 = TimelineResult.create(ImmutableList.copyOf(timelinesForDay2), TimelineLog.createEmpty());


        final Map<DateTime, TimelineResult> dateTimelinesMap = new HashMap<>();
        dateTimelinesMap.put(startOfDay1, resultsForDay1);
        dateTimelinesMap.put(startOfDay2, resultsForDay2);
        this.timelineDAODynamoDB.saveTimelinesForDates(accountId, dateTimelinesMap);

        final Map<DateTime, TimelineResult> actual = this.timelineDAODynamoDB.getTimelinesForDates(accountId, dateTimelinesMap.keySet());

        assertThat(actual.size(), is(2));
        for(final DateTime targetDay:dateTimelinesMap.keySet()){
            assertThat(actual.containsKey(targetDay), is(Boolean.TRUE));
            assertThat(actual.get(targetDay).timelines.size(), is(1));
            assertThat(actual.get(targetDay).timelines.get(0).statistics.isPresent(), is(false));
            //assertThat(actual.get(targetDay).get(0).statistics.get(), is((SleepStats)null));

            if(targetDay.equals(startOfDay1)) {
                assertThat(actual.get(targetDay).timelines.get(0).score, is(80));
                assertThat(actual.get(targetDay).timelines.get(0).message, is("test1"));
            }

            if(targetDay.equals(startOfDay2)) {
                assertThat(actual.get(targetDay).timelines.get(0).score, is(90));
                assertThat(actual.get(targetDay).timelines.get(0).message, is("test2"));
            }

        }



    }


    @Test
    public void testSetTwoDaysButOnlyGetOne(){
        final DateTime startOfDay1 = DateTime.now().withTimeAtStartOfDay();
        final ArrayList<Timeline> timelinesForDay1 = new ArrayList<>();

        timelinesForDay1.add(this.timeline1);
        long accountId = 1;


        final DateTime startOfDay2 = startOfDay1.plusDays(1);
        final ArrayList<Timeline> timelinesForDay2 = new ArrayList<>();
        timelinesForDay2.add(this.timeline2);

        final TimelineResult resultsForDay1 = TimelineResult.create(ImmutableList.copyOf(timelinesForDay1),TimelineLog.createEmpty());
        final TimelineResult resultsForDay2 = TimelineResult.create(ImmutableList.copyOf(timelinesForDay2),TimelineLog.createEmpty());

        final Map<DateTime, TimelineResult> dateTimelinesMap = new HashMap<>();
        dateTimelinesMap.put(startOfDay1, resultsForDay1);
        dateTimelinesMap.put(startOfDay2, resultsForDay2);
        this.timelineDAODynamoDB.saveTimelinesForDates(accountId, dateTimelinesMap);
        final HashSet<DateTime> queryDates = new HashSet<>();
        queryDates.add(startOfDay1);

        final Map<DateTime, TimelineResult> actual = this.timelineDAODynamoDB.getTimelinesForDates(accountId, queryDates);

        assertThat(actual.size(), is(1));
        for(final DateTime targetDay:actual.keySet()){
            assertThat(actual.get(targetDay).timelines.size(), is(1));


            assertThat(targetDay, is(startOfDay1));
            assertThat(actual.get(targetDay).timelines.get(0).statistics.isPresent(), is(false));
            //assertThat(actual.get(targetDay).get(0).statistics.get(), is((SleepStats)null));
            assertThat(actual.get(targetDay).timelines.get(0).score, is(80));
            assertThat(actual.get(targetDay).timelines.get(0).message, is("test1"));

        }



    }


    @Test
    public void testInvalidateCache(){
        final DateTime startOfDay1 = DateTime.now().withTimeAtStartOfDay();
        final ArrayList<Timeline> timelinesForDay1 = new ArrayList<>();

        timelinesForDay1.add(this.timeline1);
        long accountId = 1;


        final Map<DateTime, TimelineResult> dateTimelinesMap = new HashMap<>();

        final TimelineResult resultsForDay1 = TimelineResult.create(ImmutableList.copyOf(timelinesForDay1),TimelineLog.createEmpty());


        dateTimelinesMap.put(startOfDay1, resultsForDay1);
        this.timelineDAODynamoDB.saveTimelinesForDates(accountId, dateTimelinesMap);
        final HashSet<DateTime> queryDates = new HashSet<>();
        queryDates.add(startOfDay1);

        Map<DateTime, TimelineResult> actual = this.timelineDAODynamoDB.getTimelinesForDates(accountId, queryDates);

        assertThat(actual.size(), is(1));
        for(final DateTime targetDay:actual.keySet()){
            assertThat(actual.get(targetDay).timelines.size(), is(1));


            assertThat(targetDay, is(startOfDay1));
            assertThat(actual.get(targetDay).timelines.get(0).statistics.isPresent(), is(false));
            //assertThat(actual.get(targetDay).get(0).statistics.get(), is((SleepStats)null));
            assertThat(actual.get(targetDay).timelines.get(0).score, is(80));
            assertThat(actual.get(targetDay).timelines.get(0).message, is("test1"));

        }

        final boolean invalidateResult = this.timelineDAODynamoDB.invalidateCache(accountId, startOfDay1, startOfDay1);
        assertThat(invalidateResult, is(true));
        actual = this.timelineDAODynamoDB.getTimelinesForDates(accountId, queryDates);
        assertThat(actual.size(), is(0));  // Ahh I made this wrong....

    }

    @Test
    public void testInvalidateNoneExistingCache(){
        final DateTime startOfDay1 = DateTime.now().withTimeAtStartOfDay();

        long accountId = 1;
        final boolean invalidateResult = this.timelineDAODynamoDB.invalidateCache(accountId, startOfDay1, startOfDay1);
        assertThat(invalidateResult, is(false));
    }

    @Test
    public void testInvalidateCacheShouldNotDeleteData(){
        final DateTime startOfDay1 = DateTime.now().withTimeAtStartOfDay();
        final ArrayList<Timeline> timelinesForDay1 = new ArrayList<>();

        timelinesForDay1.add(this.timeline1);
        long accountId = 1;


        TimelineResult resultsForDay1 = TimelineResult.create(ImmutableList.copyOf(timelinesForDay1), TimelineLog.createEmpty());

        final Map<DateTime, TimelineResult> dateTimelinesMap = new HashMap<>();
        dateTimelinesMap.put(startOfDay1, resultsForDay1);
        this.timelineDAODynamoDB.saveTimelinesForDates(accountId, dateTimelinesMap);
        final HashSet<DateTime> queryDates = new HashSet<>();
        queryDates.add(startOfDay1);

        Map<DateTime, TimelineResult> actual = this.timelineDAODynamoDB.getTimelinesForDates(accountId, queryDates);

        assertThat(actual.size(), is(1));
        for(final DateTime targetDay:actual.keySet()){
            assertThat(actual.get(targetDay).timelines.size(), is(1));


            assertThat(targetDay, is(startOfDay1));
            assertThat(actual.get(targetDay).timelines.get(0).statistics.isPresent(), is(false));
            //assertThat(actual.get(targetDay).get(0).statistics.get(), is((SleepStats)null));
            assertThat(actual.get(targetDay).timelines.get(0).score, is(80));
            assertThat(actual.get(targetDay).timelines.get(0).message, is("test1"));

        }

        final boolean invalidateResult = this.timelineDAODynamoDB.setExpiredAt(accountId, startOfDay1, startOfDay1.plusDays(2));
        assertThat(invalidateResult, is(true));
        actual = this.timelineDAODynamoDB.getTimelinesForDates(accountId, queryDates);
        assertThat(actual.size(), is(1));

        for(final DateTime targetDay:actual.keySet()){
            assertThat(actual.get(targetDay).timelines.size(), is(1));
            assertThat(targetDay, is(startOfDay1));
            assertThat(actual.get(targetDay).timelines.get(0).statistics.isPresent(), is(false));
            //assertThat(actual.get(targetDay).get(0).statistics.get(), is((SleepStats)null));
            assertThat(actual.get(targetDay).timelines.get(0).score, is(80));
            assertThat(actual.get(targetDay).timelines.get(0).message, is("test1"));

        }

    }

}
