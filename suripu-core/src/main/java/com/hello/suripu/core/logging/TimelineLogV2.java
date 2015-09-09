package com.hello.suripu.core.logging;

import com.google.common.base.Optional;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.models.TimelineLog;
import com.hello.suripu.core.util.AlgorithmType;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.InvalidNightType;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTimeZone;

import java.util.UUID;

/**
 * Created by benjo on 7/31/15.

 This class is basically a wrapper around a protobuf builder.
 But, it also is easy for serialization and deserialization of this protobuf


 */
public class TimelineLogV2 {

    private final LoggingProtos.TimelineLog.Builder builder;
    private final Long accountId; //use for partition

    public static Optional<TimelineLogV2> createFromProtobuf (final  String protobufBase64)  {
        return createFromProtobuf(Base64.decodeBase64(protobufBase64));
    }

    public static Optional<TimelineLogV2> createFromProtobuf (final  byte [] protobuf)  {
        final LoggingProtos.TimelineLog log;
        try {
            log = LoggingProtos.TimelineLog.parseFrom(protobuf);

            if (!log.hasAccountId()) {
                return Optional.absent();
            }

            return Optional.of(new TimelineLogV2(log,log.getAccountId()));

        }
        catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return Optional.absent();
    }

    public TimelineLogV2(final LoggingProtos.TimelineLog log, final Long accountId) {
        builder = LoggingProtos.TimelineLog.newBuilder(log);
        this.accountId = accountId;
        builder.setAccountId(accountId);

    }
    public TimelineLogV2( final Long accountId) {
        builder = LoggingProtos.TimelineLog.newBuilder();
        this.accountId = accountId;
        builder.setAccountId(accountId);
    }

    public byte [] toProtoBuf() {
        return builder.build().toByteArray();
    }

    public String toProtobufBase64() {
        return Base64.encodeBase64URLSafeString(toProtoBuf());
    }

    public void addNotUsingIntendedAlgorithmError() {
        builder.addErrors(LoggingProtos.TimelineLog.ErrorType.INTENDED_ALGORITHM_FAILURE);
    }

    public void addMisingEventsError() {
        builder.addErrors(LoggingProtos.TimelineLog.ErrorType.MISSING_KEY_EVENTS);
    }

    public void addError(final InvalidNightType invalidNightType) {

        switch (invalidNightType) {

            case TIMESPAN_TOO_SHORT:
                builder.addErrors(LoggingProtos.TimelineLog.ErrorType.TIMESPAN_TOO_SHORT);
                break;
            case LOW_AMP_DATA:
                builder.addErrors(LoggingProtos.TimelineLog.ErrorType.LOW_AMPLITUDE_DATA);
                break;
            case NOT_ENOUGH_DATA:
                builder.addErrors(LoggingProtos.TimelineLog.ErrorType.NOT_ENOUGH_DATA);
                break;
            case NO_DATA:
                builder.addErrors(LoggingProtos.TimelineLog.ErrorType.NO_DATA);
                break;
            case INVALID_SLEEP_SCORE:
                builder.addErrors(LoggingProtos.TimelineLog.ErrorType.INVALID_SLEEP_SCORE);
                break;
            case MISSING_KEY_EVENTS:
                builder.addErrors(LoggingProtos.TimelineLog.ErrorType.MISSING_KEY_EVENTS);
                break;
        }

    }

    private LoggingProtos.TimelineLog.AlgType toAlgEnum(final AlgorithmType algorithmType) {
        switch (algorithmType) {

            case WUPANG:
                return LoggingProtos.TimelineLog.AlgType.WUPANG;
            case VOTING:
                return LoggingProtos.TimelineLog.AlgType.VOTING;
            case HMM:
                return LoggingProtos.TimelineLog.AlgType.HMM;
            case LAYERED_HMM:
                return LoggingProtos.TimelineLog.AlgType.LAYERED_HMM;
        }

        return LoggingProtos.TimelineLog.AlgType.NONE;
    }

    private AlgorithmType fromAlgEnum(final LoggingProtos.TimelineLog.AlgType algType) {
        switch (algType) {
            case WUPANG:
                return AlgorithmType.WUPANG;
            case HMM:
                return AlgorithmType.HMM;
            case VOTING:
                return AlgorithmType.VOTING;
            case LAYERED_HMM:
                return AlgorithmType.LAYERED_HMM;
        }

        return AlgorithmType.NONE;
    }

    public void setIntendedAlgorithm(final AlgorithmType algorithmType) {
        builder.setIntendedAlgorithm(toAlgEnum(algorithmType));
    }

    public void setUsedAlgorithm(final AlgorithmType algorithmType) {
        builder.setUsedAlgorithm(toAlgEnum(algorithmType));
    }

    public void setCreatedTime(final Long currentTimeUtc) {
        builder.setTimestampWhenLogGenerated(currentTimeUtc);
    }
    public void setNightOfTimeline(final Long nightOfTimeline) {
        builder.setNightOfTimeline(nightOfTimeline);
    }
    public void setNightOfTimeline(final String nightOfTimeline) {
        builder.setNightOfTimeline(DateTimeUtil.ymdStringToDateTime(nightOfTimeline).withZone(DateTimeZone.UTC).getMillis());
    }

    public void setLogUUID(final UUID uuid) {
        builder.setLogUuidLower(uuid.getLeastSignificantBits());
        builder.setLogUuidUpper(uuid.getMostSignificantBits());
    }

    public String getParitionKey() {
        return accountId.toString();
    }

    public TimelineLog getAsV1Log() {
        final LoggingProtos.TimelineLog log = builder.build();

        String usedAlgorithm = "none";
        String version = "none";
        if (log.hasUsedAlgorithm()) {
            usedAlgorithm = fromAlgEnum(log.getUsedAlgorithm()).name();


            if (log.hasIntendedAlgorithm()) {
                if (!log.getIntendedAlgorithm().equals(log.getUsedAlgorithm())) {
                    version = "BACKUP";
                }
            }

        }

        Long targetDate = 0L;

        if (log.hasNightOfTimeline()) {
            targetDate = log.getNightOfTimeline();
        }

        Long createDate = 0L;

        if (log.hasTimestampWhenLogGenerated()) {
            createDate = log.getTimestampWhenLogGenerated();
        }


        return new TimelineLog(usedAlgorithm,version,createDate,targetDate);
    }



}
