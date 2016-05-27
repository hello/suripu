package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.MultiDensityImage;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
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

    private void insertInsight(final List<InsightCard> insightCards,
                               final Long accountId,
                               final String date, Optional<MultiDensityImage> image) {
        final String title = "title";
        final String message = "message";
        final InsightCard.Category category = InsightCard.Category.LIGHT;
        final InsightCard.TimePeriod timePeriod = InsightCard.TimePeriod.NONE;
        insightCards.add(new InsightCard(accountId, title, message, category, timePeriod,
                DateTime.parse(date), Optional.<String>absent(), image,
                InsightCard.InsightType.DEFAULT, Optional.<UUID>absent()));
    }

    @Test
    public void testGetInsightsByDate() throws Exception {
        // Set up some test data in the table
        final List<InsightCard> insightCards = new ArrayList<>();
        final Long accountId = 0L;
        insertInsight(insightCards, accountId, "2015-05-06T23:17:25.162Z", Optional.<MultiDensityImage>absent());
        insertInsight(insightCards, accountId, "2015-05-05T23:17:25.162Z", Optional.<MultiDensityImage>absent());
        insertInsight(insightCards, accountId, "2015-05-04T20:17:25.162Z", Optional.<MultiDensityImage>absent());
        insertInsight(insightCards, accountId, "2015-05-03T01:17:25.162Z", Optional.<MultiDensityImage>absent());
        insertInsight(insightCards, accountId, "2015-05-02T01:17:25.162Z", Optional.<MultiDensityImage>absent());
        insertInsight(insightCards, accountId, "2015-05-01T01:17:25.162Z", Optional.<MultiDensityImage>absent());

        insightsDAODynamoDB.insertListOfInsights(insightCards);

        final int limit = 4;

        // Test reverse chronological order
        final List<InsightCard> insightsReverseChronological = insightsDAODynamoDB.getInsightsByDate(accountId, DateTime.parse("2015-05-07T23:17:25.162Z"), false, limit);
        assertThat(insightsReverseChronological.size(), is(limit));
        assertThat(insightsReverseChronological.get(0).timestamp, is(DateTime.parse("2015-05-06T23:17:25.162Z")));
        assertThat(insightsReverseChronological.get(0).image.isPresent(), is(false));
        assertThat(insightsReverseChronological.get(2).timestamp, is(DateTime.parse("2015-05-04T20:17:25.162Z")));
        assertThat(insightsReverseChronological.get(2).image.isPresent(), is(false));

        // Chronological order
        final List<InsightCard> insightsChronological = insightsDAODynamoDB.getInsightsByDate(accountId, DateTime.parse("2012-01-01T00:00:25.162Z"), true, limit);
        assertThat(insightsChronological.size(), is(limit));
        for (final InsightCard card: insightsChronological) {
            LOGGER.debug("dafuq");
            LOGGER.debug(card.timestamp.toString());
        }
        assertThat(insightsChronological.get(0).timestamp, is(DateTime.parse("2015-05-04T20:17:25.162Z")));
        assertThat(insightsChronological.get(0).image.isPresent(), is(false));
        assertThat(insightsChronological.get(3).timestamp, is(DateTime.parse("2015-05-01T01:17:25.162Z")));
        assertThat(insightsChronological.get(3).image.isPresent(), is(false));
    }

    @Test
    public void testGetInsightsByDateWithImages() {
        final List<InsightCard> insightCards = new ArrayList<>();
        final Long accountId = 0L;
        final MultiDensityImage completeImage = new MultiDensityImage(Optional.of("http://hellocdn.net/insights/images/test@1x.png"),
                                                                      Optional.of("http://hellocdn.net/insights/images/test@2x.png"),
                                                                      Optional.of("http://hellocdn.net/insights/images/test@3x.png"));
        insertInsight(insightCards, accountId, "2015-05-06T23:17:25.162Z", Optional.of(completeImage));

        final MultiDensityImage incompleteImage = new MultiDensityImage(Optional.of("http://hellocdn.net/insights/images/test@1x.png"),
                                                                        Optional.<String>absent(),
                                                                        Optional.of("http://hellocdn.net/insights/images/test@3x.png"));
        insertInsight(insightCards, accountId, "2015-05-05T23:17:25.162Z", Optional.of(incompleteImage));

        insightsDAODynamoDB.insertListOfInsights(insightCards);

        final int limit = 2;
        final List<InsightCard> insights = insightsDAODynamoDB.getInsightsByDate(accountId, DateTime.parse("2015-05-07T23:17:25.162Z"), false, limit);

        assertThat(insights.size(), is(equalTo(2)));

        final InsightCard withCompleteImage = insights.get(0);
        assertThat(withCompleteImage.image.isPresent(), is(true));

        final MultiDensityImage fromDbCompleteImage = withCompleteImage.image.get();
        assertThat(fromDbCompleteImage.phoneDensityNormal.isPresent(), is(true));
        assertThat(fromDbCompleteImage.phoneDensityNormal, is(equalTo(completeImage.phoneDensityNormal)));

        assertThat(fromDbCompleteImage.phoneDensityHigh.isPresent(), is(true));
        assertThat(fromDbCompleteImage.phoneDensityHigh, is(equalTo(completeImage.phoneDensityHigh)));

        assertThat(fromDbCompleteImage.phoneDensityExtraHigh.isPresent(), is(true));
        assertThat(fromDbCompleteImage.phoneDensityExtraHigh, is(equalTo(completeImage.phoneDensityExtraHigh)));

        final InsightCard withIncompleteImage = insights.get(1);
        assertThat(withIncompleteImage.image.isPresent(), is(true));

        final MultiDensityImage fromDbIncompleteImage = withIncompleteImage.image.get();
        assertThat(fromDbIncompleteImage.phoneDensityNormal.isPresent(), is(true));
        assertThat(fromDbIncompleteImage.phoneDensityNormal, is(equalTo(completeImage.phoneDensityNormal)));

        assertThat(fromDbIncompleteImage.phoneDensityHigh.isPresent(), is(false));

        assertThat(fromDbIncompleteImage.phoneDensityExtraHigh.isPresent(), is(true));
        assertThat(fromDbIncompleteImage.phoneDensityExtraHigh, is(equalTo(completeImage.phoneDensityExtraHigh)));
    }


    @Test
    public void testHasUUID() {
        final String title = "title";
        final String message = "message";
        final InsightCard.Category category = InsightCard.Category.LIGHT;
        final InsightCard.TimePeriod timePeriod = InsightCard.TimePeriod.NONE;
        final String date = "2016-05-06T23:17:25.162Z";
        final InsightCard card = new InsightCard(1L, title, message, category, timePeriod,
                DateTime.parse(date), Optional.<String>absent(), Optional.<MultiDensityImage>absent(), InsightCard.InsightType.DEFAULT,
                Optional.<UUID>absent());

        insightsDAODynamoDB.insertInsight(card);
        final ImmutableList<InsightCard> retrievedCards = insightsDAODynamoDB.getInsightsByCategory(1L, InsightCard.Category.LIGHT, 1);
        assertThat(retrievedCards.size(), is (1));
        assertThat(retrievedCards.get(0).uuid.isPresent(), is(true));
    }

    @Test
    public void testHasNoUUID() {
        final String title = "title";
        final String message = "message";
        final InsightCard.Category category = InsightCard.Category.AIR_QUALITY;
        final InsightCard.TimePeriod timePeriod = InsightCard.TimePeriod.NONE;
        final String date = "2016-05-07T00:17:25.162Z";
        final InsightCard card = new InsightCard(1L, title, message, category, timePeriod,
                DateTime.parse(date), Optional.<String>absent(), Optional.<MultiDensityImage>absent(), InsightCard.InsightType.DEFAULT,
                Optional.<UUID>absent());

        insightsDAODynamoDB.insertInsightWithoutUUID(card);
        final ImmutableList<InsightCard> retrievedCards = insightsDAODynamoDB.getInsightsByCategory(1L, InsightCard.Category.AIR_QUALITY, 1);
        assertThat(retrievedCards.size(), is (1));
        assertThat(retrievedCards.get(0).uuid.isPresent(), is(false));
    }

}