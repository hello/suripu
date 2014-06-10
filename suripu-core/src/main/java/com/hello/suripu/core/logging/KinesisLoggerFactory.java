package com.hello.suripu.core.logging;

import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.configuration.Queues;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class KinesisLoggerFactory {

    private final AmazonKinesisAsyncClient client;
    private final ImmutableMap<Queues, DataLogger> loggers;

    public KinesisLoggerFactory(final AmazonKinesisAsyncClient client, final Map<Queues, String> streamNames) {
        this.client = client;

        final Set<Queues> keys = streamNames.keySet();
        final Map<Queues, DataLogger> streamNameDataLoggerMap = new HashMap<Queues, DataLogger>(streamNames.size());

        for(Queues queues : keys) {
            final String streamName = streamNames.get(queues.name());
            streamNameDataLoggerMap.put(queues, new KinesisLogger(client, streamName));
        }

        this.loggers = ImmutableMap.copyOf(streamNameDataLoggerMap);
    }



    public DataLogger get(final Queues streamName) {
        if(!loggers.containsKey(streamName)) {
            throw new RuntimeException("Missing Kinesis streamName");
        }

        return loggers.get(streamName);
    }
}
