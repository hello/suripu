package com.hello.suripu.coredw.managers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.yammer.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamoDBClientManaged implements Managed {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBClientManaged.class);

    private final AmazonDynamoDB client;

    public DynamoDBClientManaged(AmazonDynamoDB dynamoDBClient) {
        this.client = dynamoDBClient;
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Attempting to shutdown DynamoDBClient.");
        client.shutdown();
        LOGGER.info("DynamoDBClient shutdown done!");
    }
}
