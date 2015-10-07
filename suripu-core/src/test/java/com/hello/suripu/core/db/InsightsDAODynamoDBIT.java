package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.hello.suripu.core.models.Insights.InsightCard;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by jakepiccolo on 10/7/15.
 */
public class InsightsDAODynamoDBIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(InsightsDAODynamoDBIT.class);

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private InsightsDAODynamoDB insightsDAODynamoDB;

    private static final String VERSION = "v_0_1";
    private static final String TABLE_NAME = "integration_test_insights";


    @Before
    public void setUp() throws Exception {
        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        tearDown();

        try {
            LOGGER.debug("-------- Creating Table {} ---------", TABLE_NAME);
            final CreateTableResult result = InsightsDAODynamoDB.createTable(TABLE_NAME, amazonDynamoDBClient);
            LOGGER.debug("Created dynamoDB table {}", result.getTableDescription());
        } catch (ResourceInUseException rie){
            LOGGER.warn("Problem creating table");
        }
        this.insightsDAODynamoDB = new InsightsDAODynamoDB(amazonDynamoDBClient, TABLE_NAME);
    }

    @After
    public void tearDown() throws Exception {
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(TABLE_NAME);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            LOGGER.warn("Can not delete non existing table");
        }
    }

    private void insertInsight(final List<InsightCard> insightCards, Long accountId, final String date) {
        final String title = "title";
        final String message = "message";
        final InsightCard.Category category = InsightCard.Category.LIGHT;
        final InsightCard.TimePeriod timePeriod = InsightCard.TimePeriod.NONE;
        insightCards.add(new InsightCard(accountId, title, message, category, timePeriod, DateTime.parse(date)));
    }

    @Test
    public void testGetInsightsByDate() throws Exception {
        // Set up some test data in the table
        final List<InsightCard> insightCards = new ArrayList<>();
        final Long accountId = new Long(0);
        insertInsight(insightCards, accountId, "2015-05-06T23:17:25.162Z");
        insertInsight(insightCards, accountId, "2015-05-05T23:17:25.162Z");
        insertInsight(insightCards, accountId, "2015-05-04T20:17:25.162Z");
        insertInsight(insightCards, accountId, "2015-05-03T01:17:25.162Z");
        insertInsight(insightCards, accountId, "2015-05-02T01:17:25.162Z");
        insertInsight(insightCards, accountId, "2015-05-01T01:17:25.162Z");

        insightsDAODynamoDB.insertListOfInsights(insightCards);

        final int limit = 4;

        // Test reverse chronological order
        final List<InsightCard> insightsReverseChronological = insightsDAODynamoDB.getInsightsByDate(accountId, DateTime.parse("2015-05-07T23:17:25.162Z"), false, limit);
        assertThat(insightsReverseChronological.size(), is(limit));
        assertThat(insightsReverseChronological.get(0).timestamp, is(DateTime.parse("2015-05-06T23:17:25.162Z")));
        assertThat(insightsReverseChronological.get(2).timestamp, is(DateTime.parse("2015-05-04T20:17:25.162Z")));

        // Chronological order
        final List<InsightCard> insightsChronological = insightsDAODynamoDB.getInsightsByDate(accountId, DateTime.parse("2012-01-01T00:00:25.162Z"), true, limit);
        assertThat(insightsChronological.size(), is(limit));
        for (final InsightCard card: insightsChronological) {
            LOGGER.debug("dafuq");
            LOGGER.debug(card.timestamp.toString());
        }
        assertThat(insightsChronological.get(0).timestamp, is(DateTime.parse("2015-05-04T20:17:25.162Z")));
        assertThat(insightsChronological.get(3).timestamp, is(DateTime.parse("2015-05-01T01:17:25.162Z")));
    }
}