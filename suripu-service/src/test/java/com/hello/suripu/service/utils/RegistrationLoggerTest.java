package com.hello.suripu.service.utils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudfront.model.InvalidArgumentException;
import com.google.common.base.Optional;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.logging.DataLogger;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by pangwu on 5/4/15.
 */
public class RegistrationLoggerTest {
    private static final String testSenseId = "test sense";

    private DataLogger mockKenisisLogger(){
        final DataLogger logger = mock(DataLogger.class);
        return logger;
    }

    private DataLogger throwExceptionWhenGivenParams(final DataLogger logger, final String deviceId, final Exception exception){
        when(logger.put(eq(deviceId), (byte[])any())).thenThrow(exception);  // @pims: this is how to test exceptions without a python simulator in unit test
        return logger;
    }

    private DataLogger returnWhenGivenParams(final DataLogger logger, final String deviceId, final byte[] content, final String returnValue){
        when(logger.put(deviceId, content)).thenReturn(returnValue);
        return logger;
    }

    private RegistrationLogger writeALog(final RegistrationLogger logger){
        logger.logProgress(Optional.<String>absent(), "test started");
        return logger;
    }

    private void testException(final Exception exception){
        final DataLogger dataLogger = mockKenisisLogger();
        final RegistrationLogger logger = RegistrationLogger.create(testSenseId, PairAction.PAIR_MORPHEUS, "127.0.0.1", dataLogger);

        writeALog(logger);
        throwExceptionWhenGivenParams(dataLogger, testSenseId,
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
        final DataLogger dataLogger = mockKenisisLogger();
        final RegistrationLogger logger = RegistrationLogger.create(testSenseId, PairAction.PAIR_MORPHEUS, "127.0.0.1", dataLogger);
        writeALog(logger);
        returnWhenGivenParams(dataLogger, testSenseId, logger.getByteLog(), "");
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
        final DataLogger dataLogger = mockKenisisLogger();
        final RegistrationLogger logger = RegistrationLogger.create(testSenseId, PairAction.PAIR_MORPHEUS, "127.0.0.1", dataLogger);
        writeMoreThan20KToLogger(logger);
        returnWhenGivenParams(dataLogger, testSenseId, logger.getByteLog(), "");
        assertThat(logger.commit(), is(false));
    }

    @Test
    public void testStartItemIs1MillisEarlier(){
        final DataLogger dataLogger = mockKenisisLogger();
        final RegistrationLogger logger = RegistrationLogger.create(testSenseId, PairAction.PAIR_MORPHEUS, "127.0.0.1", dataLogger);
        writeALog(logger);  // trigger the start write
        final byte[] bytes = logger.getByteLog();
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
