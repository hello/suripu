package com.hello.suripu.core.models.timeline.v2;

import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.util.AlgorithmType;
import com.hello.suripu.core.util.TimelineError;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.hello.suripu.api.logging.LoggingProtos.TimelineLog.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by benjo on 7/31/15.

 This class is basically a wrapper around a protobuf builder.
 But, it also is easy for serialization and deserialization of this protobuf


 */
public class TimelineLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineLog.class);

    private final LoggingProtos.BatchLogMessage.Builder builder; //mutable, but only to add timeline log messages
    private final long dateOfNight;
    private final long accountId; //use for partition as well

    private static final BiMap<TimelineError, ErrorType> invalidNightErrorMap;
    private static final BiMap<AlgorithmType,AlgType> algorithmTypeMap;

    static {
        //initialize static maps
        invalidNightErrorMap = HashBiMap.create();
        invalidNightErrorMap.put(TimelineError.NO_ERROR, ErrorType.NO_ERROR);
        invalidNightErrorMap.put(TimelineError.TIMESPAN_TOO_SHORT, ErrorType.TIMESPAN_TOO_SHORT);
        invalidNightErrorMap.put(TimelineError.LOW_AMP_DATA, ErrorType.LOW_AMPLITUDE_DATA);
        invalidNightErrorMap.put(TimelineError.NOT_ENOUGH_DATA, ErrorType.NOT_ENOUGH_DATA);
        invalidNightErrorMap.put(TimelineError.NO_DATA, ErrorType.NO_DATA);
        invalidNightErrorMap.put(TimelineError.INVALID_SLEEP_SCORE, ErrorType.INVALID_SLEEP_SCORE);
        invalidNightErrorMap.put(TimelineError.MISSING_KEY_EVENTS, ErrorType.MISSING_KEY_EVENTS);
        invalidNightErrorMap.put(TimelineError.NOT_ENOUGH_HOURS_OF_SLEEP, ErrorType.NOT_ENOUGH_SLEEP_TIME);
        invalidNightErrorMap.put(TimelineError.DATA_GAP_TOO_LARGE, ErrorType.NOT_ENOUGH_SLEEP_TIME);
        invalidNightErrorMap.put(TimelineError.EVENTS_OUT_OF_ORDER, ErrorType.NOT_ENOUGH_SLEEP_TIME);



        algorithmTypeMap = HashBiMap.create();
        algorithmTypeMap.put(AlgorithmType.NONE,AlgType.NO_ALGORITHM);
        algorithmTypeMap.put(AlgorithmType.WUPANG,AlgType.WUPANG);
        algorithmTypeMap.put(AlgorithmType.HMM,AlgType.HMM);
        algorithmTypeMap.put(AlgorithmType.ONLINE_HMM,AlgType.ONLINE_HMM);
        algorithmTypeMap.put(AlgorithmType.VOTING,AlgType.VOTING);




    }


    public static Optional<TimelineLog> createFromProtobuf (final  String protobufBase64)  {
        return createFromProtobuf(Base64.decodeBase64(protobufBase64));
    }

    public static Optional<TimelineLog> createFromProtobuf (final  byte [] protobuf)  {
        final LoggingProtos.BatchLogMessage log;
        try {
            log = LoggingProtos.BatchLogMessage.parseFrom(protobuf);

            if (log.getTimelineLogCount() == 0) {
                return Optional.absent();
            }

            final LoggingProtos.TimelineLog timelineLog = log.getTimelineLog(log.getTimelineLogCount() - 1);

            if (!timelineLog.hasAccountId() || !timelineLog.hasNightOfTimeline()) {
                return Optional.absent();
            }

            return Optional.of(new TimelineLog(log.toBuilder(),timelineLog.getAccountId(),timelineLog.getNightOfTimeline()));

        }
        catch (InvalidProtocolBufferException e) {
            LOGGER.error(e.getMessage());
        }

        return Optional.absent();
    }


    public TimelineLog(final Long accountId, final long dateOfNight) {
        this.dateOfNight = dateOfNight;
        this.accountId = accountId;

        builder = LoggingProtos.BatchLogMessage.newBuilder();
        builder.setLogType(LoggingProtos.BatchLogMessage.LogType.TIMELINE_LOG);
        builder.setReceivedAt(DateTime.now().withZone(DateTimeZone.UTC).getMillis());
    }

    private TimelineLog(final LoggingProtos.BatchLogMessage.Builder builder, final long accountId, final long dateOfNight) {
        this.builder = builder;
        this.accountId = accountId;
        this.dateOfNight = dateOfNight;
    }

    public byte [] toProtoBuf() {
        return builder.build().toByteArray();
    }

    public String toProtobufBase64() {
        return Base64.encodeBase64URLSafeString(toProtoBuf());
    }

    public String getParitionKey() {
        return Long.valueOf(accountId).toString();
    }

    public void addMessage(final TimelineError timelineError) {
        final ErrorType errorType = invalidNightErrorMap.get(timelineError);

        if (errorType == null) {
            return;
        }

        builder.addTimelineLog(
                LoggingProtos.TimelineLog.newBuilder()
                        .setAccountId(accountId)
                        .setNightOfTimeline(dateOfNight)
                        .setError(errorType)
                        .build());

    }

    public void addMessage(final AlgorithmType algorithmType) {
        final AlgType algType = algorithmTypeMap.get(algorithmType);

        if (algType == null) {
            return;
        }

        builder.addTimelineLog(
                LoggingProtos.TimelineLog.newBuilder()
                        .setAccountId(accountId)
                        .setNightOfTimeline(dateOfNight)
                        .setAlgorithm(algType)
                        .build());
    }

    public void addMessage(final AlgorithmType algorithmType, final float score, final String model) {
        final AlgType algType = algorithmTypeMap.get(algorithmType);

        if (algType == null) {
            return;
        }

        builder.addTimelineLog(
                LoggingProtos.TimelineLog.newBuilder()
                        .setAccountId(accountId)
                        .setNightOfTimeline(dateOfNight)
                        .setAlgorithm(algType)
                        .setModelName(model)
                        .setModelScore(score)
                        .build());
    }


    public com.hello.suripu.core.models.TimelineLog getAsV1Log() {
        final LoggingProtos.BatchLogMessage log = builder.build();

        String intendedAlgorithm = "none";
        String usedAlgorithm = "none";

        String version = "";
        boolean isFirst = true;
        //get first and last alg
        for (LoggingProtos.TimelineLog timelineLog : log.getTimelineLogList()) {
            if (timelineLog.hasAlgorithm()) {

                //get internal name
                final AlgorithmType algType = algorithmTypeMap.inverse().get(timelineLog.getAlgorithm());

                if (algType != null) {
                    usedAlgorithm = algType.name();
                }

                if (isFirst) {
                    intendedAlgorithm = usedAlgorithm;
                    isFirst = false;
                }
            }
        }

        if (!intendedAlgorithm.equals(usedAlgorithm)) {
            version = "backup";
        }

        long timeOfLog = 0;

        if (log.hasReceivedAt()) {
            timeOfLog = log.getReceivedAt();
        }

        return new com.hello.suripu.core.models.TimelineLog(usedAlgorithm,version,timeOfLog,dateOfNight);
    }



}
