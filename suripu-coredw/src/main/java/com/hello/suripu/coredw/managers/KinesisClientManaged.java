package com.hello.suripu.coredw.managers;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.yammer.dropwizard.lifecycle.Managed;

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
