package com.hello.suripu.core.analytics;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDBIT;
import com.hello.suripu.core.insights.InsightsLastSeenDynamoDB;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AnalyticsTrackingDynamoDBIT  {

    private final static Logger LOGGER = LoggerFactory.getLogger(AggregateSleepScoreDAODynamoDBIT.class);

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private AnalyticsTrackingDynamoDB analyticsTrackingDynamoDB;
    private final String tableName = "test_analytics_tracking";


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
            final TableDescription result = InsightsLastSeenDynamoDB.createTable(this.amazonDynamoDBClient, tableName);
            LOGGER.debug("Created dynamoDB table {}", result);
            this.analyticsTrackingDynamoDB = AnalyticsTrackingDynamoDB.create(this.amazonDynamoDBClient, tableName);
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
    public void testUpsertUnknownEvent() throws Exception {
        final boolean inserted = analyticsTrackingDynamoDB.putIfAbsent(TrackingEvent.UNKNOWN, 9999L);
        assertThat("upsert", inserted, is(false));
    }

    @Test
    public void testPutIfAbsent() throws Exception {
        final boolean inserted = analyticsTrackingDynamoDB.putIfAbsent(TrackingEvent.PILL_LOW_BATTERY, 9999L);
        assertThat("upsert", inserted, is(true));

        final boolean insertedAgain = analyticsTrackingDynamoDB.putIfAbsent(TrackingEvent.PILL_LOW_BATTERY, 9999L);
        assertThat("upsert again", insertedAgain, is(false));
    }
}
