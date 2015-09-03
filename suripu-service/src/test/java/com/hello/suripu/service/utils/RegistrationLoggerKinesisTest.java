package com.hello.suripu.service.utils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudfront.model.InvalidArgumentException;
import com.google.common.base.Optional;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.util.PairAction;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 5/4/15.
 */
public class RegistrationLoggerKinesisTest {
    private static final String testSenseId = "test sense";



    private RegistrationLogger writeALog(final RegistrationLogger logger){
        logger.logProgress(Optional.<String>absent(), "test started");
        return logger;
    }

    private void testException(final Exception exception){
        final DataLogger dataLogger = DataLoggerTestHelper.mockDataLogger();
        final RegistrationLogger logger = RegistrationLoggerKinesis.create(testSenseId, PairAction.PAIR_MORPHEUS, "127.0.0.1", dataLogger);

        writeALog(logger);
        DataLoggerTestHelper.stubPutAnyException(dataLogger, testSenseId,
                exception);
        final boolean actual = logger.commit();
        assertThat(actual, is(false));
    }

    @Test
    public void testProvisionedThroughputExceededException(){
        testException(new com.amazonaws.services.kinesis.model.ProvisionedThroughputExceededException(""));
    }

    @Test
    public void testGenericExceptionException(){
        testException(new InvalidArgumentException("generic exception"));
    }

    @Test
    public void testCommitAmazonAWSServiceException(){
        testException(new AmazonServiceException("AmazonServiceException exception"));
    }

    @Test
    public void testCommit(){
        final DataLogger dataLogger = DataLoggerTestHelper.mockDataLogger();
        final RegistrationLoggerKinesis logger = RegistrationLoggerKinesis.create(testSenseId, PairAction.PAIR_MORPHEUS, "127.0.0.1", dataLogger);
        writeALog(logger);
        DataLoggerTestHelper.stubPut(dataLogger, testSenseId, logger.toByteArray(), "");
        assertThat(logger.commit(), is(true));
    }

    private RegistrationLogger writeMoreThan20KToLogger(final RegistrationLogger logger){
        for(int i = 0; i < 2048 * 10; i++) {
            writeALog(logger);
        }
        return logger;
    }

    @Test
    public void testCommitLogMessageTooLarge(){
        final DataLogger dataLogger = DataLoggerTestHelper.mockDataLogger();
        final RegistrationLoggerKinesis logger = RegistrationLoggerKinesis.create(testSenseId, PairAction.PAIR_MORPHEUS, "127.0.0.1", dataLogger);
        writeMoreThan20KToLogger(logger);
        DataLoggerTestHelper.stubPut(dataLogger, testSenseId, logger.toByteArray(), "");
        assertThat(logger.commit(), is(false));
    }

    @Test
    public void testStartItemIs1MillisEarlier(){
        final DataLogger dataLogger = DataLoggerTestHelper.mockDataLogger();
        final RegistrationLoggerKinesis logger = RegistrationLoggerKinesis.create(testSenseId, PairAction.PAIR_MORPHEUS, "127.0.0.1", dataLogger);
        writeALog(logger);  // trigger the start write
        final byte[] bytes = logger.toByteArray();
        try {
            final LoggingProtos.BatchLogMessage batchLogMessage = LoggingProtos.BatchLogMessage.parseFrom(bytes);
            assertThat(batchLogMessage.getLogType(), is(LoggingProtos.BatchLogMessage.LogType.ONBOARDING_LOG));
            assertThat(batchLogMessage.getRegistrationLogCount(), is(2));
            final long ts1 = batchLogMessage.getRegistrationLog(0).getTimestamp();
            final long ts2 = batchLogMessage.getRegistrationLog(1).getTimestamp();
            assertThat(ts1, is(ts2-1));

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            assertThat(true, is(false));
        }
    }
}
