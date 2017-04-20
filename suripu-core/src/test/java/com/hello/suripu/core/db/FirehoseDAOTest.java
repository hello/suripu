package com.hello.suripu.core.db;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.regions.Region;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseAsync;
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
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.actions.Action;
import com.hello.suripu.core.actions.ActionFirehoseDAO;
import com.hello.suripu.core.actions.ActionResult;
import com.hello.suripu.core.actions.ActionType;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by ksg on 1/25/17
 */
public class FirehoseDAOTest {

    private static final String STREAM_NAME = "test_stream";

    private static List<Action> actions = ImmutableList.of(
            new Action(1L, ActionType.LOGIN, Optional.of(ActionResult.OKAY.string()), DateTime.now(DateTimeZone.UTC).minusMinutes(5), Optional.of(0)),
            new Action(2L, ActionType.LOGIN, Optional.of(ActionResult.FAIL.string()), DateTime.now(DateTimeZone.UTC).minusMinutes(10), Optional.of(0)),
            new Action(3L, ActionType.LOGIN, Optional.of(ActionResult.OKAY.string()), DateTime.now(DateTimeZone.UTC).minusMinutes(15), Optional.of(0))
    );

    private class DummyFirehoseAsync implements AmazonKinesisFirehoseAsync {

        @Override
        public Future<CreateDeliveryStreamResult> createDeliveryStreamAsync(CreateDeliveryStreamRequest createDeliveryStreamRequest) {
            return null;
        }

        @Override
        public Future<CreateDeliveryStreamResult> createDeliveryStreamAsync(CreateDeliveryStreamRequest createDeliveryStreamRequest, AsyncHandler<CreateDeliveryStreamRequest, CreateDeliveryStreamResult> asyncHandler) {
            return null;
        }

        @Override
        public Future<DeleteDeliveryStreamResult> deleteDeliveryStreamAsync(DeleteDeliveryStreamRequest deleteDeliveryStreamRequest) {
            return null;
        }

        @Override
        public Future<DeleteDeliveryStreamResult> deleteDeliveryStreamAsync(DeleteDeliveryStreamRequest deleteDeliveryStreamRequest, AsyncHandler<DeleteDeliveryStreamRequest, DeleteDeliveryStreamResult> asyncHandler) {
            return null;
        }

        @Override
        public Future<DescribeDeliveryStreamResult> describeDeliveryStreamAsync(DescribeDeliveryStreamRequest describeDeliveryStreamRequest) {
            return null;
        }

        @Override
        public Future<DescribeDeliveryStreamResult> describeDeliveryStreamAsync(DescribeDeliveryStreamRequest describeDeliveryStreamRequest, AsyncHandler<DescribeDeliveryStreamRequest, DescribeDeliveryStreamResult> asyncHandler) {
            return null;
        }

        @Override
        public Future<ListDeliveryStreamsResult> listDeliveryStreamsAsync(ListDeliveryStreamsRequest listDeliveryStreamsRequest) {
            return null;
        }

        @Override
        public Future<ListDeliveryStreamsResult> listDeliveryStreamsAsync(ListDeliveryStreamsRequest listDeliveryStreamsRequest, AsyncHandler<ListDeliveryStreamsRequest, ListDeliveryStreamsResult> asyncHandler) {
            return null;
        }

        @Override
        public Future<PutRecordResult> putRecordAsync(PutRecordRequest putRecordRequest) {
            return null;
        }

        @Override
        public Future<PutRecordResult> putRecordAsync(PutRecordRequest putRecordRequest, AsyncHandler<PutRecordRequest, PutRecordResult> asyncHandler) {
            return null;
        }

        @Override
        public Future<PutRecordBatchResult> putRecordBatchAsync(PutRecordBatchRequest putRecordBatchRequest) {
            return null;
        }

        @Override
        public Future<PutRecordBatchResult> putRecordBatchAsync(PutRecordBatchRequest putRecordBatchRequest, AsyncHandler<PutRecordBatchRequest, PutRecordBatchResult> asyncHandler) {
            return null;
        }

        @Override
        public Future<UpdateDestinationResult> updateDestinationAsync(UpdateDestinationRequest updateDestinationRequest) {
            return null;
        }

        @Override
        public Future<UpdateDestinationResult> updateDestinationAsync(UpdateDestinationRequest updateDestinationRequest, AsyncHandler<UpdateDestinationRequest, UpdateDestinationResult> asyncHandler) {
            return null;
        }

        @Override
        public void setEndpoint(String endpoint) {

        }

        @Override
        public void setRegion(Region region) {

        }

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
        public void shutdown() {

        }

        @Override
        public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
            return null;
        }
    }

        @Test
    public void testBatchInsertAllSuccess() {
        final PutRecordBatchRequest request = new PutRecordBatchRequest().withDeliveryStreamName(STREAM_NAME);

        final AmazonKinesisFirehoseAsync firehose = new DummyFirehoseAsync() {
            @Override
            public Future<PutRecordBatchResult> putRecordBatchAsync(PutRecordBatchRequest putRecordBatchRequest, AsyncHandler<PutRecordBatchRequest, PutRecordBatchResult> asyncHandler) {
                request.withDeliveryStreamName(putRecordBatchRequest.getDeliveryStreamName())
                        .withRecords(putRecordBatchRequest.getRecords());
                final PutRecordBatchResult result = new PutRecordBatchResult().withFailedPutCount(0);
                asyncHandler.onSuccess(request, result);
                return new Future<PutRecordBatchResult>() {
                    @Override
                    public boolean cancel(boolean mayInterruptIfRunning) { return false; }

                    @Override
                    public boolean isCancelled() { return false; }

                    @Override
                    public boolean isDone() { return true; }

                    @Override
                    public PutRecordBatchResult get() throws InterruptedException, ExecutionException {
                        return result;
                    }

                    @Override
                    public PutRecordBatchResult get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                        return null;
                    }
                };
            }
        };

        final ActionFirehoseDAO dao = new ActionFirehoseDAO(STREAM_NAME, firehose);
        final int inserted = dao.batchInsertAll(actions);

        assertThat(inserted, is(actions.size()));
        assertThat(request.getRecords().size(), is (actions.size()));

        final String firstRecordString = new String (dao.toRecord(actions.get(0)).getData().array());
        assertThat(new String(request.getRecords().get(0).getData().array()).equals(firstRecordString), is(true));
    }

    @Test
    public void testServiceUnavailable() {
        final AmazonKinesisFirehoseAsync firehose = new DummyFirehoseAsync() {
            @Override
            public Future<PutRecordBatchResult> putRecordBatchAsync(PutRecordBatchRequest putRecordBatchRequest, AsyncHandler<PutRecordBatchRequest, PutRecordBatchResult> asyncHandler) {
               throw new ServiceUnavailableException("I said nope for the last time.");
            }
        };

        final ActionFirehoseDAO dao = new ActionFirehoseDAO(STREAM_NAME, firehose);
        final int inserted = dao.batchInsertAll(actions);

        assertThat(inserted, is(0));
    }

    @Test
    public void testFailedSomeRecords() {
        final int numFail = 1;
        final PutRecordBatchRequest request = new PutRecordBatchRequest().withDeliveryStreamName(STREAM_NAME);

        final AmazonKinesisFirehoseAsync firehose = new DummyFirehoseAsync() {
            @Override
            public Future<PutRecordBatchResult> putRecordBatchAsync(PutRecordBatchRequest putRecordBatchRequest, AsyncHandler<PutRecordBatchRequest, PutRecordBatchResult> asyncHandler) {
                final PutRecordBatchResult result = new PutRecordBatchResult().withFailedPutCount(numFail)
                        .withRequestResponses(
                                new PutRecordBatchResponseEntry().withErrorCode("Loser, why do you keep failing!"),
                                new PutRecordBatchResponseEntry().withRecordId("okay, you made it."),
                                new PutRecordBatchResponseEntry().withRecordId("Smart Trump, always whining.")
                        );
                request.withDeliveryStreamName(putRecordBatchRequest.getDeliveryStreamName())
                        .withRecords(putRecordBatchRequest.getRecords());
                if (asyncHandler != null) {
                    asyncHandler.onSuccess(request, result);
                }
                return new Future<PutRecordBatchResult>() {
                    @Override
                    public boolean cancel(boolean mayInterruptIfRunning) { return false; }

                    @Override
                    public boolean isCancelled() { return false; }

                    @Override
                    public boolean isDone() { return false; }

                    @Override
                    public PutRecordBatchResult get() throws InterruptedException, ExecutionException { return result; }

                    @Override
                    public PutRecordBatchResult get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                        return null;
                    }
                };
            }
        };

        final ActionFirehoseDAO dao = new ActionFirehoseDAO(STREAM_NAME, firehose);
        final int inserted = dao.batchInsertAll(actions);

        assertThat(inserted, is(actions.size() - numFail));
        assertThat(request.getRecords().size(), is(1)); // last remaining failed record after retries
    }
}
