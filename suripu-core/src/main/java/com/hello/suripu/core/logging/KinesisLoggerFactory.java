package com.hello.suripu.core.logging;

import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.configuration.QueueName;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class KinesisLoggerFactory {

    private final ImmutableMap<QueueName, DataLogger> loggers;

    /**
     * Builds a Map of KinesisLogger with pre-configured stream name
     * @param client
     * @param streamNames
     */
    public KinesisLoggerFactory(final AmazonKinesisAsyncClient client, final Map<QueueName, String> streamNames) {

        final Set<QueueName> keys = streamNames.keySet();
        final Map<QueueName, DataLogger> streamNameDataLoggerMap = new HashMap<QueueName, DataLogger>(streamNames.size());

        for(final QueueName queueNames : keys) {
            final String streamName = streamNames.get(queueNames);
            if(streamName == null) {
                throw new RuntimeException("Stream name is null");
            }
            streamNameDataLoggerMap.put(queueNames, new KinesisLogger(client, streamName));
        }

        this.loggers = ImmutableMap.copyOf(streamNameDataLoggerMap);
    }


    /**
     * Get DataLogger (KinesisLogger) from the map. Will blow up if streamName does not exist
     * and that's what we want.
     * @param streamName
     * @return
     */
    public DataLogger get(final QueueName streamName) {
        if(!loggers.containsKey(streamName)) {
            throw new RuntimeException("Missing Kinesis streamName");
        }

        return loggers.get(streamName);
    }
}
