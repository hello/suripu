package com.hello.suripu.app.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Created by pangwu on 5/30/14.
 */
public class CreateDynamoDBTimeZoneHistoryTableCommand extends ConfiguredCommand<SuripuAppConfiguration> {

    public CreateDynamoDBTimeZoneHistoryTableCommand(){
        super("create_timezone_history_table", "Create dynamoDB time zone history table");
    }

    @Override
    protected void run(Bootstrap<SuripuAppConfiguration> bootstrap, Namespace namespace, SuripuAppConfiguration configuration) throws Exception {
        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getTimeZoneHistoryDBConfiguration().getEndpoint());
        final String tableName = configuration.getTimeZoneHistoryDBConfiguration().getTableName();

        final CreateTableResult result = TimeZoneHistoryDAODynamoDB.createTable(tableName, client);
        final TableDescription description = result.getTableDescription();
        System.out.println(description.getTableStatus());
    }
}
