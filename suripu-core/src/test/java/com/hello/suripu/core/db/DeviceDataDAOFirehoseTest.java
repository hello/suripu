package com.hello.suripu.core.db;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.regions.Region;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.CreateDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.CreateDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.model.DeleteDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.DeleteDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.model.ListDeliveryStreamsRequest;
import com.amazonaws.services.kinesisfirehose.model.ListDeliveryStreamsResult;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResponseEntry;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResult;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult;
import com.amazonaws.services.kinesisfirehose.model.ServiceUnavailableException;
import com.amazonaws.services.kinesisfirehose.model.UpdateDestinationRequest;
import com.amazonaws.services.kinesisfirehose.model.UpdateDestinationResult;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.DeviceData;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by jakepiccolo on 11/18/15.
 */
public class DeviceDataDAOFirehoseTest{

    private static final DateTime DATE_TIME = new DateTime(2014, 1, 1, 1, 1);
    private static final List<DeviceData> DATA_LIST = ImmutableList.of(
            new DeviceData.Builder()
                    .withAccountId(1L)
                    .withDeviceId(2L)
                    .withDateTimeUTC(DATE_TIME)
                    .withOffsetMillis(0)
                    .build(),
            new DeviceData.Builder()
                    .withAccountId(3L)
                    .withDeviceId(4L)
                    .withDateTimeUTC(DATE_TIME.plusMinutes(1))
                    .withOffsetMillis(0)
                    .build()
    );

    private static final List<String> EXPECTED_RESULTS = ImmutableList.of(
            "0|1|2|0|0|0|0|2014-01-01 01:01|2014-01-01 01:01|0|0|0|0|0|0|0|0|0|0|0|0|0\n",
            "0|3|4|0|0|0|0|2014-01-01 01:02|2014-01-01 01:02|0|0|0|0|0|0|0|0|0|0|0|0|0\n"
    );


    private class DummyFirehose implements AmazonKinesisFirehose {

        @Override
        public void setEndpoint(String s) {}

        @Override
        public void setRegion(Region region) {}

        @Override
        public CreateDeliveryStreamResult createDeliveryStream(CreateDeliveryStreamRequest createDeliveryStreamRequest) {
            return null;
        }

        @Override
        public DeleteDeliveryStreamResult deleteDeliveryStream(DeleteDeliveryStreamRequest deleteDeliveryStreamRequest) {
            return null;
        }

        @Override
        public DescribeDeliveryStreamResult describeDeliveryStream(DescribeDeliveryStreamRequest describeDeliveryStreamRequest) {
            return null;
        }

        @Override
        public ListDeliveryStreamsResult listDeliveryStreams(ListDeliveryStreamsRequest listDeliveryStreamsRequest) {
            return null;
        }

        @Override
        public PutRecordResult putRecord(PutRecordRequest putRecordRequest) {
            return null;
        }

        @Override
        public PutRecordBatchResult putRecordBatch(PutRecordBatchRequest putRecordBatchRequest) {
            return null;
        }

        @Override
        public UpdateDestinationResult updateDestination(UpdateDestinationRequest updateDestinationRequest) {
            return null;
        }

        @Override
        public void shutdown() {}

        @Override
        public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest amazonWebServiceRequest) {
            return null;
        }
    }


    @Test
    public void testBatchInsertAll() {
        final String streamName = "test_stream";
        final PutRecordBatchRequest request = new PutRecordBatchRequest();
        final AmazonKinesisFirehose firehose = new DummyFirehose() {
            @Override
            public PutRecordBatchResult putRecordBatch(final PutRecordBatchRequest putRecordBatchRequest) {
                request.withDeliveryStreamName(putRecordBatchRequest.getDeliveryStreamName())
                        .withRecords(putRecordBatchRequest.getRecords());
                return new PutRecordBatchResult().withFailedPutCount(0);
            }
        };
        final DeviceDataDAOFirehose dao = new DeviceDataDAOFirehose(streamName, firehose);
        final int result = dao.batchInsertAll(DATA_LIST);

        assertThat(result, is(2));
        assertThat(request.getDeliveryStreamName(), is(streamName));
        assertThat(request.getRecords().size(), is(2));
        assertThat(new String(request.getRecords().get(0).getData().array()), is(EXPECTED_RESULTS.get(0)));
        assertThat(new String(request.getRecords().get(1).getData().array()), is(EXPECTED_RESULTS.get(1)));
    }

    @Test
    public void testBatchInsertServiceUnavailable() {
        final String streamName = "test_stream";
        final PutRecordBatchRequest request = new PutRecordBatchRequest();
        final AmazonKinesisFirehose firehose = new DummyFirehose() {
            private int attemptsTried = 0;

            @Override
            public PutRecordBatchResult putRecordBatch(final PutRecordBatchRequest putRecordBatchRequest) {
                attemptsTried++;
                if (attemptsTried < 3) {
                    // Force the caller to try 3 times.
                    throw new ServiceUnavailableException("You have failed your family.");
                }

                request.withDeliveryStreamName(putRecordBatchRequest.getDeliveryStreamName())
                        .withRecords(putRecordBatchRequest.getRecords());
                return new PutRecordBatchResult().withFailedPutCount(0);
            }
        };

        final DeviceDataDAOFirehose dao = new DeviceDataDAOFirehose(streamName, firehose);
        final int result = dao.batchInsertAll(DATA_LIST);

        assertThat(result, is(2));
        assertThat(request.getDeliveryStreamName(), is(streamName));
        assertThat(request.getRecords().size(), is(2));
        assertThat(new String(request.getRecords().get(0).getData().array()), is(EXPECTED_RESULTS.get(0)));
        assertThat(new String(request.getRecords().get(1).getData().array()), is(EXPECTED_RESULTS.get(1)));
    }

    @Test
    public void testBatchInsertServiceUnavailableTooManyTimes() {
        final String streamName = "test_stream";
        final AmazonKinesisFirehose firehose = new DummyFirehose() {
            @Override
            public PutRecordBatchResult putRecordBatch(final PutRecordBatchRequest putRecordBatchRequest) {
                throw new ServiceUnavailableException("You have failed your family. For good this time.");
            }
        };

        final DeviceDataDAOFirehose dao = new DeviceDataDAOFirehose(streamName, firehose);
        final int result = dao.batchInsertAll(DATA_LIST);

        assertThat(result, is(0));
    }

    @Test
    public void testBatchInsertWithSomeFailedRecords() {
        final String streamName = "test_stream";
        final AmazonKinesisFirehose firehose = new DummyFirehose() {
            @Override
            public PutRecordBatchResult putRecordBatch(final PutRecordBatchRequest putRecordBatchRequest) {
                return new PutRecordBatchResult()
                        .withFailedPutCount(1)
                        .withRequestResponses(
                                new PutRecordBatchResponseEntry().withErrorCode("An error! Failed again!"),
                                new PutRecordBatchResponseEntry().withRecordId("Aha, finally you win. Kinda."));
            }
        };

        final DeviceDataDAOFirehose dao = new DeviceDataDAOFirehose(streamName, firehose);
        final int result = dao.batchInsertAll(DATA_LIST);

        assertThat(result, is(1));
    }

}