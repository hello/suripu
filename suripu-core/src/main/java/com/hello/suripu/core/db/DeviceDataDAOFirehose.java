package com.hello.suripu.core.db;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResponseEntry;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResult;
import com.amazonaws.services.kinesisfirehose.model.Record;
import com.amazonaws.services.kinesisfirehose.model.ServiceUnavailableException;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.DeviceData;

import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by jakepiccolo on 11/17/15.
 */
public class DeviceDataDAOFirehose implements DeviceDataIngestDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(DeviceDataDAOFirehose.class);

    private final String deliveryStreamName;
    private final AmazonKinesisFirehose firehose;

    private static final String DATE_TIME_STRING_TEMPLATE = "yyyy-MM-dd HH:mm";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern(DATE_TIME_STRING_TEMPLATE);
    private static final Integer MAX_PUT_RECORDS = 500;
    private static final Integer MAX_BATCH_PUT_ATTEMPTS = 5;


    public DeviceDataDAOFirehose(final String deliveryStreamName, final AmazonKinesisFirehose firehose) {
        this.deliveryStreamName = deliveryStreamName;
        this.firehose = firehose;
    }


    //region DeviceDataIngestDAO implementation
    //-------------------------------------------------------------------------
    @Override
    public int batchInsertAll(final List<DeviceData> allDeviceData) {
        final List<Record> records = Lists.newArrayListWithCapacity(allDeviceData.size());

        for (final DeviceData data : allDeviceData) {
            records.add(toRecord(data));
        }

        final List<Record> failedRecords = batchInsertAllRecords(records);

        return allDeviceData.size() - failedRecords.size();
    }

    @Override
    public Class name() {
        return DeviceDataDAOFirehose.class;
    }
    //endregion


    /**
     * Partition and insert all records.
     * @return List of failed records.
     */
    private List<Record> batchInsertAllRecords(final List<Record> records) {
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
    private List<Record> batchInsertRecords(final List<Record> records) {
        int numAttempts = 0;
        List<Record> uninsertedRecords = records;

        while (!uninsertedRecords.isEmpty() && numAttempts < MAX_BATCH_PUT_ATTEMPTS) {
            numAttempts++;
            final PutRecordBatchRequest batchRequest = new PutRecordBatchRequest()
                    .withDeliveryStreamName(deliveryStreamName)
                    .withRecords(uninsertedRecords);

            try {
                final PutRecordBatchResult result = firehose.putRecordBatch(batchRequest);
                uninsertedRecords = failedRecords(uninsertedRecords, result);
            } catch (ServiceUnavailableException sue) {
                if (numAttempts < MAX_BATCH_PUT_ATTEMPTS) {
                    backoff(numAttempts);
                    continue;
                } else {
                    LOGGER.error("ServiceUnavailableException persists, out of retries. Failed to write {} records.",
                            uninsertedRecords.size());
                }
            }
        }

        return uninsertedRecords;
    }

    private static List<Record> failedRecords(final List<Record> attemptedRecords,
                                              final PutRecordBatchResult batchResult) {
        final List<Record> failed = Lists.newArrayList();
        if (batchResult.getFailedPutCount() == 0) {
            // All successful!
            return failed;
        }

        final List<PutRecordBatchResponseEntry> responseEntries = batchResult.getRequestResponses();
        for (int i = 0; i < responseEntries.size(); i++) {
            if (responseEntries.get(i).getErrorCode() != null) {
                LOGGER.error("Encountered error code while inserting record: {}", responseEntries.get(i).getErrorCode());
                failed.add(attemptedRecords.get(i));
            }
        }
        return failed;
    }

    private void backoff(int numberOfAttempts) {
        try {
            long sleepMillis = (long) Math.pow(2, numberOfAttempts) * 50;
            LOGGER.warn("Throttled by Firehose, sleeping for {} ms.", sleepMillis);
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while attempting exponential backoff.");
        }
    }

    private static String toString(final DateTime dateTime) {
        return dateTime.toString(DATE_TIME_FORMATTER);
    }

    private static String toString(final Integer value) {
        if (value == null) {
            return "0";
        }
        return value.toString();
    }

    private static Record toRecord(final DeviceData model) {
        final String pipeDelimited = Joiner.on("|").join(
                "0", // Dummy primary key
                model.accountId.toString(),
                model.deviceId.toString(),
                toString(model.ambientTemperature),
                toString(model.ambientLight),
                toString(model.ambientHumidity),
                toString(model.ambientAirQuality),
                toString(model.dateTimeUTC),
                toString(model.localTime()),
                toString(model.offsetMillis),
                toString(model.ambientLightVariance),
                toString(model.ambientLightPeakiness),
                toString(model.ambientAirQualityRaw),
                toString(model.ambientDustVariance),
                toString(model.ambientDustMin),
                toString(model.ambientDustMax),
                toString(model.firmwareVersion),
                toString(model.waveCount),
                toString(model.holdCount),
                toString(model.audioNumDisturbances),
                toString(model.audioPeakDisturbancesDB),
                toString(model.audioPeakBackgroundDB));
        final String data = pipeDelimited + "\n";
        return createRecord(data);
    }

    private static Record createRecord(final String data) {
        return new Record().withData(ByteBuffer.wrap(data.getBytes()));
    }
}
