package com.hello.suripu.workers.timeline;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.base.Optional;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.core.db.AlgorithmResultsDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.TimelineDAODynamoDB;
import com.hello.suripu.core.models.TimelineResult;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by pangwu on 1/26/15.
 */
public class TimelineRecordProcessor extends HelloBaseRecordProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(TimelineRecordProcessor.class);
    private final TimelineProcessor timelineProcessor;
    private final TimelineWorkerConfiguration configuration;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final TimelineDAODynamoDB timelineDAODynamoDB;
    private final DeviceDAO deviceDAO;
    private final AlgorithmResultsDAODynamoDB algorithmResultsDAODynamoDB;


    private final Meter messagesProcessed;
    private final Meter timelinesSaved;
    private final Meter timelinesExpired;

    public TimelineRecordProcessor(final TimelineProcessor timelineProcessor,
                                   final DeviceDAO deviceDAO,
                                   final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                                   final TimelineDAODynamoDB timelineDAODynamoDB,
                                   final AlgorithmResultsDAODynamoDB algorithmResultsDAODynamoDB,
                                   final TimelineWorkerConfiguration configuration){

        this.timelineProcessor = timelineProcessor;
        this.configuration = configuration;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.timelineDAODynamoDB = timelineDAODynamoDB;
        this.deviceDAO = deviceDAO;
        this.algorithmResultsDAODynamoDB = algorithmResultsDAODynamoDB;

        this.messagesProcessed = Metrics.defaultRegistry().newMeter(TimelineRecordProcessor.class, "messages", "messages-processed", TimeUnit.SECONDS);
        this.timelinesSaved = Metrics.defaultRegistry().newMeter(TimelineRecordProcessor.class, "timelines-saved", "timelines-saved", TimeUnit.SECONDS);
        this.timelinesExpired = Metrics.defaultRegistry().newMeter(TimelineRecordProcessor.class, "timelines-expired", "timelines-expired", TimeUnit.SECONDS);
    }

    @Override
    public void initialize(String s) {
        LOGGER.info("Time line processor initialized: " + s);
    }

    @Override
    public void processRecords(final List<Record> list, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        messagesProcessed.mark(list.size());
        final List<SenseCommandProtos.batched_pill_data> batchedPillData = new ArrayList<>();
        for(final Record record:list){
            try {
                SenseCommandProtos.batched_pill_data dataBatch = SenseCommandProtos.batched_pill_data.parseFrom(record.getData().array());
                batchedPillData.add(dataBatch);
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to decode protobuf: {}", e.getMessage());
            }
        }

        final Map<String, Set<DateTime>> pillIdTargetDatesMapByHeartbeat = BatchProcessUtils.groupRequestingPillIdsByDataType(batchedPillData,
                BatchProcessUtils.DataTypeFilter.PILL_HEARTBEAT);
        final Map<String, Set<DateTime>> pillIdTargetDatesMapByData = BatchProcessUtils.groupRequestingPillIdsByDataType(batchedPillData,
                BatchProcessUtils.DataTypeFilter.PILL_DATA);

        final Map<Long, Set<DateTime>> groupedAccountIdAndRegenerateTimelineTargetDateLocalUTCMap = BatchProcessUtils.groupAccountAndProcessDateLocalUTC(pillIdTargetDatesMapByHeartbeat,
                this.configuration.getEarliestProcessTime(),
                this.configuration.getLastProcessTime(),
                DateTime.now(),
                this.deviceDAO,
                this.mergedUserInfoDynamoDB);

        final Map<Long, Set<DateTime>> groupedAccountIdAndExpiredTargetDateLocalUTCMap = BatchProcessUtils.groupAccountAndExpireDateLocalUTC(pillIdTargetDatesMapByData,
                this.deviceDAO,
                this.mergedUserInfoDynamoDB);

        // Expires all out dated timeline
        expires(groupedAccountIdAndExpiredTargetDateLocalUTCMap, DateTime.now());

        // Reprocess
        batchProcess(groupedAccountIdAndRegenerateTimelineTargetDateLocalUTCMap);

        try {
            iRecordProcessorCheckpointer.checkpoint();
        } catch (InvalidStateException e) {
            LOGGER.error("checkpoint {}", e.getMessage());
        } catch (ShutdownException e) {
            LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
            System.exit(1);
        }
    }

    private void batchProcess(final Map<Long, Set<DateTime>> groupedAccountIdTargetDateLocalUTCMap){
        for(final Long accountId:groupedAccountIdTargetDateLocalUTCMap.keySet()) {
            for(final DateTime targetDateLocalUTC:groupedAccountIdTargetDateLocalUTCMap.get(accountId)) {
                if (this.timelineProcessor.shouldProcessTimelineByWorker(accountId,
                        this.configuration.getMaxNoMoitonPeriodInMinutes(),
                        DateTime.now())) {
                    continue;
                }

                try {
                    final Optional<TimelineResult> result = this.timelineProcessor.retrieveTimelinesFast(accountId, targetDateLocalUTC);
                    if(!result.isPresent()){
                        continue;
                    }

                    this.timelineDAODynamoDB.saveTimelinesForDate(accountId, targetDateLocalUTC, result.get());
                    timelinesSaved.mark(1);

                    LOGGER.info("{} Timeline saved for account {} at local utc {}",
                            DateTime.now(),
                            accountId,
                            DateTimeUtil.dateToYmdString(targetDateLocalUTC));

                    // TODO: Push notification here?
                } catch (AmazonServiceException awsException) {
                    LOGGER.error("Failed to generate timeline: {}", awsException.getErrorMessage());
                } catch (Exception ex) {
                    LOGGER.error("Failed to generate timeline. General error {}", ex.getMessage());
                }
            }
        }

    }

    private void expires(final Map<Long, Set<DateTime>> groupedAccountIdTargetDateLocalUTCMap, final DateTime expiresAtUTC){
        for(final Long accountId:groupedAccountIdTargetDateLocalUTCMap.keySet()) {
            for(final DateTime targetDate:groupedAccountIdTargetDateLocalUTCMap.get(accountId)) {
                try {
                    final boolean expired = this.timelineDAODynamoDB.invalidateCache(accountId,
                            targetDate,
                            expiresAtUTC);
                    LOGGER.info("Timeline expired {} for account {} at local utc {}",
                            expired,
                            accountId,
                            DateTimeUtil.dateToYmdString(targetDate));
                    timelinesExpired.mark(1);
                } catch (AmazonServiceException awsException) {
                    LOGGER.error("Failed to expire timeline: {}", awsException.getErrorMessage());
                } catch (Exception ex) {
                    LOGGER.error("Failed to expire timeline. General error {}", ex.getMessage());
                }
            }
        }
    }


    @Override
    public void shutdown(final IRecordProcessorCheckpointer iRecordProcessorCheckpointer, final ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
        System.exit(1);
    }
}
