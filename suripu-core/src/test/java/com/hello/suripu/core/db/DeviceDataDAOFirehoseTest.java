package com.hello.suripu.core.db;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.regions.Region;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient;
import com.amazonaws.services.kinesisfirehose.model.CreateDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.CreateDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.model.DeleteDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.DeleteDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.model.ListDeliveryStreamsRequest;
import com.amazonaws.services.kinesisfirehose.model.ListDeliveryStreamsResult;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResult;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult;
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
        final DateTime dateTime = new DateTime(2014, 1, 1, 1, 1);
        final List<DeviceData> dataList = ImmutableList.of(
                new DeviceData.Builder()
                        .withAccountId(1L)
                        .withDeviceId(2L)
                        .withDateTimeUTC(dateTime)
                        .withOffsetMillis(0)
                        .build(),
                new DeviceData.Builder()
                        .withAccountId(3L)
                        .withDeviceId(4L)
                        .withDateTimeUTC(dateTime.plusMinutes(1))
                        .withOffsetMillis(0)
                        .build()
        );
        final int result = dao.batchInsertAll(dataList);

        assertThat(result, is(2));
        assertThat(request.getDeliveryStreamName(), is(streamName));
        assertThat(request.getRecords().size(), is(2));
        assertThat(new String(request.getRecords().get(0).getData().array()), is("1|2|0|0|0|0|0|0|0|0|0|0|2014-01-01 01:01|2014-01-01 01:01|0|0|0|0|0|0|0\n"));
        assertThat(new String(request.getRecords().get(1).getData().array()), is("3|4|0|0|0|0|0|0|0|0|0|0|2014-01-01 01:02|2014-01-01 01:02|0|0|0|0|0|0|0\n"));
    }

}