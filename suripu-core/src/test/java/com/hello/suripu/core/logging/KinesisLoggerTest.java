package com.hello.suripu.core.logging;

import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import com.amazonaws.services.kinesis.model.PutRecordsResultEntry;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.configuration.QueueName;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static ch.qos.logback.core.encoder.ByteArrayUtil.hexStringToByteArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by ksg on 4/15/16
 */
public class KinesisLoggerTest {

    private class DummyKinesisClient extends AmazonKinesisAsyncClient {
        @Override
        public PutRecordResult putRecord(PutRecordRequest putRecordRequest) {
            return null;
        }

        @Override
        public PutRecordsResult putRecords(PutRecordsRequest putRecordsRequest) {
            return null;
        }
    }

    @Test
    public void testBatchPutsFailAll() {
        final DummyKinesisClient dummyClient = new DummyKinesisClient() {
            @Override
            public PutRecordResult putRecord(PutRecordRequest putRecordRequest) {
                return new PutRecordResult().withSequenceNumber(null).withShardId(null);
            }

            @Override
            public PutRecordsResult putRecords(PutRecordsRequest putRecordsRequest) {
                final List<PutRecordsResultEntry> putRecordsResultEntryList = Lists.newArrayList();
                for (int i = 0; i < putRecordsRequest.getRecords().size(); i++) {
                    final PutRecordsResultEntry result = new PutRecordsResultEntry();
                    result.setErrorCode("error");
                    result.setErrorMessage("you suck");
                    putRecordsResultEntryList.add(result);
                }
                final PutRecordsResult putRecordsResult = new PutRecordsResult();
                putRecordsResult.setRecords(putRecordsResultEntryList);
                putRecordsResult.setFailedRecordCount(putRecordsRequest.getRecords().size());
                return putRecordsResult;
            }
        };

        Map<QueueName, String> streams = Maps.newHashMap();
        streams.put(QueueName.SENSE_SENSORS_DATA_FANOUT_ONE, "test");

        final KinesisLoggerFactory kinesisLoggerFactory = new KinesisLoggerFactory(dummyClient, streams);
        final DataLogger dataLogger = kinesisLoggerFactory.get(QueueName.SENSE_SENSORS_DATA_FANOUT_ONE);


        final List<DataLoggerBatchPayload> batch = Lists.newArrayList();
        batch.add(new DataLoggerBatchPayload("deviceid_1", hexStringToByteArray("abcdef123")));
        batch.add(new DataLoggerBatchPayload("deviceid_2", hexStringToByteArray("abcdef1234")));

        final KinesisBatchPutResult result = dataLogger.putRecords(batch);
        assertThat(result.batchSize, is(batch.size()));
        assertThat(result.numSuccesses, is(0));
        assertThat(result.successPuts.size(), is(batch.size()));
        assertThat(result.successPuts.get(0), is(false));
        assertThat(result.successPuts.get(1), is(false));
    }

    @Test
    public void testBatchPutsFailFirstRecord() {
        final String firstId = "deviceid_1";

        final DummyKinesisClient dummyClient = new DummyKinesisClient() {
            @Override
            public PutRecordResult putRecord(PutRecordRequest putRecordRequest) {
                if (putRecordRequest.getPartitionKey().equalsIgnoreCase(firstId)) {
                    return new PutRecordResult().withSequenceNumber(null).withShardId(null);
                }
                return new PutRecordResult().withSequenceNumber(putRecordRequest.getPartitionKey());
            }

            @Override
            public PutRecordsResult putRecords(PutRecordsRequest putRecordsRequest) {
                final List<PutRecordsResultEntry> putRecordsResultEntryList = Lists.newArrayList();
                for (int i = 0; i < putRecordsRequest.getRecords().size(); i++) {
                    final PutRecordsResultEntry result = new PutRecordsResultEntry();
                    if (putRecordsRequest.getRecords().get(i).getPartitionKey().equalsIgnoreCase(firstId)) {
                        result.setErrorCode("error");
                        result.setErrorMessage("you suck");
                    } else {
                        result.setErrorCode(null);
                        result.setErrorMessage("");
                    }
                    putRecordsResultEntryList.add(result);
                }
                final PutRecordsResult putRecordsResult = new PutRecordsResult();
                putRecordsResult.setRecords(putRecordsResultEntryList);
                putRecordsResult.setFailedRecordCount(putRecordsRequest.getRecords().size());
                return putRecordsResult;
            }
        };


        Map<QueueName, String> streams = Maps.newHashMap();
        streams.put(QueueName.SENSE_SENSORS_DATA_FANOUT_ONE, "test");

        final KinesisLoggerFactory kinesisLoggerFactory = new KinesisLoggerFactory(dummyClient, streams);
        final DataLogger dataLogger = kinesisLoggerFactory.get(QueueName.SENSE_SENSORS_DATA_FANOUT_ONE);


        final List<DataLoggerBatchPayload> batch = Lists.newArrayList();
        batch.add(new DataLoggerBatchPayload(firstId, hexStringToByteArray("abcdef123")));
        batch.add(new DataLoggerBatchPayload("deviceid_2", hexStringToByteArray("abcdef1234")));

        final KinesisBatchPutResult result = dataLogger.putRecords(batch);
        assertThat(result.batchSize, is(batch.size()));
        assertThat(result.numSuccesses, is(1));
        assertThat(result.successPuts.size(), is(batch.size()));
        assertThat(result.successPuts.get(0), is(false));
        assertThat(result.successPuts.get(1), is(true));
    }
}
