package com.hello.suripu.queue.models;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.yammer.metrics.core.HealthCheck;

/**
 * Created by ksg on 3/15/16
 */

public class QueueHealthCheck extends HealthCheck{
    private String name;
    private final AmazonSQSAsync sqsClient;
    private final String queueURL;

    public QueueHealthCheck(final String name, final AmazonSQSAsync sqsClient, final String queueURL) {
        super("health-check");
        this.name = name;
        this.sqsClient = sqsClient;
        this.queueURL = queueURL;
    }

    @Override
    public Result check() throws Exception {
        // ping SQS
        final ListQueuesResult results = sqsClient.listQueues();
        if (results.getQueueUrls().contains(this.queueURL)) {
            return Result.healthy();
        }
        return Result.unhealthy("Fail health check!");
    }
}
