package com.hello.suripu.core.db;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseAsync;
import com.amazonaws.services.kinesisfirehose.model.DeliveryStreamDescription;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResponseEntry;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResult;
import com.amazonaws.services.kinesisfirehose.model.Record;
import com.amazonaws.services.kinesisfirehose.model.ServiceUnavailableException;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by jakepiccolo on 4/8/16, enhanced by ksg 01/23/2017 ;)
 */
public abstract class FirehoseDAO<T> {

    private class FirehoseAsyncHandler implements AsyncHandler<PutRecordBatchRequest, PutRecordBatchResult>
    {
        private List<Record> toInsertRecords;
        private List<Record> failedRecords = Lists.newArrayList();

        void addRecords(final List<Record> records) {
            this.toInsertRecords = records;
        }

        @Override
        public void onError(Exception exception) {
            logger().error("error=async-put-firehose-fail stream={} err_msg={}", deliveryStreamName, exception.getMessage());
        }

        @Override
        public void onSuccess(PutRecordBatchRequest request, PutRecordBatchResult putRecordBatchResult) {
            final List<Record> unprocessed = Lists.newArrayList();
            if (putRecordBatchResult.getFailedPutCount() != 0) {
                final List<PutRecordBatchResponseEntry> responseEntries = putRecordBatchResult.getRequestResponses();
                IntStream.range(0, responseEntries.size())
                        .filter(i -> responseEntries.get(i).getErrorCode() != null)
                        .peek(i -> logger().error("error=firehose-insert-fail error_code={}", responseEntries.get(i).getErrorCode()))
                        .forEach(i -> unprocessed.add(toInsertRecords.get(i)));
            }
            failedRecords = unprocessed;
        }

        List<Record> failedRecords() {
            return failedRecords;
        }
    }

    private static final Integer MAX_PUT_RECORDS = 500;
    private static final Integer MAX_BATCH_PUT_ATTEMPTS = 5;
    public static final String NULL_STRING = "\\N";

    private final String deliveryStreamName;
    private final AmazonKinesisFirehoseAsync asyncFirehose;

    public FirehoseDAO(final String deliveryStreamName, final AmazonKinesisFirehoseAsync asyncFirehose) {
        this.deliveryStreamName = deliveryStreamName;
        this.asyncFirehose = asyncFirehose;
    }

    public DeliveryStreamDescription describeStream() {
        return asyncFirehose
                .describeDeliveryStream(new DescribeDeliveryStreamRequest().withDeliveryStreamName(deliveryStreamName))
                .getDeliveryStreamDescription();
    }

    /**
     * batch insert a list of data-items
     * @param dataList the list of data
     * @return no. of records inserted
     */
    public int batchInsertAll(List<T> dataList) {
        final List<Record> records = Lists.newArrayListWithCapacity(dataList.size());

        for (final T data : dataList) {
            records.add(toRecord(data));
        }

        final List<Record> failedRecords = batchInsertAllRecords(records);

        return dataList.size() - failedRecords.size();
    }

    /**
     * Partition and insert all records.
     * @return List of failed records.
     */
    public List<Record> batchInsertAllRecords(final List<Record> records) {
        final List<Record> uninserted = Lists.newArrayList();
        for (final List<Record> recordsToInsert : Lists.partition(records, MAX_PUT_RECORDS)) {
            uninserted.addAll(batchInsertRecordsAsync(recordsToInsert));
        }
        return uninserted;
    }

    /**
     * Insert records that are <= the limit set by Amazon (MAX_PUT_RECORDS).
     * @return the list of failed records.
     */
    public List<Record> batchInsertRecordsAsync(final List<Record> records) {
        int numAttempts = 0;
        List<Record> unInsertedRecords = records;

        FirehoseAsyncHandler asyncHandler = new FirehoseAsyncHandler();

        while (!unInsertedRecords.isEmpty() && numAttempts < MAX_BATCH_PUT_ATTEMPTS) {
            numAttempts++;
            final PutRecordBatchRequest batchRequest = new PutRecordBatchRequest()
                    .withDeliveryStreamName(deliveryStreamName)
                    .withRecords(unInsertedRecords);

            try {
                asyncHandler.addRecords(unInsertedRecords);
                asyncFirehose.putRecordBatchAsync(batchRequest, asyncHandler);
                unInsertedRecords = asyncHandler.failedRecords();
            } catch (ServiceUnavailableException sue) {
                if (numAttempts < MAX_BATCH_PUT_ATTEMPTS) {
                    backoff(numAttempts);
                } else {
                    logger().error("error=firehose-service-unavailable-persists retries={} failed_records={}",
                            numAttempts, unInsertedRecords.size());
                }
            }
        }
        return unInsertedRecords;
    }

    private List<Record> failedRecords(final List<Record> attemptedRecords,
                                              final Future<PutRecordBatchResult> batchResultFuture) {
        try {
            final PutRecordBatchResult batchResult = batchResultFuture.get();
            if (batchResult.getFailedPutCount() == 0) {
                // All successful!
                return Collections.emptyList();
            }

            final List<PutRecordBatchResponseEntry> responseEntries = batchResult.getRequestResponses();
            return IntStream.range(0, responseEntries.size())
                    .filter(i -> responseEntries.get(i).getErrorCode() != null)
                    .peek(i -> logger().error("error=firehose-insert-fail error_code={}", responseEntries.get(i).getErrorCode()))
                    .mapToObj(attemptedRecords::get)
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            logger().error("error=batch-insert-interrupted-exception error_msg={}", e.getMessage());
        } catch (ExecutionException e) {
            logger().error("error=batch-insert-execution-exception error_msg={}", e.getMessage());
        }

        return attemptedRecords;
    }

    private void backoff(int numberOfAttempts) {
        try {
            long sleepMillis = (long) Math.pow(2, numberOfAttempts) * 50;
            logger().warn("warning=firehose-throttling sleep_ms={}", sleepMillis);
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            logger().error("error=exponential-backoff-interrupted error_msg={}", e.getMessage());
        }
    }

    private static Record toPipeDelimitedRecord(final Iterable<String> strings) {
        final String pipeDelimited = Joiner.on("|").join(strings);
        final String data = pipeDelimited + "\n";
        return createRecord(data);
    }

    public static Record toPipeDelimitedRecord(final String... strings) {
        return toPipeDelimitedRecord(Arrays.asList(strings));
    }

    private static Record createRecord(final String data) {
        return new Record().withData(ByteBuffer.wrap(data.getBytes()));
    }

    //region abstract methods
    protected abstract Logger logger();
    protected abstract Record toRecord(final T model);
    protected abstract String toString(final DateTime value);
    //endregion


    protected String toString(final Integer value) {
        if (value == null) {
            return FirehoseDAO.NULL_STRING;
        }
        return value.toString();
    }

    protected String toString(final Long value) {
        if (value == null) {
            return FirehoseDAO.NULL_STRING;
        }
        return value.toString();
    }

}
