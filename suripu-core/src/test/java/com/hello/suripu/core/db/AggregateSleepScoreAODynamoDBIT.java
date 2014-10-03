package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.hello.suripu.core.models.AggregateScore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


/**
 * Created by kingshy on 10/02/14.
 */
public class AggregateSleepScoreAODynamoDBIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(AggregateSleepScoreAODynamoDBIT.class);

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB;
    private final String version = "v_0_1";
    private final String tableName = "sleep_score";

    @Before
    public void setUp(){
        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        cleanUp();

        try {
            LOGGER.debug("-------- Creating Table {} ---------", tableName);
            final CreateTableResult result = AggregateSleepScoreDAODynamoDB.createTable(tableName + "_" + this.version, this.amazonDynamoDBClient);
            LOGGER.debug("Created dynamoDB table {}", result.getTableDescription());
            this.aggregateSleepScoreDAODynamoDB = new AggregateSleepScoreDAODynamoDB(
                    this.amazonDynamoDBClient,
                    this.tableName,
                    this.version
            );
        }catch (ResourceInUseException rie){
            LOGGER.warn("Problem creating table");
            rie.printStackTrace();
        }
    }


    @After
    public void cleanUp(){
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(this.tableName + "_" + this.version);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            ex.printStackTrace();
        }
    }

    @Test
    public void testGetNoScore(){
        LOGGER.debug("---------- Testing No Score ----------");
        final long accountId = 3;
        final String date = "2014-01-01";
        final AggregateScore actual = this.aggregateSleepScoreDAODynamoDB.getSingleScore(accountId, date);
        final AggregateScore expected = new AggregateScore(accountId, 0, "You haven't been sleeping", date, "sleep", this.version);
        assertThat(expected.accountId, is(actual.accountId));
        assertThat(expected.score, is(actual.score));
        assertThat(expected.date, is(actual.date));
        assertThat(expected.message, is(actual.message));
        assertThat(expected.scoreType, is(actual.scoreType));
        assertThat(expected.version, is(actual.version));
    }
}
