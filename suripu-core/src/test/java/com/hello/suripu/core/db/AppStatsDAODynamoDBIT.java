package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AppStatsDAODynamoDBIT {
    private final static Logger LOGGER = LoggerFactory.getLogger(AppStatsDAODynamoDBIT.class);

    private static final long TEST_ACCOUNT_ID = 42L;
    private static final DateTime TEST_DATE_TIME_1 = new DateTime(2015, 11, 12, 13, 14, DateTimeZone.UTC);
    private static final DateTime TEST_DATE_TIME_2 = new DateTime(2015, 12, 11, 10, 9, DateTimeZone.UTC);

    private static final String TABLE_NAME = "app_stats_test";

    private AmazonDynamoDBClient amazonDynamoDBClient;
    private AppStatsDAODynamoDB appStats;

    @Before
    public void setUp() throws Exception {
        final BasicAWSCredentials awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);

        this.amazonDynamoDBClient = new AmazonDynamoDBClient(awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        tearDown();

        try {
            AppStatsDAODynamoDB.createTable(TABLE_NAME, this.amazonDynamoDBClient);
            this.appStats = new AppStatsDAODynamoDB(
                    this.amazonDynamoDBClient,
                    TABLE_NAME
            );
        } catch (ResourceInUseException rie) {
            LOGGER.warn("Table already exists");
        }
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

    @Test
    public void insightsLastViewed() throws Exception {
        final Optional<DateTime> defaultValue = appStats.getInsightsLastViewed(TEST_ACCOUNT_ID);
        assertThat(defaultValue.isPresent(), is(false));


        appStats.putInsightsLastViewed(TEST_ACCOUNT_ID, TEST_DATE_TIME_1);

        final Optional<DateTime> savedValue1 = appStats.getInsightsLastViewed(TEST_ACCOUNT_ID);
        assertThat(savedValue1.isPresent(), is(true));
        assertThat(savedValue1.get(), is(equalTo(TEST_DATE_TIME_1)));


        appStats.putInsightsLastViewed(TEST_ACCOUNT_ID, TEST_DATE_TIME_2);

        final Optional<DateTime> savedValue2 = appStats.getInsightsLastViewed(TEST_ACCOUNT_ID);
        assertThat(savedValue2.isPresent(), is(true));
        assertThat(savedValue2.get(), is(equalTo(TEST_DATE_TIME_2)));
    }

    @Test
    public void questionsLastViewed() throws Exception {
        final Optional<DateTime> defaultValue = appStats.getQuestionsLastViewed(TEST_ACCOUNT_ID);
        assertThat(defaultValue.isPresent(), is(false));


        appStats.putQuestionsLastViewed(TEST_ACCOUNT_ID, TEST_DATE_TIME_1);

        final Optional<DateTime> savedValue1 = appStats.getQuestionsLastViewed(TEST_ACCOUNT_ID);
        assertThat(savedValue1.isPresent(), is(true));
        assertThat(savedValue1.get(), is(equalTo(TEST_DATE_TIME_1)));


        appStats.putQuestionsLastViewed(TEST_ACCOUNT_ID, TEST_DATE_TIME_2);

        final Optional<DateTime> savedValue2 = appStats.getQuestionsLastViewed(TEST_ACCOUNT_ID);
        assertThat(savedValue2.isPresent(), is(true));
        assertThat(savedValue2.get(), is(equalTo(TEST_DATE_TIME_2)));
    }
}
