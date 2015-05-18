package com.hello.suripu.service.utils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.kinesis.model.ProvisionedThroughputExceededException;
import com.google.common.base.Optional;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.util.PairAction;
import com.hello.suripu.core.util.PairingResults;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pangwu on 4/29/15.
 */
public class RegistrationLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationLogger.class);
    private static final long MAX_LOG_SIZE = 10 * 1024;  // 10K per commit

    private String senseId;
    private final DataLogger dataLogger;
    private final PairAction action;
    private final String ip;
    private Optional<Long> accountId = Optional.absent();
    private List<LoggingProtos.RegistrationLog> logs = new ArrayList<>();


    public void setSenseId(final String senseId){
        this.senseId = senseId;
    }


    public void setAccountId(final Long accountId){
        this.accountId = Optional.fromNullable(accountId);
    }


    private RegistrationLogger(final String senseId, final PairAction action, final String ip, final DataLogger logger){
        this.senseId = senseId;
        this.ip = ip;
        this.dataLogger = logger;
        this.action = action;

    }

    public static RegistrationLogger create(final String sendId, final PairAction action, final String ip, final DataLogger logger){
        return new RegistrationLogger(sendId, action, ip, logger);
    }

    private LoggingProtos.RegistrationLog.Builder getRegistrationLogBuilder(final Optional<String> pillId,
                                                                            final String info,
                                                                            final PairingResults result,
                                                                            final DateTime currentTime){
        final LoggingProtos.RegistrationLog.Builder builder = LoggingProtos.RegistrationLog.newBuilder().setSenseId(this.senseId)
                .setAction(this.action.toString())
                .setResult(result.toString())
                .setTimestamp(currentTime.getMillis())
                .setIpAddress(this.ip)
                .setInfo(info);
        if(this.accountId.isPresent()){
            builder.setAccountId(this.accountId.get());
        }

        if(pillId.isPresent()){
            builder.setPillId(pillId.get());
        }

        return builder;
    }

    protected byte[] toByteArray(){
        final LoggingProtos.BatchLogMessage.Builder builder = LoggingProtos.BatchLogMessage.newBuilder();
        builder.setLogType(LoggingProtos.BatchLogMessage.LogType.ONBOARDING_LOG);
        builder.addAllRegistrationLog(this.logs);

        final byte[] bytes = builder.build().toByteArray();
        return bytes;
    }

    private boolean postLog(){
        try{
            final byte[] bytes = toByteArray();
            if(bytes.length >= MAX_LOG_SIZE){
                LOGGER.warn("Log message too large, size {}", bytes.length);
                return false;
            }
            this.dataLogger.put(this.senseId, bytes);
            return true;
        } catch (ProvisionedThroughputExceededException ptEx){
            LOGGER.error("Provisioned Throughput Exceeded.");
        } catch (AmazonClientException awsEx){
            LOGGER.error("Post log message for sense {} failed: {}", this.senseId, awsEx.getMessage());
        } catch (Exception ex){
            LOGGER.error("Post log message for sense {} failed, generic error: {}", this.senseId, ex.getMessage());
        }

        return false;
    }

    private void logImpl(final Optional<String> pillId,
                             final String info,
                             final DateTime now,
                             final PairingResults result){
        if(this.logs.size() == 0){
            final LoggingProtos.RegistrationLog log = getRegistrationLogBuilder(pillId,
                    "enter function call",
                    PairingResults.START,
                    now)
                    .build();
            this.logs.add(log);
        }

        final LoggingProtos.RegistrationLog log = getRegistrationLogBuilder(pillId,
                info,
                result,
                now.plusMillis(1))
                .build();
        this.logs.add(log);
    }

    public void logFailure(final Optional<String> pillId,
                                  final String info){
        logImpl(pillId, info, DateTime.now(), PairingResults.FAILED);
    }

    public void logProgress(final Optional<String> pillId,
                               final String info){
        logImpl(pillId, info, DateTime.now(), PairingResults.IN_PROGRESS);
    }

    public void logSuccess(final Optional<String> pillId,
                                      final String info){
        logImpl(pillId, info, DateTime.now(), PairingResults.SUCCESS);
    }

    public boolean commit(){
        LOGGER.info("Committing onboarding log...");
        logImpl(Optional.<String>absent(), "exit function call", DateTime.now().plusMillis(1), PairingResults.EXIT);
        final boolean result = this.postLog();
        this.logs.clear();
        LOGGER.info("Onboarding log comitted.");
        return result;
    }
}
