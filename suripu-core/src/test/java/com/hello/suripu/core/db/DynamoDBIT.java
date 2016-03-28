package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jakepiccolo on 3/11/16.
 */
public abstract class DynamoDBIT<T> {

    private final static Logger LOGGER = LoggerFactory.getLogger(DynamoDBIT.class);

    protected BasicAWSCredentials awsCredentials;
    protected AmazonDynamoDB amazonDynamoDBClient;
    protected T dao;

    protected static String TABLE_NAME = "integration_test";

    protected abstract CreateTableResult createTable();

    protected abstract T createDAO();

    protected void deleteTable() {
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(TABLE_NAME);
        this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
    }

    @Before
    public void setUp() throws Exception {
        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        final ListTablesResult res = this.amazonDynamoDBClient.listTables();
        for (final String tablename : res.getTableNames()) {
            LOGGER.debug("table {} exist", tablename);
        }

        this.dao = createDAO();

        tearDown();

        try {
            LOGGER.debug("-------- Creating Table {} ---------", TABLE_NAME);
            final CreateTableResult result = createTable();
            LOGGER.debug("Created dynamoDB table {}", result.getTableDescription());
        } catch (ResourceInUseException rie){
            LOGGER.warn("Problem creating table");
        }
    }

    @After
    public void tearDown() throws Exception {
        try {
            deleteTable();
        }catch (ResourceNotFoundException ex){
            LOGGER.warn("Can not delete non existing table {}", TABLE_NAME);
        }
    }
}
