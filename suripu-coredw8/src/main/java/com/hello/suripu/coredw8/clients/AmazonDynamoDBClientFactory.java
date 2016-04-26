package com.hello.suripu.coredw8.clients;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.collect.Maps;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.hello.suripu.coredw8.configuration.NewDynamoDBConfiguration;
import com.hello.suripu.coredw8.metrics.InstrumentedDynamoDBClient;

import java.util.Map;

public class AmazonDynamoDBClientFactory {

    private final AWSCredentialsProvider awsCredentialsProvider;
    private final ClientConfiguration clientConfiguration;
    private final Map<String, AmazonDynamoDB> clients = Maps.newHashMap();
    private final Map<String, AmazonDynamoDB> instrumentedClients = Maps.newHashMap(); // key is className
    private final NewDynamoDBConfiguration dynamoDBConfiguration;

    private final static ClientConfiguration DEFAULT_CLIENT_CONFIGURATION = new ClientConfiguration().withConnectionTimeout(200).withMaxErrorRetry(1);

    @Deprecated
    public static AmazonDynamoDBClientFactory create(final AWSCredentialsProvider awsCredentialsProvider) {

        final NewDynamoDBConfiguration dynamoDBConfiguration = new NewDynamoDBConfiguration();
        return new AmazonDynamoDBClientFactory(awsCredentialsProvider, DEFAULT_CLIENT_CONFIGURATION, dynamoDBConfiguration);
    }

    public static AmazonDynamoDBClientFactory create(final AWSCredentialsProvider awsCredentialsProvider, final ClientConfiguration clientConfiguration, final NewDynamoDBConfiguration dynamoDBConfiguration) {
        return new AmazonDynamoDBClientFactory(awsCredentialsProvider, clientConfiguration, dynamoDBConfiguration);
    }

    public static AmazonDynamoDBClientFactory create(final AWSCredentialsProvider awsCredentialsProvider, final NewDynamoDBConfiguration dynamoDBConfiguration) {
        return new AmazonDynamoDBClientFactory(awsCredentialsProvider, DEFAULT_CLIENT_CONFIGURATION, dynamoDBConfiguration);
    }

    private AmazonDynamoDBClientFactory(final AWSCredentialsProvider awsCredentialsProvider, final ClientConfiguration clientConfiguration, final NewDynamoDBConfiguration dynamoDBConfiguration) {
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.clientConfiguration = clientConfiguration;
        this.dynamoDBConfiguration = dynamoDBConfiguration;
    }


    @Deprecated
    public synchronized AmazonDynamoDB getForEndpoint(final String endpoint) {
        if(clients.containsKey(endpoint)) {
            return clients.get(endpoint);
        }

        final AmazonDynamoDB client = new AmazonDynamoDBClient(awsCredentialsProvider, clientConfiguration);
        client.setEndpoint(endpoint);
        clients.put(endpoint, client);
        return client;
    }


    public synchronized AmazonDynamoDB getForTable(final DynamoDBTableName tableName) {
        if(!dynamoDBConfiguration.tables().containsKey(tableName) || !dynamoDBConfiguration.endpoints().containsKey(tableName)) {
            throw new IllegalArgumentException("Check configuration. Invalid table name or endpoint name for: " + tableName.toString());
        }

        // This is an exception to the way clients are created
        // We want an individual and independent threadpool for our feature flipper
        // Important: it isn't cached, so any call to this method will return a new client
        if(DynamoDBTableName.FEATURES.equals(tableName)) {
            final AmazonDynamoDB client = new AmazonDynamoDBClient(awsCredentialsProvider, clientConfiguration);
            client.setEndpoint(dynamoDBConfiguration.endpoints().get(DynamoDBTableName.FEATURES));
            return client;
        }

        final String endpoint = dynamoDBConfiguration.endpoints().get(tableName);
        if(clients.containsKey(endpoint)) {
            return clients.get(endpoint);
        }

        final AmazonDynamoDB client = new AmazonDynamoDBClient(awsCredentialsProvider, clientConfiguration);
        client.setEndpoint(endpoint);
        clients.put(endpoint, client);
        return client;
    }


    public synchronized AmazonDynamoDB getInstrumented(final DynamoDBTableName tableName, final Class<?> klass) {
        if(!dynamoDBConfiguration.tables().containsKey(tableName) || !dynamoDBConfiguration.endpoints().containsKey(tableName)) {
            throw new IllegalArgumentException("Check configuration. Invalid tableName: " + tableName.toString());
        }

        final String endpoint = dynamoDBConfiguration.endpoints().get(tableName);
        if(instrumentedClients.containsKey(klass.getName())) {
            return instrumentedClients.get(klass.getName());
        }

        final AmazonDynamoDB client = new InstrumentedDynamoDBClient(new AmazonDynamoDBClient(awsCredentialsProvider, clientConfiguration), klass);
        client.setEndpoint(endpoint);
        instrumentedClients.put(klass.getName(), client);
        return client;
    }

    public static ClientConfiguration getDefaultClientConfiguration() {
        return DEFAULT_CLIENT_CONFIGURATION;
    }
}
