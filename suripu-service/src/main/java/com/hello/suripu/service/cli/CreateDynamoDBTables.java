package com.hello.suripu.service.cli;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.hello.suripu.core.configuration.NewDynamoDBConfiguration;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.service.configuration.SuripuConfiguration;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

public class CreateDynamoDBTables extends ConfiguredCommand<SuripuConfiguration> {

    public CreateDynamoDBTables() {
        super("create_dynamodb_tables_service", "Create service specific dynamoDB tables");
    }

    @Override
    protected void run(Bootstrap<SuripuConfiguration> bootstrap, Namespace namespace, SuripuConfiguration configuration) throws Exception {

        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        createRingTimeHistoryTable(configuration, awsCredentialsProvider);
    }


    private void createRingTimeHistoryTable(final SuripuConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider){
        final NewDynamoDBConfiguration config = configuration.dynamoDBConfiguration();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        final ImmutableMap<DynamoDBTableName, String> tableNames = configuration.dynamoDBConfiguration().tables();
        final ImmutableMap<DynamoDBTableName, String> endpoints = configuration.dynamoDBConfiguration().endpoints();

        final String tableName = tableNames.get(DynamoDBTableName.RING_SCHEDULE_HISTORY);
        final String endpoint = endpoints.get(DynamoDBTableName.RING_SCHEDULE_HISTORY);

        client.setEndpoint(endpoint);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = RingTimeHistoryDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(tableName + ": " + description.getTableStatus());
        }
    }

}
