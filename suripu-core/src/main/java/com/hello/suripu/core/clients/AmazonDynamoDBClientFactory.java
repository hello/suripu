package com.hello.suripu.core.clients;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.collect.Maps;

import java.util.Map;

public class AmazonDynamoDBClientFactory {

    private final AWSCredentialsProvider awsCredentialsProvider;
    private final ClientConfiguration clientConfiguration;
    private final Map<String, AmazonDynamoDB> clients = Maps.newHashMap();


    public static AmazonDynamoDBClientFactory create(final AWSCredentialsProvider awsCredentialsProvider) {
        final ClientConfiguration defaultClientConfiguration = new ClientConfiguration().withConnectionTimeout(200).withMaxErrorRetry(1);
        return new AmazonDynamoDBClientFactory(awsCredentialsProvider, defaultClientConfiguration);
    }

    public static AmazonDynamoDBClientFactory create(final AWSCredentialsProvider awsCredentialsProvider, final ClientConfiguration clientConfiguration) {
        return new AmazonDynamoDBClientFactory(awsCredentialsProvider, clientConfiguration);
    }

    private AmazonDynamoDBClientFactory(final AWSCredentialsProvider awsCredentialsProvider, final ClientConfiguration clientConfiguration) {
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.clientConfiguration = clientConfiguration;
    }



    public synchronized AmazonDynamoDB getForEndpoint(final String endpoint) {
        if(clients.containsKey(endpoint)) {
            return clients.get(endpoint);
        }

        final AmazonDynamoDB client = new AmazonDynamoDBClient(awsCredentialsProvider, clientConfiguration);
        client.setEndpoint(endpoint);
        clients.put(endpoint, client);
        return client;
    }
}
