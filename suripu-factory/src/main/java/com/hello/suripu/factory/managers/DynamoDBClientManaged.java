package com.hello.suripu.factory.managers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.yammer.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamoDBClientManaged implements Managed {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBClientManaged.class);

    private final AmazonDynamoDBClient client;

    public DynamoDBClientManaged(AmazonDynamoDBClient dynamoDBClient) {
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
