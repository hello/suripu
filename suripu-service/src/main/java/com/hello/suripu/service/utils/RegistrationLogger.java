package com.hello.suripu.service.utils;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.logging.DataLogger;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by pangwu on 4/29/15.
 */
public class RegistrationLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationLogger.class);

    private final String senseId;
    private final DataLogger dataLogger;
    private final PairAction action;
    private final String ip;

    private RegistrationLogger(final String senseId, final PairAction action, final String ip, final DataLogger logger){
        this.senseId = senseId;
        this.ip = ip;
        this.dataLogger = logger;
        this.action = action;

    }

    public static RegistrationLogger create(final String sendId, final PairAction action, final String ip, final DataLogger logger){
        return new RegistrationLogger(sendId, action, ip, logger);
    }

    private static LoggingProtos.RegistrationLog.Builder getRegistrationLogBuilder(final String senseId,
                                                                            final String pillId,
                                                                            final PairAction action,
                                                                            final String info,
                                                                            final RegistrationActionResults result,
                                                                            final String ip){
        return LoggingProtos.RegistrationLog.newBuilder().setSenseId(senseId)
                .setPillId(pillId)
                .setAction(action.toString())
                .setResult(result.toString())
                .setTimestamp(DateTime.now().getMillis())
                .setIpAddress(ip)
                .setInfo(info);
    }

    private static boolean postLog(final DataLogger logger, final String senseId, final LoggingProtos.RegistrationLog log){
        try{
            logger.put(senseId, log.toByteArray());
            return true;
        }catch (AmazonServiceException awsEx){
            LOGGER.error("Post log message for sense {} failed: {}", senseId, awsEx.getErrorMessage());
        }

        return false;
    }

    private static boolean logImpl(final String senseId, final Optional<String> pillId,
                                     final PairAction action,
                                     final String info,
                                     final RegistrationActionResults result,
                                     final DataLogger registrationLogger,
                                     final String ip){
        final LoggingProtos.RegistrationLog log = getRegistrationLogBuilder(senseId,
                pillId.isPresent() ? pillId.get() : "",
                action,
                info,
                result,
                ip)
                .build();
        return postLog(registrationLogger, senseId, log);
    }

    public boolean logFailure(final Optional<String> pillId,
                                  final String info){
        return logImpl(this.senseId, pillId, this.action, info, RegistrationActionResults.FAILED, this.dataLogger, this.ip);
    }

    public boolean logProgress(final Optional<String> pillId,
                               final String info){
        return logImpl(this.senseId, pillId, this.action, info, RegistrationActionResults.IN_PROGRESS, this.dataLogger, this.ip);
    }

    public boolean logSuccess(final Optional<String> pillId,
                                      final String info){
        return logImpl(this.senseId, pillId, this.action, info, RegistrationActionResults.SUCCESS, this.dataLogger, this.ip);
    }
}
