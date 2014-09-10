package com.hello.suripu.core.logging;

import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.configuration.QueueNames;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class KinesisLoggerFactory {

    private final AmazonKinesisAsyncClient client;
    private final ImmutableMap<QueueNames, DataLogger> loggers;

    public KinesisLoggerFactory(final AmazonKinesisAsyncClient client, final Map<QueueNames, String> streamNames) {
        this.client = client;

        final Set<QueueNames> keys = streamNames.keySet();
        final Map<QueueNames, DataLogger> streamNameDataLoggerMap = new HashMap<QueueNames, DataLogger>(streamNames.size());

        for(final QueueNames queueNames : keys) {
            final String streamName = streamNames.get(queueNames);
            if(streamName == null) {
                throw new RuntimeException("Stream name is null");
            }
            streamNameDataLoggerMap.put(queueNames, new KinesisLogger(client, streamName));
        }

        this.loggers = ImmutableMap.copyOf(streamNameDataLoggerMap);
    }



    public DataLogger get(final QueueNames streamName) {
        if(!loggers.containsKey(streamName)) {
            throw new RuntimeException("Missing Kinesis streamName");
        }

        return loggers.get(streamName);
    }
}
