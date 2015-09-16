package com.hello.suripu.workers.logs.timeline;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class TimelineLogProcessor extends HelloBaseRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(TimelineLogProcessor.class);
    private final TimelineAnalytics timelineAnalytics;

    public TimelineLogProcessor(final TimelineAnalytics timelineAnalytics) {
        this.timelineAnalytics = timelineAnalytics;
    }

    public static TimelineLogProcessor create(final TimelineAnalytics timelineAnalytics) {
        return new TimelineLogProcessor(timelineAnalytics);
    }

    @Override
    public void initialize(String shardId) {

    }

    @Override
    public void processRecords(List<Record> records, IRecordProcessorCheckpointer checkpointer) {
        final List<LoggingProtos.TimelineLog> logs = Lists.newArrayList();
        final Set<LoggingProtos.TimelineLog> uniqueLogs = Sets.newHashSet();

        for(final Record record : records) {
            try {
                final LoggingProtos.BatchLogMessage batchLogMessage = LoggingProtos.BatchLogMessage.parseFrom(record.getData().array());
                if(!batchLogMessage.hasLogType() && batchLogMessage.getLogType().equals(LoggingProtos.BatchLogMessage.LogType.TIMELINE_LOG) && batchLogMessage.getTimelineLogCount() > 0) {
                    continue;
                }

                for(final LoggingProtos.TimelineLog timelineLog : batchLogMessage.getTimelineLogList()) {
                    if(uniqueLogs.contains(timelineLog)) {
                        continue;
                    }

                    logs.add(timelineLog);
                    uniqueLogs.add(timelineLog);
                }
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed converting protobuf: {}", e.getMessage());
            }
        }

        timelineAnalytics.insertBatchWithIndividualRetry(logs);
        // TODO: checkpoint here
    }

    @Override
    public void shutdown(IRecordProcessorCheckpointer checkpointer, ShutdownReason reason) {

    }
}
