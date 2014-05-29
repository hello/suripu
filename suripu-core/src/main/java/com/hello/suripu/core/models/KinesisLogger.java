package com.hello.suripu.core.models;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class KinesisLogger {

    private final static Logger LOGGER = LoggerFactory.getLogger(KinesisLogger.class);

    private final AmazonKinesisAsyncClient kinesisClient;
    private final String streamName;

    public KinesisLogger(final AmazonKinesisAsyncClient kinesisClient, final String streamName) {
        this.kinesisClient = kinesisClient;
        this.streamName = streamName;
    }

    public void put(final String deviceId, final byte[] payload) {

        final ByteBuffer data = ByteBuffer.wrap(payload);
        final PutRecordRequest putRecordRequest = new PutRecordRequest();
        putRecordRequest.setData(data);
        putRecordRequest.setStreamName(streamName);
        putRecordRequest.setPartitionKey(deviceId);

        kinesisClient.putRecordAsync(putRecordRequest, new AsyncHandler<PutRecordRequest, PutRecordResult>() {
            @Override
            public void onError(Exception exception) {
                LOGGER.error(exception.getMessage());
            }

            @Override
            public void onSuccess(PutRecordRequest request, PutRecordResult putRecordResult) {
                LOGGER.debug("ShardId = {}", putRecordResult.getShardId());
                LOGGER.debug("Sequence number = {}",putRecordResult.getSequenceNumber());
            }
        });

    }
}
