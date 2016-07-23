package com.hello.suripu.core.db;

import com.amazonaws.*;
import com.amazonaws.auth.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.insights.*;
import com.hello.suripu.core.insights.InsightsLastSeen;
import com.hello.suripu.core.models.Insights.InsightCard;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.*;
import org.slf4j.*;

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
       // assertThat(insertResult, is(true));

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
        final InsightCard.Category category1 = InsightCard.Category.SLEEP_HYGIENE;
        final InsightCard.Category category2 = InsightCard.Category.SLEEP_DURATION;
        final InsightCard.Category category3 = InsightCard.Category.AIR_QUALITY;

        ImmutableList<InsightsLastSeen> results = insightsLastSeenDynamoDB.getAll(accoundId);
        assertThat(results.size(), is(1));

        Boolean insertResult;
        insertResult = insightsLastSeenDynamoDB.markLastSeen(new InsightsLastSeen(accoundId, category1, dateTime.minusDays(2)));
        assertThat(insertResult, is(true));

        insertResult = insightsLastSeenDynamoDB.markLastSeen(new InsightsLastSeen(accoundId, category2, dateTime.minusDays(1)));
        assertThat(insertResult, is(true));

        insertResult = insightsLastSeenDynamoDB.markLastSeen(new InsightsLastSeen(accoundId, category2, dateTime));
        assertThat(insertResult, is(true));

        results = insightsLastSeenDynamoDB.getAll(accoundId);
        assertThat(results.size(), is(2));
        assertThat(results.get(0).seenCategory, is(category1));
        assertThat(results.get(0).updatedUTC, is(dateTime.minusDays(2)));
        assertThat(results.get(1).seenCategory, is(category2));
        assertThat(results.get(1).updatedUTC, is(dateTime));

        Optional<InsightsLastSeen> result = insightsLastSeenDynamoDB.getFor(accoundId,category1);
        assertThat(result.get().seenCategory, is(category1));
        assertThat(result.get().updatedUTC, is(dateTime.minusDays(2)));


        result  = insightsLastSeenDynamoDB.getFor(accoundId,category3);
        assertThat(result.isPresent(), is(false));


    }

}
