package com.hello.suripu.core.logging;

import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.configuration.QueueName;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class KinesisLoggerFactory {

    private final AmazonKinesisAsyncClient client;
    private final ImmutableMap<QueueName, DataLogger> loggers;

    public KinesisLoggerFactory(final AmazonKinesisAsyncClient client, final Map<QueueName, String> streamNames) {
        this.client = client;

        final Set<QueueName> keys = streamNames.keySet();
        final Map<QueueName, DataLogger> streamNameDataLoggerMap = new HashMap<QueueName, DataLogger>(streamNames.size());

        for(final QueueName queueName : keys) {
            final String streamName = streamNames.get(queueName);
            streamNameDataLoggerMap.put(queueName, new KinesisLogger(client, streamName));
        }

        this.loggers = ImmutableMap.copyOf(streamNameDataLoggerMap);
    }



    public DataLogger get(final QueueName streamName) {
        if(!loggers.containsKey(streamName)) {
            throw new RuntimeException("Missing Kinesis streamName");
        }

        return loggers.get(streamName);
    }
}
