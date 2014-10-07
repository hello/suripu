package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.AggregateScore;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;


/**
 * Created by kingshy on 10/02/14.
 */
public class AggregateSleepScoreDAODynamoDBIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(AggregateSleepScoreDAODynamoDBIT.class);

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB;

    private static final String VERSION = "v_0_1";
    private static final String SCORE_TYPE = "sleep";
    private final String tableName = "sleep_score";


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
            final CreateTableResult result = AggregateSleepScoreDAODynamoDB.createTable(tableName + "_" + this.VERSION, this.amazonDynamoDBClient);
            LOGGER.debug("Created dynamoDB table {}", result.getTableDescription());
            this.aggregateSleepScoreDAODynamoDB = new AggregateSleepScoreDAODynamoDB(
                    this.amazonDynamoDBClient,
                    this.tableName,
                    this.VERSION
            );
        }catch (ResourceInUseException rie){
            LOGGER.warn("Problem creating table");
            rie.printStackTrace();
        }
    }

    @After
    public void cleanUp(){
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(this.tableName + "_" + this.VERSION);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            ex.printStackTrace();
        }
    }

    @Test
    public void testGetNoScore(){
        LOGGER.debug("---------- Testing no score returns ----------");
        final long accountId = 3;
        final String date = "2014-02-01";
        final AggregateScore actual = this.aggregateSleepScoreDAODynamoDB.getSingleScore(accountId, date);
        final AggregateScore expected = new AggregateScore(accountId, 0, date, this.SCORE_TYPE, this.VERSION);
        assertThat(expected, is(actual));
    }

    @Test
    public void testSingleScore() {
        LOGGER.debug("---------- Testing write single score ----------");

        final long accountId = 3;
        final String date = "2014-10-07";

        final AggregateScore score = new AggregateScore(accountId, 10, date, this.SCORE_TYPE, this.VERSION);

        this.aggregateSleepScoreDAODynamoDB.writeSingleScore(score);

        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final AggregateScore retrievedScore = this.aggregateSleepScoreDAODynamoDB.getSingleScore(accountId, date);
        assertThat(score, is(retrievedScore));

    }

    @Test
    public void testBatchScores() {
        LOGGER.debug("---------- Testing write batch scores ----------");

        final long accountId = 3;
        final DateTime startTime = DateTime.now().withTimeAtStartOfDay();
        final int numDays = 10;


        final List<AggregateScore> scores = new ArrayList<>();
        final String queryEndDate = DateTimeUtil.dateToYmdString(startTime);
        String queryStartDate = DateTimeUtil.dateToYmdString(startTime.minusDays(numDays - 1));

        Random r = new Random(numDays);
        for (int i = 0; i < numDays; i++) {
            final DateTime targetDate = startTime.minusDays(i);
            final AggregateScore score = new AggregateScore(accountId, r.nextInt(100), DateTimeUtil.dateToYmdString(targetDate), this.SCORE_TYPE, this.VERSION);
            scores.add(score);
            queryStartDate = DateTimeUtil.dateToYmdString(targetDate);
        }

        this.aggregateSleepScoreDAODynamoDB.writeBatchScores(scores);

        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // let's read the batch back
        final ImmutableList<AggregateScore> retrievedScores = this.aggregateSleepScoreDAODynamoDB.getBatchScores(
                accountId, queryStartDate, queryEndDate, numDays);

        assertThat(scores.size(), is(retrievedScores.size()));
        assertThat(scores, containsInAnyOrder(retrievedScores.toArray()));
    }

}
