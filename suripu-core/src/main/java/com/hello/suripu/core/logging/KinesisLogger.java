package com.hello.suripu.core.logging;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import com.amazonaws.services.kinesis.model.PutRecordsResultEntry;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KinesisLogger implements DataLogger {

    private final static Logger LOGGER = LoggerFactory.getLogger(KinesisLogger.class);

    private final static int MAX_BATCH_RECORDS = 500;

    private final AmazonKinesisAsyncClient kinesisClient;
    private final String streamName;

    private class ProcessFailuresResult {
        int numSuccess;
        List<Boolean> putSuccesses;

        ProcessFailuresResult(final int numSuccess, final List<Boolean> putSuccesses) {
            this.numSuccess = numSuccess;
            this.putSuccesses = putSuccesses;
        }

    }

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
                LOGGER.error("error=sending-to-kinesis stream={} err_msg={}", streamName, exception.getMessage());
            }

            @Override
            public void onSuccess(final PutRecordRequest request, final PutRecordResult putRecordResult) {
                LOGGER.trace("ShardId = {}", putRecordResult.getShardId());
                LOGGER.trace("Sequence number = {}",putRecordResult.getSequenceNumber());
            }
        });

    }

    @Override
    public String put(final String partitionKey, final byte[] payload) {
        final ByteBuffer data = ByteBuffer.wrap(payload);
        final String noSequenceNumber = "";

        final Optional<String> optionalSequenceNumber = putSingleRecord(partitionKey, data, noSequenceNumber);

        if (!optionalSequenceNumber.isPresent()) {
            throw new RuntimeException("Did not receive SequenceNumber or ShardId from Kinesis. Bad stream configuration? Currently using: " + streamName);
        }

        return optionalSequenceNumber.get();
    }

    @Override
    public String putWithSequenceNumber(final String deviceId, final byte[] payload, final String sequenceNumber) {
        final ByteBuffer data = ByteBuffer.wrap(payload);
        final Optional<String> optionalSequenceNumber = putSingleRecord(deviceId, data, sequenceNumber);
        if (!optionalSequenceNumber.isPresent()) {
            throw new RuntimeException("Did not receive SequenceNumber or ShardId from Kinesis. Bad stream configuration? Currently using: " + streamName);
        }
        return optionalSequenceNumber.get();
    }

    @Override
    public KinesisBatchPutResult putRecords(final List<DataLoggerBatchPayload> payloadBatch) {

        final List<List<DataLoggerBatchPayload>> batches = Lists.partition(payloadBatch, MAX_BATCH_RECORDS);

        int numSuccesses = 0;
        final List<Boolean> putSuccesses = Lists.newArrayList();

        for (final List<DataLoggerBatchPayload> batch : batches) {

            // insert a batch of up to 500 records
            final List<PutRecordsRequestEntry> putRecordsRequestEntries = Lists.newArrayList();
            for (final DataLoggerBatchPayload singlePayload : batch) {
                final ByteBuffer data = ByteBuffer.wrap(singlePayload.payload);
                final PutRecordsRequestEntry putRecordsRequestEntry = new PutRecordsRequestEntry();
                putRecordsRequestEntry.setData(data);
                putRecordsRequestEntry.setPartitionKey(singlePayload.deviceId);
                putRecordsRequestEntries.add(putRecordsRequestEntry);
            }

            final PutRecordsRequest putRecordsRequest = new PutRecordsRequest();
            putRecordsRequest.setStreamName(streamName);
            putRecordsRequest.setRecords(putRecordsRequestEntries);

            // put all the records
            final PutRecordsResult putRecordsResult = kinesisClient.putRecords(putRecordsRequest);

            if (putRecordsResult.getFailedRecordCount() == 0) {
                // success :)
                numSuccesses += batch.size();
                putSuccesses.addAll(new ArrayList<>(Collections.nCopies(batch.size(), true)));
            } else {
                // fails :(
                final ProcessFailuresResult failuresResult = processFailedPuts(putRecordsResult, putRecordsRequest);
                numSuccesses += failuresResult.numSuccess;
                putSuccesses.addAll(failuresResult.putSuccesses);
            }
        }

        return new KinesisBatchPutResult(numSuccesses, payloadBatch.size(), putSuccesses);
    }

    private Optional<String> putSingleRecord(final String partitionKey, final ByteBuffer data, final String sequenceNumber) {
        final PutRecordRequest putRecordRequest = new PutRecordRequest();
        putRecordRequest.setData(data);
        putRecordRequest.setStreamName(streamName);
        putRecordRequest.setPartitionKey(partitionKey);

        if (!sequenceNumber.isEmpty()) {
            putRecordRequest.setSequenceNumberForOrdering(sequenceNumber);
        }

        final PutRecordResult recordResult = kinesisClient.putRecord(putRecordRequest);
        if (recordResult.getSequenceNumber() == null || recordResult.getShardId() == null) {
            LOGGER.error("error=fail-to-put-into-Kinesis stream={}, partition_key={}, seq_number={}",
                    streamName, partitionKey, sequenceNumber);
            return Optional.absent();

        }

        LOGGER.trace("msg=successfully-saved-in-Kinesis seq_number={} shardId={}",
                recordResult.getSequenceNumber(),
                recordResult.getShardId()
        );

        return Optional.of(recordResult.getSequenceNumber());
    }

    private ProcessFailuresResult processFailedPuts(final PutRecordsResult putRecordsResult, final PutRecordsRequest putRecordsRequest) {
        int numSuccesses = 0;
        String lastSequenceNumber = "";
        final List<Boolean> putSuccesses = Lists.newArrayList();

        final List<PutRecordsResultEntry> putRecordsResultEntryList = putRecordsResult.getRecords();

        // retry with put, set sequence number to be after the previous successful put
        for (int i = 0; i < putRecordsResultEntryList.size(); i++) {

            // get put result for this record
            final PutRecordsResultEntry resultEntry = putRecordsResultEntryList.get(i);

            if (resultEntry.getErrorCode() == null) { // successful
                lastSequenceNumber = resultEntry.getSequenceNumber();
                numSuccesses++;
                putSuccesses.add(true);
                continue;
            }

            LOGGER.error("error=failed-batch-puts error_code={} msg={}",
                    resultEntry.getErrorCode(), resultEntry.getErrorMessage());

            // failure, try again
            final PutRecordsRequestEntry requestEntry = putRecordsRequest.getRecords().get(i);
            final ByteBuffer data = requestEntry.getData();
            final String partitionKey = requestEntry.getPartitionKey();

            final Optional<String> optionalSequenceNumber = putSingleRecord(partitionKey, data, lastSequenceNumber);

            if (optionalSequenceNumber.isPresent()) {
                // successful retry
                numSuccesses++;
                lastSequenceNumber = optionalSequenceNumber.get();
                putSuccesses.add(true);
            } else {
                putSuccesses.add(false);
            }
        }
        return new ProcessFailuresResult(numSuccesses, putSuccesses);
    }
}
