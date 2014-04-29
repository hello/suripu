package com.hello.suripu.factory.health;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.yammer.metrics.core.HealthCheck;

public class DynamoDbHealthCheck extends HealthCheck {

    private final AmazonDynamoDBClient client;

    public DynamoDbHealthCheck(final AmazonDynamoDBClient client) {
        super("dynamodb");
        this.client = client;
    }

    @Override
    protected Result check() throws Exception {
        final ListTablesResult listTablesResult = client.listTables();
        return Result.healthy();
    }
}
