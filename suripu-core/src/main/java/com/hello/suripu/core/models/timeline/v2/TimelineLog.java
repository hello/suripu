package com.hello.suripu.core.models.timeline.v2;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.util.AlgorithmType;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.TimelineError;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.hello.suripu.api.logging.LoggingProtos.TimelineLog.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by benjo on 7/31/15.

 This class is basically a wrapper around a protobuf builder.
 But, it also is easy for serialization and deserialization of this protobuf


 */
public class TimelineLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineLog.class);

    public static final long DEFAULT_TEST_GROUP = 0L;

    private final LoggingProtos.BatchLogMessage.Builder builder; //mutable, but only to add timeline log messages
    private final long dateOfNight;
    private final String dateOfNightString;
    private final long accountId; //use for partition as well
    private final long timestamp;
    private final long testGroup;

    private static final BiMap<TimelineError, ErrorType> invalidNightErrorMap;
    private static final BiMap<AlgorithmType,AlgType> algorithmTypeMap;
    private static final BiMap<Event.Type,SleepEventType> eventTypeMap;

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
        invalidNightErrorMap.put(TimelineError.DATA_GAP_TOO_LARGE, ErrorType.DATA_GAP_TOO_LARGE);
        invalidNightErrorMap.put(TimelineError.EVENTS_OUT_OF_ORDER, ErrorType.EVENTS_OUT_OF_ORDER);
        invalidNightErrorMap.put(TimelineError.PARTNER_FILTER_REJECTED_DATA,ErrorType.PARTNER_FILTER_REJECTED_MY_DATA);
        invalidNightErrorMap.put(TimelineError.UNEXEPECTED, ErrorType.UNEXEPECTED);
        invalidNightErrorMap.put(TimelineError.NO_MOTION_DURING_SLEEP, ErrorType.NO_MOTION_DURING_SLEEP);



        algorithmTypeMap = HashBiMap.create();
        algorithmTypeMap.put(AlgorithmType.NONE,AlgType.NO_ALGORITHM);
        algorithmTypeMap.put(AlgorithmType.WUPANG,AlgType.WUPANG);
        algorithmTypeMap.put(AlgorithmType.HMM,AlgType.HMM);
        algorithmTypeMap.put(AlgorithmType.ONLINE_HMM,AlgType.ONLINE_HMM);
        algorithmTypeMap.put(AlgorithmType.VOTING,AlgType.VOTING);
        algorithmTypeMap.put(AlgorithmType.NEURAL_NET,AlgType.NEURAL_NET);
        algorithmTypeMap.put(AlgorithmType.NEURAL_NET_FOUR_EVENT,AlgType.NEURAL_NET_FOUR_EVENT);


        eventTypeMap = HashBiMap.create();
        eventTypeMap.put(Event.Type.IN_BED,SleepEventType.IN_BED);
        eventTypeMap.put(Event.Type.SLEEP,SleepEventType.SLEEP);
        eventTypeMap.put(Event.Type.WAKE_UP,SleepEventType.WAKE);
        eventTypeMap.put(Event.Type.OUT_OF_BED,SleepEventType.OUT_OF_BED);




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

            long testGroup = DEFAULT_TEST_GROUP;
            if (timelineLog.hasTestGroup()) {
                testGroup = timelineLog.getTestGroup();
            }

            return Optional.of(new TimelineLog(log.toBuilder(),timelineLog.getAccountId(),timelineLog.getNightOfTimeline(), timelineLog.getTimestampWhenLogGenerated(),testGroup));

        }
        catch (InvalidProtocolBufferException e) {
            LOGGER.error(e.getMessage());
        }

        return Optional.absent();
    }

    public TimelineLog(final long accountId, final long dateOfNight, final long timestamp) {
        this(accountId, dateOfNight, timestamp, DEFAULT_TEST_GROUP);
    }

    public TimelineLog(final long accountId, final long dateOfNight, final long timestamp, final long testGroup) {
        this.dateOfNight = dateOfNight;
        this.dateOfNightString = DateTimeUtil.dateToYmdString(new DateTime().withZone(DateTimeZone.UTC).withMillis(dateOfNight));
        this.accountId = accountId;

        builder = LoggingProtos.BatchLogMessage.newBuilder();
        builder.setLogType(LoggingProtos.BatchLogMessage.LogType.TIMELINE_LOG);
        builder.setReceivedAt(timestamp);
        this.timestamp = timestamp;
        this.testGroup = testGroup;

    }

    public TimelineLog(final Long accountId, final long dateOfNight) {
        this(accountId, dateOfNight, DateTime.now(DateTimeZone.UTC).getMillis(),DEFAULT_TEST_GROUP);
    }

    private TimelineLog(final LoggingProtos.BatchLogMessage.Builder builder, final long accountId, final long dateOfNight, final long timestamp, final long testGroup) {
        this.builder = builder;
        this.accountId = accountId;
        this.dateOfNight = dateOfNight;
        this.timestamp = timestamp;
        this.dateOfNightString = DateTimeUtil.dateToYmdString(new DateTime().withZone(DateTimeZone.UTC).withMillis(dateOfNight));
        this.testGroup = testGroup;
    }

    @VisibleForTesting
    public LoggingProtos.BatchLogMessage build() {
        return builder.build();
    }

    public byte [] toProtoBuf() {
        return build().toByteArray();
    }

    public String toProtobufBase64() {
        return Base64.encodeBase64URLSafeString(toProtoBuf());
    }

    public String getPartitionKey() {
        return Long.valueOf(accountId).toString();
    }

    private static Optional<Prediction> predictionFromEvent(final Event event) {
        final SleepEventType eventType = eventTypeMap.get(event.getType());

        if (eventType == null) {
            return Optional.absent();
        }

        return Optional.of(Prediction.newBuilder()
                .setEventType(eventType)
                .setTimezoneOffsetMillis(event.getTimezoneOffset())
                .setEventTimeUtcMillis(event.getStartTimestamp()).build());

    }

    private Builder getTimelineLogBuilder() {
        return LoggingProtos.TimelineLog.newBuilder()
                .setAccountId(accountId)
                .setNightOfTimeline(dateOfNight)
                .setNightOf(dateOfNightString)
                .setTimestampWhenLogGenerated(timestamp)
                .setTestGroup(testGroup);
    }

    public void addMessage(final TimelineError timelineError) {
        final ErrorType errorType = invalidNightErrorMap.get(timelineError);

        if (errorType == null) {
            return;
        }

        builder.addTimelineLog(
                getTimelineLogBuilder()
                .setAlgorithm(AlgType.NO_ALGORITHM)
                .setError(errorType)
                .build());
    }

    public void addMessage(final TimelineError timelineError, final String message) {
        final ErrorType errorType = invalidNightErrorMap.get(timelineError);

        if (errorType == null) {
            return;
        }
        builder.addTimelineLog(
                getTimelineLogBuilder()
                        .setAlgorithm(AlgType.NO_ALGORITHM)
                        .setError(errorType)
                        .setMessage(message)
                        .build());
    }

    public void addMessage(final AlgorithmType algorithmType,final List<Event> predictions) {
        final AlgType algType = algorithmTypeMap.get(algorithmType);

        if (algType == null) {
            return;
        }

        final LoggingProtos.TimelineLog.Builder timelineLogBuilder = getTimelineLogBuilder()
                .setAlgorithm(algType)
                .setError(ErrorType.NO_ERROR);

        for (final Event event : predictions) {
            final Optional<Prediction> predictionOptional = predictionFromEvent(event);

            if (!predictionOptional.isPresent()) {
                continue;
            }

            timelineLogBuilder.addPredictions(predictionOptional.get());
        }

        builder.addTimelineLog(timelineLogBuilder.build());
    }

    public void addMessage(final AlgorithmType algorithmType, final TimelineError timelineError) {
        final AlgType algType = algorithmTypeMap.get(algorithmType);

        if (algType == null) {
            return;
        }

        final ErrorType error = invalidNightErrorMap.get(timelineError);

        if (error == null) {
            return;
        }

        builder.addTimelineLog(
                getTimelineLogBuilder()
                        .setAlgorithm(algType)
                        .setError(error)
                        .build());
    }

    public void addMessage(final AlgorithmType algorithmType, final TimelineError timelineError, final String message) {
        final AlgType algType = algorithmTypeMap.get(algorithmType);

        if (algType == null) {
            return;
        }

        final ErrorType error = invalidNightErrorMap.get(timelineError);

        if (error == null) {
            return;
        }

        builder.addTimelineLog(
                getTimelineLogBuilder()
                        .setAlgorithm(algType)
                        .setError(error)
                        .setMessage(message)
                        .build());
    }

    public void addMessage(final AlgorithmType algorithmType, final float score, final String model) {
        final AlgType algType = algorithmTypeMap.get(algorithmType);

        if (algType == null) {
            return;
        }

        builder.addTimelineLog(
                getTimelineLogBuilder()
                        .setAlgorithm(algType)
                        .setError(ErrorType.NO_ERROR)
                        .setModelName(model)
                        .setModelScore(score)
                        .build());
    }


    public com.hello.suripu.core.models.TimelineLog getAsV1Log() {
        final LoggingProtos.BatchLogMessage log = builder.build();

        String intendedAlgorithm = "none";
        String usedAlgorithm = "none";
        String version = "none";

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
