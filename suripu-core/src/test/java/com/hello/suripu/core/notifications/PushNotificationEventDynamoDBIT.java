package com.hello.suripu.core.notifications;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jakepiccolo on 5/3/16.
 */
public class PushNotificationEventDynamoDBIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(PushNotificationEventDynamoDBIT.class);

    private BasicAWSCredentials awsCredentials;
    private final ClientConfiguration clientConfiguration = new ClientConfiguration();
    private final String endpoint = "http://localhost:7777";
    private AmazonDynamoDB amazonDynamoDBClient;
    private PushNotificationEventDynamoDB dao;

    private static final String TABLE_PREFIX = "integration_test_push_event";
    private static final String TABLE_NAME_2016 = TABLE_PREFIX + "_2016";
    private static final String TABLE_NAME_2017 = TABLE_PREFIX + "_2017";



    //region setUp/tearDown

    @Before
    public void setUp() throws Exception {
        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");
        this.dao = new PushNotificationEventDynamoDB(amazonDynamoDBClient, TABLE_PREFIX);

        tearDown();
        final List<String> tables = ImmutableList.of(TABLE_NAME_2016, TABLE_NAME_2017);

        try {
            for (final String tableName : tables) {
                LOGGER.debug("-------- Creating Table {} ---------", tableName);
                final CreateTableResult result = dao.createTable(tableName);
                LOGGER.debug("Created dynamoDB table {}", result.getTableDescription());
            }
        } catch (ResourceInUseException rie){
            LOGGER.warn("Problem creating table");
        }
    }

    @After
    public void tearDown() throws Exception {
        final List<String> tables = ImmutableList.of(TABLE_NAME_2016, TABLE_NAME_2017);
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
    public void testGetTableName() throws Exception {
        final DateTime oct_2016 = new DateTime(2016, 10, 24, 0, 0, DateTimeZone.UTC);
        assertThat(dao.getTableName(oct_2016), is(TABLE_NAME_2016));

        final DateTime jan_2016 = new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC);
        assertThat(dao.getTableName(jan_2016), is(TABLE_NAME_2016));

        final DateTime oct_2017 = new DateTime(2017, 10, 24, 0, 0, DateTimeZone.UTC);
        assertThat(dao.getTableName(oct_2017), is(TABLE_NAME_2017));
    }



    //region insert

    @Test
    public void testInsertHappyPath() throws Exception {
        final DateTime dateTime = new DateTime(2016, 10, 24, 0, 0, DateTimeZone.UTC);
        final PushNotificationEvent event = PushNotificationEvent.newBuilder()
                .withAccountId(1L)
                .withSenseId("senseId")
                .withTimestamp(dateTime)
                .withType("insight")
                .withHelloPushMessage(new HelloPushMessage("body", "target", "details"))
                .build();
        final boolean result = dao.insert(event);
        assertThat(result, is(true));
    }

    private void setAmazonDynamoDBClient(final AmazonDynamoDB client) {
        client.setEndpoint(endpoint);
        dao = new PushNotificationEventDynamoDB(client, TABLE_PREFIX);
    }

    @Test
    public void testInsertThrottled() throws Exception {
        final DateTime dateTime = new DateTime(2016, 10, 24, 0, 0, DateTimeZone.UTC);
        final PushNotificationEvent event = PushNotificationEvent.newBuilder()
                .withAccountId(1L)
                .withSenseId("senseId")
                .withTimestamp(dateTime)
                .withType("insight")
                .withHelloPushMessage(new HelloPushMessage("body", "target", "details"))
                .build();

        final AmazonDynamoDB throttlingClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration) {
            int numTries = 0;
            @Override
            public PutItemResult putItem(String tableName, Map<String, AttributeValue> item) {
                // Throttle the first two tries
                if (numTries < 2) {
                    numTries++;
                    throw new ProvisionedThroughputExceededException("exceeded throughput lolol");
                }
                return super.putItem(tableName, item);
            }
        };
        setAmazonDynamoDBClient(throttlingClient);

        final boolean result = dao.insert(event);
        assertThat(result, is(true));
    }

    @Test
    public void testInsertThrottledTooMuch() throws Exception {
        final DateTime dateTime = new DateTime(2016, 10, 24, 0, 0, DateTimeZone.UTC);
        final PushNotificationEvent event = PushNotificationEvent.newBuilder()
                .withAccountId(1L)
                .withSenseId("senseId")
                .withTimestamp(dateTime)
                .withType("insight")
                .withHelloPushMessage(new HelloPushMessage("body", "target", "details"))
                .build();

        final AmazonDynamoDB throttlingClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration) {
            @Override
            public PutItemResult putItem(final PutItemRequest putItemRequest) {
                // Always throttle
                throw new ProvisionedThroughputExceededException("exceeded throughput lolol");
            }
        };
        setAmazonDynamoDBClient(throttlingClient);

        final boolean result = dao.insert(event);
        assertThat(result, is(false));
    }

    @Test
    public void testInsertInternalServerError() throws Exception {
        final DateTime dateTime = new DateTime(2016, 10, 24, 0, 0, DateTimeZone.UTC);
        final PushNotificationEvent event = PushNotificationEvent.newBuilder()
                .withAccountId(1L)
                .withSenseId("senseId")
                .withTimestamp(dateTime)
                .withType("insight")
                .withHelloPushMessage(new HelloPushMessage("body", "target", "details"))
                .build();

        final AmazonDynamoDB stupidClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration) {
            @Override
            public PutItemResult putItem(final PutItemRequest putItemRequest) {
                // Always throttle
                throw new InternalServerErrorException("ruh roh");
            }
        };
        setAmazonDynamoDBClient(stupidClient);

        final boolean result = dao.insert(event);
        assertThat(result, is(false));
    }

    @Test
    public void testInsertAlreadyExists() throws Exception {
        final DateTime dateTime = new DateTime(2016, 10, 24, 0, 0, DateTimeZone.UTC);
        final PushNotificationEvent event = PushNotificationEvent.newBuilder()
                .withAccountId(1L)
                .withSenseId("senseId")
                .withTimestamp(dateTime)
                .withType("insight")
                .withHelloPushMessage(new HelloPushMessage("body", "target", "details"))
                .build();

        final boolean firstResult = dao.insert(event);
        assertThat(firstResult, is(true));

        // It should fail to insert the second time.
        final boolean secondResult = dao.insert(event);
        assertThat(secondResult, is(false));
    }

    //endregion insert



    @Test
    public void testQuery() throws Exception {
        final Long account1 = 1L;
        final Long account2 = 2L;
        final DateTime startTime = new DateTime(2016, 10, 24, 0, 0, DateTimeZone.UTC);
        final String sense1 = "sense1";
        final String insight = "insight";
        final String pillBattery = "pillBattery";
        final String senseStatus = "senseStatus";
        final String pillStatus = "pillStatus";

        final List<PushNotificationEvent> account1Events = new ArrayList<>();
        account1Events.add(PushNotificationEvent.newBuilder()
                .withAccountId(account1)
                .withTimestamp(startTime)
                .withHelloPushMessage(new HelloPushMessage("body1", "target1", "details1"))
                .withType(insight)
                .build());
        account1Events.add(PushNotificationEvent.newBuilder()
                .withAccountId(account1)
                .withTimestamp(startTime.plusHours(1))
                .withHelloPushMessage(new HelloPushMessage("body2", "target2", "details2"))
                .withType(senseStatus)
                .withSenseId(sense1)
                .build());
        account1Events.add(PushNotificationEvent.newBuilder()
                .withAccountId(account1)
                .withTimestamp(startTime.plusMonths(5)) // 2017 now
                .withHelloPushMessage(new HelloPushMessage("body3", "target3", "details3"))
                .withType(pillBattery)
                .build());

        final List<PushNotificationEvent> account2Events = new ArrayList<>();
        account2Events.add(PushNotificationEvent.newBuilder()
                .withAccountId(account2)
                .withTimestamp(startTime)
                .withHelloPushMessage(new HelloPushMessage("body4", "target4", "details4"))
                .withType(pillStatus)
                .build());

        final List<PushNotificationEvent> allEvents = ImmutableList.<PushNotificationEvent>builder()
                .addAll(account1Events)
                .addAll(account2Events).build();
        for (final PushNotificationEvent event: allEvents) {
            dao.insert(event);
        }

        assertThat(dao.query(account1, startTime.minusHours(12), startTime.minusHours(1)).data.isEmpty(), is(true));
        assertThat(dao.query(account1, startTime, startTime.plusMinutes(30)).data, is(account1Events.subList(0, 1)));
        assertThat(dao.query(account1, startTime, startTime.plusDays(1)).data, is(account1Events.subList(0, 2)));
        assertThat(dao.query(account1, startTime, startTime.plusYears(1)).data, is(account1Events));
        assertThat(dao.query(account1, startTime.plusYears(1), startTime.plusYears(2)).data.isEmpty(), is(true));
        assertThat(dao.query(account2, startTime, startTime.plusMinutes(60)).data, is(account2Events));
        assertThat(dao.query(-1L, startTime, startTime.plusYears(2)).data.isEmpty(), is(true));
    }

    @Test
    public void testQueryForType() throws Exception {
        final Long account1 = 1L;
        final DateTime startTime = new DateTime(2016, 10, 24, 0, 0, DateTimeZone.UTC);
        final String sense1 = "sense1";
        final String insight = "insight";
        final String pillBattery = "pillBattery";
        final String senseStatus = "senseStatus";
        final String pillStatus = "pillStatus";

        final List<PushNotificationEvent> account1Events = new ArrayList<>();
        account1Events.add(PushNotificationEvent.newBuilder()
                .withAccountId(account1)
                .withTimestamp(startTime)
                .withHelloPushMessage(new HelloPushMessage("body1", "target1", "details1"))
                .withType(insight)
                .build());
        account1Events.add(PushNotificationEvent.newBuilder()
                .withAccountId(account1)
                .withTimestamp(startTime.plusHours(1))
                .withHelloPushMessage(new HelloPushMessage("body2", "target2", "details2"))
                .withType(senseStatus)
                .withSenseId(sense1)
                .build());
        account1Events.add(PushNotificationEvent.newBuilder()
                .withAccountId(account1)
                .withTimestamp(startTime.plusMonths(5)) // 2017 now
                .withHelloPushMessage(new HelloPushMessage("body3", "target3", "details3"))
                .withType(pillBattery)
                .build());
        for (final PushNotificationEvent event: account1Events) {
            dao.insert(event);
        }

        assertThat(dao.query(account1, startTime, startTime.plusYears(2), insight).data,
                is(account1Events.subList(0, 1)));
        assertThat(dao.query(account1, startTime, startTime.plusYears(2), senseStatus).data,
                is(account1Events.subList(1, 2)));
        assertThat(dao.query(account1, startTime, startTime.plusYears(2), pillBattery).data,
                is(account1Events.subList(2, 3)));
        assertThat(dao.query(account1, startTime, startTime.plusMinutes(30), senseStatus).data.isEmpty(),
                is(true));
    }
}
