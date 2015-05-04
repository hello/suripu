package com.hello.suripu.service.utils;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.logging.DataLogger;
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
                                                                            final RegistrationActionResults result){
        final LoggingProtos.RegistrationLog.Builder builder = LoggingProtos.RegistrationLog.newBuilder().setSenseId(this.senseId)
                .setAction(this.action.toString())
                .setResult(result.toString())
                .setTimestamp(DateTime.now().getMillis())
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

    private boolean postLog(){
        try{
            final LoggingProtos.BatchLogMessage.Builder builder = LoggingProtos.BatchLogMessage.newBuilder();
            builder.setLogType(LoggingProtos.BatchLogMessage.LogType.ONBOARDING_LOG);
            builder.addAllRegistrationLog(this.logs);

            this.dataLogger.put(this.senseId, builder.build().toByteArray());
            return true;
        }catch (AmazonServiceException awsEx){
            LOGGER.error("Post log message for sense {} failed: {}", this.senseId, awsEx.getErrorMessage());
        }

        return false;
    }

    private void bufferLog(final LoggingProtos.RegistrationLog log){
        this.logs.add(log);
    }

    private void logImpl(final Optional<String> pillId,
                             final String info,
                             final RegistrationActionResults result){
        final LoggingProtos.RegistrationLog log = getRegistrationLogBuilder(pillId,
                info,
                result)
                .build();
        bufferLog(log);
    }

    public void logFailure(final Optional<String> pillId,
                                  final String info){
        logImpl(pillId, info, RegistrationActionResults.FAILED);
    }

    public void logProgress(final Optional<String> pillId,
                               final String info){
        logImpl(pillId, info, RegistrationActionResults.IN_PROGRESS);
    }

    public void logSuccess(final Optional<String> pillId,
                                      final String info){
        logImpl(pillId, info, RegistrationActionResults.SUCCESS);
    }

    public void start(){
        logImpl(Optional.<String>absent(), "enter function call", RegistrationActionResults.START);
    }

    public void commit(){
        logImpl(Optional.<String>absent(), "exit function call", RegistrationActionResults.EXIT);
        this.postLog();
        this.logs.clear();
    }
}
