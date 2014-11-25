package com.hello.suripu.app.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.core.db.FeatureStore;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Created by pangwu on 9/17/14.
 */
public class CreateFeaturesDynamoDBTableCommand extends ConfiguredCommand<SuripuAppConfiguration> {
    public CreateFeaturesDynamoDBTableCommand(){
        super("create_features_table", "Create dynamoDB features table");
    }

    @Override
    protected void run(Bootstrap<SuripuAppConfiguration> bootstrap, Namespace namespace, SuripuAppConfiguration configuration) throws Exception {
        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getFeaturesDynamoDBConfiguration().getEndpoint());
        final String tableName = configuration.getFeaturesDynamoDBConfiguration().getTableName();

        final CreateTableResult result = FeatureStore.createTable(tableName, client);
        final TableDescription description = result.getTableDescription();
        System.out.println(description.getTableStatus());
    }
}
