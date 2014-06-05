package com.hello.suripu.core.db;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

/**
 * Created by pangwu on 5/30/14.
 */
public class TrackerMotionDAODynamoDBTest {

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private TrackerMotionDAODynamoDB trackerMotionDAODynamoDB;
    private final String tableName = "tracker_motion_test";

    @Before
    public void setUp(){

        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(TrackerMotionDAODynamoDB.ACCOUNT_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(TrackerMotionDAODynamoDB.TARGET_DATE_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(TrackerMotionDAODynamoDB.ACCOUNT_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N),
                new AttributeDefinition().withAttributeName(TrackerMotionDAODynamoDB.TARGET_DATE_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)
        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        try {
            this.amazonDynamoDBClient.createTable(request);

            this.trackerMotionDAODynamoDB = new TrackerMotionDAODynamoDB(
                    this.amazonDynamoDBClient,
                    tableName
            );
        }catch (ResourceInUseException rie){
            rie.printStackTrace();
        }
    }

    @After
    public void cleanUp(){
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(tableName);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            ex.printStackTrace();
        }
    }



    @Test
    public void testSetTrackerMotions(){
        final ArrayList<TrackerMotion> testData = new ArrayList<TrackerMotion>();
        final DateTime startTime = DateTime.now().withTimeAtStartOfDay();
        long accountId = 1;

        testData.add(new TrackerMotion(-1, accountId, "",
                startTime.getMillis(),10, DateTimeZone.getDefault().getOffset(startTime)));
        testData.add(new TrackerMotion(-1,accountId,"",
                        startTime.plusMinutes(1).getMillis(),11, DateTimeZone.getDefault().getOffset(startTime.plusMinutes(1))));

        this.trackerMotionDAODynamoDB.setTrackerMotions(accountId, testData);
        final ImmutableList<TrackerMotion> actual = this.trackerMotionDAODynamoDB.getTrackerMotionForDate(accountId, startTime);

        assertThat(actual, containsInAnyOrder(testData.toArray(new TrackerMotion[0])));
        assertThat(actual.size(), is(testData.size()));

        final ArrayList<TrackerMotion> testDataCrossDays = new ArrayList<TrackerMotion>();
        testDataCrossDays.addAll(testData);
        testDataCrossDays.add(new TrackerMotion(-1,accountId,"",
                startTime.plusDays(1).plusMinutes(1).getMillis(),11, DateTimeZone.getDefault().getOffset(startTime.plusDays(1).plusMinutes(1))));
        this.trackerMotionDAODynamoDB.setTrackerMotions(accountId, testDataCrossDays);
        this.trackerMotionDAODynamoDB.setTrackerMotions(accountId, testDataCrossDays);
        final ImmutableList<TrackerMotion> dataFromDay1 = this.trackerMotionDAODynamoDB.getTrackerMotionForDate(accountId, startTime);
        final ImmutableList<TrackerMotion> dataFromDay2 = this.trackerMotionDAODynamoDB.getTrackerMotionForDate(accountId, startTime.plusDays(1));

        final ArrayList<TrackerMotion> actualForCrossDayTest = new ArrayList<TrackerMotion>();
        actualForCrossDayTest.addAll(dataFromDay1);
        actualForCrossDayTest.addAll(dataFromDay2);
        assertThat(actualForCrossDayTest, containsInAnyOrder(testDataCrossDays.toArray()));
        assertThat(testDataCrossDays.size(), is(actualForCrossDayTest.size()));
    }

    @Test
    public void testGetBetween(){
        final ArrayList<TrackerMotion> testData = new ArrayList<TrackerMotion>();
        final DateTime startTime = DateTime.now().withTimeAtStartOfDay();
        long accountId = 1;

        testData.add(new TrackerMotion(-1, accountId, "",
                startTime.getMillis(),10, DateTimeZone.getDefault().getOffset(startTime)));
        testData.add(new TrackerMotion(-1,accountId,"",
                startTime.plusMinutes(1).getMillis(),11, DateTimeZone.getDefault().getOffset(startTime.plusMinutes(1))));

        this.trackerMotionDAODynamoDB.setTrackerMotions(accountId, testData);
        final ImmutableList<TrackerMotion> actual = this.trackerMotionDAODynamoDB.getBetween(accountId, startTime.minusDays(1), startTime.plusMinutes(1));

        assertThat(actual, containsInAnyOrder(testData.toArray(new TrackerMotion[0])));
        assertThat(actual.size(), is(testData.size()));

        final ArrayList<TrackerMotion> testDataCrossDays = new ArrayList<TrackerMotion>();
        testDataCrossDays.addAll(testData);
        testDataCrossDays.add(new TrackerMotion(-1,accountId,"",
                startTime.plusDays(1).plusMinutes(1).getMillis(),12, DateTimeZone.getDefault().getOffset(startTime.plusDays(1).plusMinutes(1))));
        this.trackerMotionDAODynamoDB.setTrackerMotions(accountId, testDataCrossDays);

        final ImmutableList<TrackerMotion> actualForCrossDayTest = this.trackerMotionDAODynamoDB.getBetween(accountId, startTime.minusDays(1), startTime.plusDays(1).plusMinutes(1));

        assertThat(actualForCrossDayTest, containsInAnyOrder(testDataCrossDays.toArray()));
        assertThat(testDataCrossDays.size(), is(actualForCrossDayTest.size()));

        final ImmutableList<TrackerMotion> actualJustQueryOneDay = this.trackerMotionDAODynamoDB.getBetween(accountId, startTime, startTime);

        final TrackerMotion[] arrayHasOnlyFirstData = new TrackerMotion[]{ testData.get(0) };
        assertThat(actualJustQueryOneDay, containsInAnyOrder(arrayHasOnlyFirstData));
        assertThat(arrayHasOnlyFirstData.length, is(actualJustQueryOneDay.size()));

    }

    @Test
    public void testGetTrackerMotionForDates(){
        final Map<DateTime, List<TrackerMotion>> testData = new HashMap<DateTime, List<TrackerMotion>>();
        final DateTime startTime = DateTime.now().withTimeAtStartOfDay();
        long accountId = 1;

        final List<TrackerMotion> dataForDay1 = new ArrayList<TrackerMotion>();
        dataForDay1.add(new TrackerMotion(-1, accountId, "",
                startTime.getMillis(), 10,
                DateTimeZone.getDefault().getOffset(startTime)));
        dataForDay1.add(new TrackerMotion(-1,accountId,"",
                startTime.plusMinutes(1).getMillis(),11,
                DateTimeZone.getDefault().getOffset(startTime.plusMinutes(1))));

        testData.put(startTime, dataForDay1);

        this.trackerMotionDAODynamoDB.setTrackerMotions(accountId, dataForDay1);

        final List<TrackerMotion> dataForDay2 = new ArrayList<TrackerMotion>();
        dataForDay2.add(new TrackerMotion(-1, accountId, "",
                startTime.plusDays(1).plusMinutes(1).getMillis(), 12,
                DateTimeZone.getDefault().getOffset(startTime.plusDays(1).plusMinutes(1))));

        this.trackerMotionDAODynamoDB.setTrackerMotions(accountId, dataForDay2);

        final List<DateTime> dates = new ArrayList<DateTime>();
        dates.add(startTime);

        ImmutableMap<DateTime, List<TrackerMotion>> actual = this.trackerMotionDAODynamoDB.getTrackerMotionForDates(accountId, dates);

        for(final DateTime targetDate:dates) {
            assertThat(actual.get(targetDate), containsInAnyOrder(testData.get(targetDate).toArray()));
            assertThat(actual.get(targetDate).size(), is(testData.get(targetDate).size()));
        }

        testData.put(startTime.plusDays(1), dataForDay2);
        dates.add(startTime.plusDays(1));


        final ImmutableList<TrackerMotion> actualDebug = this.trackerMotionDAODynamoDB.getTrackerMotionForDate(accountId, startTime);
        assertThat(actualDebug, containsInAnyOrder(testData.get(startTime).toArray()));


        actual = this.trackerMotionDAODynamoDB.getTrackerMotionForDates(accountId, dates);

        for(final DateTime targetDate:dates) {
            assertThat(actual.get(targetDate), containsInAnyOrder(testData.get(targetDate).toArray()));
            assertThat(actual.get(targetDate).size(), is(testData.get(targetDate).size()));
        }

        dates.add(startTime.plusDays(100));
        testData.put(startTime.plusDays(100), Collections.<TrackerMotion>emptyList());
        actual = this.trackerMotionDAODynamoDB.getTrackerMotionForDates(accountId, dates);

        for(final DateTime targetDate:dates) {
            assertThat(actual.get(targetDate), containsInAnyOrder(testData.get(targetDate).toArray()));
            assertThat(actual.get(targetDate).size(), is(testData.get(targetDate).size()));
        }


    }


    @Test
    public void testCompactness(){
        InputProtos.TrackerDataBatch.Builder builder = InputProtos.TrackerDataBatch.newBuilder();
        final DateTime startTime = DateTime.now().withTimeAtStartOfDay();
        for(int i = 0; i < 24 * 60; i++){
            final DateTime currentTime = startTime.plusMinutes(i);
            InputProtos.TrackerDataBatch.TrackerData trackerData = InputProtos.TrackerDataBatch.TrackerData.newBuilder()
                    .setTimestamp(currentTime.getMillis())
                    .setOffsetMillis(Integer.MIN_VALUE)  // Store negative value need more space ??
                    .setSvmNoGravity(Integer.MIN_VALUE)
                    .build();
            builder.addSamples(trackerData);
        }

        final InputProtos.TrackerDataBatch trackerDataBatch = builder.build();

        final ByteBuffer byteBuffer = ByteBuffer.wrap(trackerDataBatch.toByteArray());
        System.out.println(byteBuffer.array().length);

        int maxDataSizePerAttribute = 64 * 1024 * 1024; // The max dtaa size imposed by DynamoDB
        assertThat(byteBuffer.array().length, lessThan(maxDataSizePerAttribute));

    }
}
