package com.hello.suripu.app.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.core.configuration.DynamoDBConfiguration;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

public class CreateSleepScoreDynamoDBTable extends ConfiguredCommand<SuripuAppConfiguration> {
    public CreateSleepScoreDynamoDBTable(){
        super("create_sleep_score_table", "Create dynamoDB alarm table");
    }

    @Override
    protected void run(Bootstrap<SuripuAppConfiguration> bootstrap, Namespace namespace, SuripuAppConfiguration configuration) throws Exception {
        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        final DynamoDBConfiguration config = configuration.getSleepScoreDBConfiguration();
        final String version = configuration.getSleepScoreVersion();

        client.setEndpoint(config.getEndpoint());
        final String tableName = config.getTableName() + "_" + version;

        final CreateTableResult result = AggregateSleepScoreDAODynamoDB.createTable(tableName, client);
        final TableDescription description = result.getTableDescription();
        System.out.println(description.getTableStatus());
    }
}
