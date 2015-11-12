package com.hello.suripu.coredw8.managers;

import com.amazonaws.services.kinesis.AmazonKinesis;

import io.dropwizard.lifecycle.Managed;


public class KinesisClientManaged implements Managed {

    private final AmazonKinesis client;

    public KinesisClientManaged(AmazonKinesis kinesisClient) {
        this.client = kinesisClient;
    }
    @Override
    public void start() throws Exception {
        // Attempt operation to validate that everything is alright
        client.listStreams();
    }

    @Override
    public void stop() throws Exception {
        client.shutdown();
    }
}
