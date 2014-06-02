package com.hello.suripu.core.db;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 5/30/14.
 */
public class TrackerMotionDAODynamoDBTest {

    private AWSCredentialsProvider awsCredentialsProvider;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private TrackerMotionDAODynamoDB trackerMotionDAODynamoDB;

    @Before
    public void setUp(){
        this.awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentialsProvider);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        // TODO; set region here?
        final String tableName = "tracker_motion_test";

        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(tableName);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){

        }


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

        this.amazonDynamoDBClient.createTable(request);

        this.trackerMotionDAODynamoDB = new TrackerMotionDAODynamoDB(
                this.amazonDynamoDBClient,
                tableName
        );
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
        final ImmutableList<TrackerMotion> actual = this.trackerMotionDAODynamoDB.getBetween(accountId, startTime.minusDays(1), startTime.plusDays(1));

        assertThat(actual, containsInAnyOrder(testData.toArray(new TrackerMotion[0])));
        assertThat(actual.size(), is(testData.size()));

        final ArrayList<TrackerMotion> testDataCrossDays = new ArrayList<TrackerMotion>();
        testDataCrossDays.addAll(testData);
        testDataCrossDays.add(new TrackerMotion(-1,accountId,"",
                startTime.plusDays(1).plusMinutes(1).getMillis(),12, DateTimeZone.getDefault().getOffset(startTime.plusDays(1).plusMinutes(1))));
        this.trackerMotionDAODynamoDB.setTrackerMotions(accountId, testDataCrossDays);

        final ImmutableList<TrackerMotion> actualForCrossDayTest = this.trackerMotionDAODynamoDB.getBetween(accountId, startTime.minusDays(1), startTime.plusDays(2));

        assertThat(actualForCrossDayTest, containsInAnyOrder(testDataCrossDays.toArray()));
        assertThat(testDataCrossDays.size(), is(actualForCrossDayTest.size()));

        final ImmutableList<TrackerMotion> actualJustQueryOneDay = this.trackerMotionDAODynamoDB.getBetween(accountId, startTime, startTime);

        assertThat(actualJustQueryOneDay, containsInAnyOrder(testData.toArray()));
        assertThat(testData.size(), is(actualJustQueryOneDay.size()));

    }
}
