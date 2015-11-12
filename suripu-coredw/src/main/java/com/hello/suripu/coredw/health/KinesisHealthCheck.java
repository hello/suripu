package com.hello.suripu.coredw.health;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.yammer.metrics.core.HealthCheck;

public class KinesisHealthCheck extends HealthCheck {

    private final AmazonKinesis client;

    public KinesisHealthCheck(AmazonKinesis client) {
        super("kinesis");
        this.client = client;
    }

    @Override
    protected Result check() throws Exception {
        client.listStreams();
        return Result.healthy();
    }
}
