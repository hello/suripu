package com.hello.suripu.workers.timeline;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.RingTimeDAODynamoDB;
import com.hello.suripu.core.db.TimelineDAODynamoDB;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by pangwu on 1/26/15.
 */
public class TimelineRecordProcessor extends HelloBaseRecordProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(TimelineRecordProcessor.class);
    private final TimelineProcessor timelineProcessor;
    private final TimelineWorkerConfiguration configuration;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final RingTimeDAODynamoDB ringTimeDAODynamoDB;
    private final TimelineDAODynamoDB timelineDAODynamoDB;
    private final DeviceDAO deviceDAO;

    public TimelineRecordProcessor(final TimelineProcessor timelineProcessor,
                                   final DeviceDAO deviceDAO,
                                   final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                                   final RingTimeDAODynamoDB ringTimeDAODynamoDB,
                                   final TimelineDAODynamoDB timelineDAODynamoDB,
                                   final TimelineWorkerConfiguration configuration){

        this.timelineProcessor = timelineProcessor;
        this.configuration = configuration;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.ringTimeDAODynamoDB = ringTimeDAODynamoDB;
        this.timelineDAODynamoDB = timelineDAODynamoDB;
        this.deviceDAO = deviceDAO;

    }

    @Override
    public void initialize(String s) {
        LOGGER.info("Time line processor initialized: " + s);
    }

    @Override
    public void processRecords(final List<Record> list, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        final Map<String, Set<DateTime>> pillIdTargetDatesMap = BatchProcessUtils.groupRequestingPillIds(list);
        final Map<Long, Set<DateTime>> groupedAccountIdTargetDateLocalUTCMap = BatchProcessUtils.groupAccountAndProcessDateLocalUTC(pillIdTargetDatesMap,
                this.deviceDAO,
                this.mergedUserInfoDynamoDB);

        batchProcess(groupedAccountIdTargetDateLocalUTCMap);

        try {
            iRecordProcessorCheckpointer.checkpoint();
        } catch (InvalidStateException e) {
            LOGGER.error("checkpoint {}", e.getMessage());
        } catch (ShutdownException e) {
            LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
        }
    }

    private void batchProcess(final Map<Long, Set<DateTime>> groupedAccountIdTargetDateLocalUTCMap){
        final Map<Long, Integer> defaultValues = new HashMap<>();
        for(final Long accountId:groupedAccountIdTargetDateLocalUTCMap.keySet()) {
            defaultValues.put(accountId, missingDataDefaultValue(accountId));

        }
        this.timelineProcessor.batchProcessTimelines(groupedAccountIdTargetDateLocalUTCMap,
                defaultValues,
                1000,
                this.timelineDAODynamoDB);
    }


    @Override
    public void shutdown(final IRecordProcessorCheckpointer iRecordProcessorCheckpointer, final ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
    }
}
