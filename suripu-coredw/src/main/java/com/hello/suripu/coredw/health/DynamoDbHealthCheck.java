package com.hello.suripu.coredw.health;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.yammer.metrics.core.HealthCheck;

public class DynamoDbHealthCheck extends HealthCheck {

    private final AmazonDynamoDB client;

    public DynamoDbHealthCheck(final AmazonDynamoDB client) {
        super("dynamodb");
        this.client = client;
    }

    @Override
    protected Result check() throws Exception {
        final ListTablesResult listTablesResult = client.listTables();
        return Result.healthy();
    }
}
