package com.hello.suripu.service.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.hello.suripu.core.db.TrackerMotionDAODynamoDB;
import com.hello.suripu.service.configuration.SuripuConfiguration;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Created by pangwu on 5/30/14.
 */
public class CreateDynamoDBTrackerTableCommand extends ConfiguredCommand<SuripuConfiguration> {

    public CreateDynamoDBTrackerTableCommand(){
        super("create_motion_table", "Create dynamoDB tracker motion table");
    }


    @Override
    protected void run(Bootstrap<SuripuConfiguration> bootstrap, Namespace namespace, SuripuConfiguration configuration) throws Exception {
        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getMotionDBConfiguration().getEndpoint());
        final String tableName = configuration.getMotionDBConfiguration().getKeyStoreTable();

        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(TrackerMotionDAODynamoDB.ACCOUNT_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(TrackerMotionDAODynamoDB.TARGET_DATE_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(TrackerMotionDAODynamoDB.ACCOUNT_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N),
                new AttributeDefinition().withAttributeName(TrackerMotionDAODynamoDB.TARGET_DATE_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)
        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = client.createTable(request);
        final TableDescription description = result.getTableDescription();
        System.out.println(description.getTableStatus());
    }
}
