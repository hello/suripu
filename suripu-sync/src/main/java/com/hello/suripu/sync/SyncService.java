package com.hello.suripu.sync;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.hello.dropwizard.mikkusu.helpers.JacksonProtobufProvider;
import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.dropwizard.mikkusu.resources.VersionResource;
import com.hello.suripu.core.db.PublicKeyStore;
import com.hello.suripu.core.db.PublicKeyStoreDynamoDB;
import com.hello.suripu.sync.configuration.SyncConfiguration;
import com.hello.suripu.sync.resources.ActivationResource;
import com.hello.suripu.sync.resources.SyncResource;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

public class SyncService extends Service<SyncConfiguration> {

    public static void main(final String[] args) throws Exception {
        new SyncService().run(args);
    }

    @Override
    public void initialize(final Bootstrap<SyncConfiguration> bootstrap) {

    }

    @Override
    public void run(final SyncConfiguration configuration, final Environment environment) throws Exception {
        environment.addProvider(new JacksonProtobufProvider());

        final AWSCredentialsProvider awsCredentialsProvider = new EnvironmentVariableCredentialsProvider();
        final AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(awsCredentialsProvider);
        dynamoDBClient.setEndpoint(configuration.getDynamoDBConfiguration().getEndpoint());

        final PublicKeyStore publicKeyStore = new PublicKeyStoreDynamoDB(
                dynamoDBClient,
                configuration.getDynamoDBConfiguration().getKeyStoreTable()
        );

        environment.addResource(new SyncResource());
        environment.addResource(new ActivationResource(publicKeyStore));
        environment.addResource(new PingResource());
        environment.addResource(new VersionResource());
    }
}
