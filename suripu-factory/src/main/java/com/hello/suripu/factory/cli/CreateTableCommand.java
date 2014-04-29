package com.hello.suripu.factory.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.hello.suripu.factory.configuration.SuripuFactoryConfiguration;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

public class CreateTableCommand extends ConfiguredCommand<SuripuFactoryConfiguration> {

    public CreateTableCommand() {
        super("create_table", "Create dynamoDB table");
    }

    @Override
    protected void run(Bootstrap<SuripuFactoryConfiguration> bootstrap, Namespace namespace, SuripuFactoryConfiguration configuration) throws Exception {
        final AWSCredentialsProvider awsCredentialsProvider= new EnvironmentVariableCredentialsProvider();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        client.setEndpoint(configuration.getDynamoDBConfiguration().getEndpoint());
        final String tableName = configuration.getDynamoDBConfiguration().getKeyStoreTable();

        final String hashKey = "device_id";

        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(hashKey).withKeyType(KeyType.HASH)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(hashKey).withAttributeType(ScalarAttributeType.S)
        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = client.createTable(request);
        final TableDescription description = result.getTableDescription();
        System.out.println(description.getTableStatus());
    }
}
