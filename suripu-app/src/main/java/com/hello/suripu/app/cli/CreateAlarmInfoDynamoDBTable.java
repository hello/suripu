package com.hello.suripu.app.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.core.db.MergedAlarmInfoDynamoDB;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Created by pangwu on 9/29/14.
 */
public class CreateAlarmInfoDynamoDBTable extends ConfiguredCommand<SuripuAppConfiguration> {
    public CreateAlarmInfoDynamoDBTable(){
        super("create_alarm_info_table", "Create dynamoDB megred alarm info table");
    }

    @Override
    protected void run(Bootstrap<SuripuAppConfiguration> bootstrap, Namespace namespace, SuripuAppConfiguration configuration) throws Exception {
        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getAlarmInfoDynamoDBConfiguration().getEndpoint());
        final String tableName = configuration.getAlarmInfoDynamoDBConfiguration().getTableName();

        final CreateTableResult result = MergedAlarmInfoDynamoDB.createTable(tableName, client);
        final TableDescription description = result.getTableDescription();
        System.out.println(description.getTableStatus());
    }
}
