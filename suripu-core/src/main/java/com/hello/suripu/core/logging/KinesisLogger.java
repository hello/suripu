package com.hello.suripu.core.logging;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class KinesisLogger implements DataLogger {

    private final static Logger LOGGER = LoggerFactory.getLogger(KinesisLogger.class);

    private final AmazonKinesisAsyncClient kinesisClient;
    private final String streamName;

    public KinesisLogger(final AmazonKinesisAsyncClient kinesisClient, final String streamName) {
        this.kinesisClient = kinesisClient;
        this.streamName = streamName;
    }

    @Override
    public void putAsync(final String partitionKey, final byte[] payload) {

        final ByteBuffer data = ByteBuffer.wrap(payload);
        final PutRecordRequest putRecordRequest = new PutRecordRequest();
        putRecordRequest.setData(data);
        putRecordRequest.setStreamName(streamName);
        putRecordRequest.setPartitionKey(partitionKey);

        kinesisClient.putRecordAsync(putRecordRequest, new AsyncHandler<PutRecordRequest, PutRecordResult>() {
            @Override
            public void onError(final Exception exception) {
                LOGGER.error("Error sending to Kinesis for stream: {}", streamName, exception.getMessage());
            }

            @Override
            public void onSuccess(final PutRecordRequest request, final PutRecordResult putRecordResult) {
                LOGGER.debug("ShardId = {}", putRecordResult.getShardId());
                LOGGER.debug("Sequence number = {}",putRecordResult.getSequenceNumber());
            }
        });

    }

    @Override
    public String put(final String partitionKey, final byte[] payload) {
        final ByteBuffer data = ByteBuffer.wrap(payload);
        final PutRecordRequest putRecordRequest = new PutRecordRequest();
        putRecordRequest.setData(data);
        putRecordRequest.setStreamName(streamName);
        putRecordRequest.setPartitionKey(partitionKey);

        final PutRecordResult  recordResult = kinesisClient.putRecord(putRecordRequest);
        if(recordResult.getSequenceNumber() == null || recordResult.getShardId() == null) {
            throw new RuntimeException("Did not receive SequenceNumber or ShardId from Kinesis. Bad stream configuration? Currently using: " + streamName);
        }
        LOGGER.debug("Successfully saved in Kinesis. Seq Number = {} and ShardId = {}",
                recordResult.getSequenceNumber(),
                recordResult.getShardId()
        );
        return recordResult.getSequenceNumber();
    }

    @Override
    public String putWithSequenceNumber(final String deviceId, final byte[] payload, final String sequenceNumber) {
        final ByteBuffer data = ByteBuffer.wrap(payload);
        final PutRecordRequest putRecordRequest = new PutRecordRequest();
        putRecordRequest.setData(data);
        putRecordRequest.setStreamName(streamName);
        putRecordRequest.setPartitionKey(deviceId);
        putRecordRequest.setSequenceNumberForOrdering(sequenceNumber);

        final PutRecordResult  recordResult = kinesisClient.putRecord(putRecordRequest);
        LOGGER.debug("Successfully saved in Kinesis. Seq Number = {} and ShardId = {}",
                recordResult.getSequenceNumber(),
                recordResult.getShardId()
        );
        return recordResult.getSequenceNumber();
    }
}
