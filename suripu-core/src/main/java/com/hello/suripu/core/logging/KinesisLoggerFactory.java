package com.hello.suripu.core.logging;

import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class KinesisLoggerFactory {

    private final AmazonKinesisAsyncClient client;
    private final ConcurrentMap<String, KinesisLogger> loggers = new ConcurrentHashMap<String, KinesisLogger>();

    public KinesisLoggerFactory(final AmazonKinesisAsyncClient client) {
        this.client = client;
    }

    public KinesisLoggerFactory(final AmazonKinesisAsyncClient client, List<String> streamNames) {
        this(client);
        for(String streamName : streamNames) {
            loggers.putIfAbsent(streamName, new KinesisLogger(client, streamName));
        }
    }

    public KinesisLogger buildForStreamName(final String streamName) {
        KinesisLogger logger = loggers.get(streamName);
        if (logger  == null) {
            final KinesisLogger newLogger = new KinesisLogger(client, streamName);
            logger = loggers.putIfAbsent(streamName, newLogger);
            if (logger == null) {
                logger = newLogger;
            }
        }
        return logger;
    }
}
