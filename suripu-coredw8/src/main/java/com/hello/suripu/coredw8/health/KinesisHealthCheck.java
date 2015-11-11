package com.hello.suripu.coredw8.health;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.codahale.metrics.health.HealthCheck;

public class KinesisHealthCheck extends HealthCheck {

    private final AmazonKinesis client;

    public KinesisHealthCheck(AmazonKinesis client) {
        this.client = client;
    }

    @Override
    protected Result check() throws Exception {
        client.listStreams();
        return Result.healthy();
    }
}