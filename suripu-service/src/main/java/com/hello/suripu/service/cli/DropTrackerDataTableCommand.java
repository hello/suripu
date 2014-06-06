package com.hello.suripu.service.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.hello.suripu.service.configuration.SuripuConfiguration;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Created by pangwu on 6/4/14.
 */
public class DropTrackerDataTableCommand extends ConfiguredCommand<SuripuConfiguration> {

    public DropTrackerDataTableCommand(){
        super("drop_motion_table", "Drop dynamoDB tracker motion table");
    }

    @Override
    protected void run(Bootstrap<SuripuConfiguration> bootstrap, Namespace namespace, SuripuConfiguration configuration) throws Exception {

        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getMotionDBConfiguration().getEndpoint());
        final String tableName = configuration.getMotionDBConfiguration().getTableName();

        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(tableName);
        try {
            client.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            ex.printStackTrace();
        }

        System.out.println("Motion table dropped");

    }
}
