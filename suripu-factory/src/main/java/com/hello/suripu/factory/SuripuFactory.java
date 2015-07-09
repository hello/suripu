package com.hello.suripu.factory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.hello.dropwizard.mikkusu.helpers.JacksonProtobufProvider;
import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.dropwizard.mikkusu.resources.VersionResource;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.health.DynamoDbHealthCheck;
import com.hello.suripu.coredw.managers.DynamoDBClientManaged;
import com.hello.suripu.factory.cli.CreateTableCommand;
import com.hello.suripu.factory.configuration.SuripuFactoryConfiguration;
import com.hello.suripu.factory.resources.FactoryResource;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

import java.util.TimeZone;

public class SuripuFactory extends Service<SuripuFactoryConfiguration>{


    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        new SuripuFactory().run(args);
    }

    @Override
    public void initialize(Bootstrap<SuripuFactoryConfiguration> bootstrap) {
        bootstrap.addCommand(new CreateTableCommand());
    }

    @Override
    public void run(SuripuFactoryConfiguration configuration, Environment environment) throws Exception {
        environment.addProvider(new JacksonProtobufProvider());


        final AWSCredentialsProvider awsCredentialsProvider = new EnvironmentVariableCredentialsProvider();
        final AmazonDynamoDB dynamoDBClient = new AmazonDynamoDBClient(awsCredentialsProvider);
        dynamoDBClient.setEndpoint(configuration.getDynamoDBConfiguration().getEndpoint());
        // TODO; set region here?

        final KeyStore keyStore = new KeyStoreDynamoDB(
                dynamoDBClient,
                configuration.getDynamoDBConfiguration().getTableName(),
                new byte[16],
                120
        );

        environment.addResource(new FactoryResource(keyStore));
        environment.addResource(new PingResource());
        environment.addResource(new VersionResource());
        environment.manage(new DynamoDBClientManaged(dynamoDBClient));
        environment.addHealthCheck(new DynamoDbHealthCheck(dynamoDBClient));
    }
}
