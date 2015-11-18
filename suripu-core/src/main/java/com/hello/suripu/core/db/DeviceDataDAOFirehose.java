package com.hello.suripu.core.db;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResult;
import com.amazonaws.services.kinesisfirehose.model.Record;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.DeviceData;

import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by jakepiccolo on 11/17/15.
 */
public class DeviceDataDAOFirehose implements DeviceDataIngestDAO {

    private final String deliveryStreamName;
    private final AmazonKinesisFirehose firehose;

    private static final String DATE_TIME_STRING_TEMPLATE = "yyyy-MM-dd HH:mm";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern(DATE_TIME_STRING_TEMPLATE);
    private static final Integer MAX_PUT_RECORDS = 500;


    public DeviceDataDAOFirehose(final String deliveryStreamName, final AmazonKinesisFirehose firehose) {
        this.deliveryStreamName = deliveryStreamName;
        this.firehose = firehose;
    }


    //region DeviceDataIngestDAO implementation
    //-------------------------------------------------------------------------
    @Override
    public int batchInsertAll(final List<DeviceData> allDeviceData) {
        final List<Record> records = Lists.newArrayListWithCapacity(allDeviceData.size());
        int inserted = 0;

        for (final DeviceData data : allDeviceData) {
            records.add(toRecord(data));
        }

        for (final List<Record> recordsToInsert : Lists.partition(records, MAX_PUT_RECORDS)) {
            inserted += batchInsert(recordsToInsert);
        }

        return inserted;
    }

    @Override
    public Class name() {
        return DeviceDataDAOFirehose.class;
    }
    //endregion


    /**
     * Insert records that are <= the limit set by Amazon (MAX_PUT_RECORDS).
     */
    private int batchInsert(final List<Record> records) {
        /*
        TODO If FailedPutCount is greater than 0 (zero), retry the request.
        A retry of the entire batch of records is possible; however, we strongly recommend that you inspect the entire response and resend only those records that failed processing.
        This minimizes duplicate records and also reduces the total bytes sent (and corresponding charges).

        TODO If the PutRecordBatch operation throws a ServiceUnavailableException, back off and retry.
        If the exception persists, it is possible that the throughput limits have been exceeded for the delivery stream.
        */
        final PutRecordBatchRequest batchRequest = new PutRecordBatchRequest()
                .withDeliveryStreamName(deliveryStreamName)
                .withRecords(records);
        final PutRecordBatchResult result = firehose.putRecordBatch(batchRequest);
        return records.size() - result.getFailedPutCount();
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
                model.accountId.toString(),
                model.deviceId.toString(),
                toString(model.ambientTemperature),
                toString(model.ambientLight),
                toString(model.ambientLightVariance),
                toString(model.ambientLightPeakiness),
                toString(model.ambientHumidity),
                toString(model.ambientAirQuality),
                toString(model.ambientAirQualityRaw),
                toString(model.ambientDustVariance),
                toString(model.ambientDustMin),
                toString(model.ambientDustMax),
                toString(model.dateTimeUTC),
                toString(model.localTime()),
                toString(model.offsetMillis),
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
