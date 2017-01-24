package com.hello.suripu.core.db;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
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
import java.util.List;

/**
 * Created by jakepiccolo on 4/8/16, enhanced by ksg 01/23/2017 ;)
 */
public abstract class FirehoseDAO<T> {

    // private final static Logger LOGGER = LoggerFactory.getLogger(FirehoseDAO.class);

    private static final Integer MAX_PUT_RECORDS = 500;
    private static final Integer MAX_BATCH_PUT_ATTEMPTS = 5;
    public static final String NULL_STRING = "\\N";

    private final String deliveryStreamName;
    private final AmazonKinesisFirehose firehose;

    public FirehoseDAO(final String deliveryStreamName, final AmazonKinesisFirehose firehose) {
        this.deliveryStreamName = deliveryStreamName;
        this.firehose = firehose;
    }

    public DeliveryStreamDescription describeStream() {
        return firehose
                .describeDeliveryStream(new DescribeDeliveryStreamRequest().withDeliveryStreamName(deliveryStreamName))
                .getDeliveryStreamDescription();
    }

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
            uninserted.addAll(batchInsertRecords(recordsToInsert));
        }
        return uninserted;
    }

    /**
     * Insert records that are <= the limit set by Amazon (MAX_PUT_RECORDS).
     * @return the list of failed records.
     */
    public List<Record> batchInsertRecords(final List<Record> records) {
        int numAttempts = 0;
        List<Record> unInsertedRecords = records;

        while (!unInsertedRecords.isEmpty() && numAttempts < MAX_BATCH_PUT_ATTEMPTS) {
            numAttempts++;
            final PutRecordBatchRequest batchRequest = new PutRecordBatchRequest()
                    .withDeliveryStreamName(deliveryStreamName)
                    .withRecords(unInsertedRecords);

            try {
                final PutRecordBatchResult result = firehose.putRecordBatch(batchRequest);
                unInsertedRecords = failedRecords(unInsertedRecords, result);
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
                                              final PutRecordBatchResult batchResult) {
        final List<Record> failed = Lists.newArrayList();
        if (batchResult.getFailedPutCount() == 0) {
            // All successful!
            return failed;
        }

        final List<PutRecordBatchResponseEntry> responseEntries = batchResult.getRequestResponses();
        for (int i = 0; i < responseEntries.size(); i++) {
            if (responseEntries.get(i).getErrorCode() != null) {
                logger().error("error=firehose-insert-fail error_code={}", responseEntries.get(i).getErrorCode());
                failed.add(attemptedRecords.get(i));
            }
        }
        return failed;
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
