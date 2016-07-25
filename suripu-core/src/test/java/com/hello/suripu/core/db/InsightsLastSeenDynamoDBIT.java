package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.insights.InsightsLastSeen;
import com.hello.suripu.core.insights.InsightsLastSeenDynamoDB;
import com.hello.suripu.core.models.Insights.InsightCard;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
/**
 * Created by jarredheinrich on 7/21/16.
 */
public class InsightsLastSeenDynamoDBIT{
        private final static Logger LOGGER = LoggerFactory.getLogger(AggregateSleepScoreDAODynamoDBIT.class);

        private BasicAWSCredentials awsCredentials;
        private AmazonDynamoDBClient amazonDynamoDBClient;
        private InsightsLastSeenDynamoDB insightsLastSeenDynamoDB;
        private final String tableName = "test_insights_last_seen";


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
                this.insightsLastSeenDynamoDB = InsightsLastSeenDynamoDB.create(this.amazonDynamoDBClient, tableName);
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
    public void testUpdateAndGetInsight() throws Exception {
        final Long accoundId = 1L;
        final DateTime dateTimePast = DateTime.now(DateTimeZone.UTC).minusDays(1);
        final DateTime dateTimeNow = DateTime.now(DateTimeZone.UTC);
        final InsightCard.Category category = InsightCard.Category.SLEEP_HYGIENE;
        InsightsLastSeen insightLastSeen= new InsightsLastSeen(accoundId, category, dateTimePast);

        Boolean insertResult = insightsLastSeenDynamoDB.markLastSeen(insightLastSeen);
        assertThat(insertResult, is(true));

        ImmutableList<InsightsLastSeen> retrievedInsights= insightsLastSeenDynamoDB.getAll(accoundId);
        assertThat(retrievedInsights.get(0).updatedUTC, is(dateTimePast));
        assertThat(retrievedInsights.get(0).seenCategory, is(category));
        assertThat(retrievedInsights.get(0).accountId, is(accoundId));


        insightLastSeen= new InsightsLastSeen(accoundId, category, dateTimeNow);
        insertResult = insightsLastSeenDynamoDB.markLastSeen(insightLastSeen);
        assertThat(insertResult, is(true));

        retrievedInsights = insightsLastSeenDynamoDB.getAll(accoundId);
        assertThat(retrievedInsights.get(0).updatedUTC, is(dateTimeNow));
        assertThat(retrievedInsights.get(0).seenCategory, is(category));
        assertThat(retrievedInsights.get(0).accountId, is(accoundId));
    }
    @Test
    public void testGetLatestInsights() throws Exception {
        final Long accoundId = 1L;
        final DateTime dateTime = DateTime.now(DateTimeZone.UTC);
        final InsightCard.Category category1 = InsightCard.Category.GENERIC;
        final InsightCard.Category category2 = InsightCard.Category.SLEEP_HYGIENE;
        final InsightCard.Category category3 = InsightCard.Category.LIGHT;
        final InsightCard.Category category4 = InsightCard.Category.SOUND;
        final InsightCard.Category category5 = InsightCard.Category.TEMPERATURE;
        final InsightCard.Category category6 = InsightCard.Category.HUMIDITY;

        //with no results.
        ImmutableList<InsightsLastSeen> results = insightsLastSeenDynamoDB.getAll(accoundId);
        assertThat(results.size(), is(0));

        Boolean insertResult;
        insertResult = insightsLastSeenDynamoDB.markLastSeen(new InsightsLastSeen(accoundId, category1, dateTime.minusDays(2)));
        assertThat(insertResult, is(true));
        insertResult = insightsLastSeenDynamoDB.markLastSeen(new InsightsLastSeen(accoundId, category2, dateTime.minusDays(1)));
        assertThat(insertResult, is(true));
        insertResult = insightsLastSeenDynamoDB.markLastSeen(new InsightsLastSeen(accoundId, category2, dateTime));
        assertThat(insertResult, is(true));
        insertResult = insightsLastSeenDynamoDB.markLastSeen(new InsightsLastSeen(accoundId, category3, dateTime));
        assertThat(insertResult, is(true));
        insertResult = insightsLastSeenDynamoDB.markLastSeen(new InsightsLastSeen(accoundId, category4, dateTime));
        assertThat(insertResult, is(true));
        insertResult = insightsLastSeenDynamoDB.markLastSeen(new InsightsLastSeen(accoundId, category5, dateTime));
        assertThat(insertResult, is(true));


        results = insightsLastSeenDynamoDB.getAll(accoundId);
        assertThat(results.size(), is(5));
        assertThat(results.get(0).seenCategory, is(category1));
        assertThat(results.get(0).updatedUTC, is(dateTime.minusDays(2)));
        assertThat(results.get(1).seenCategory, is(category2));
        assertThat(results.get(1).updatedUTC, is(dateTime));
        assertThat(results.get(2).seenCategory, is(category3));
        assertThat(results.get(3).seenCategory, is(category4));
        assertThat(results.get(4).seenCategory, is(category5));


        Optional<InsightsLastSeen> result = insightsLastSeenDynamoDB.getFor(accoundId,category1);
        assertThat(result.get().seenCategory, is(category1));
        assertThat(result.get().updatedUTC, is(dateTime.minusDays(2)));


        result  = insightsLastSeenDynamoDB.getFor(accoundId,category6);
        assertThat(result.isPresent(), is(false));

    }

}
