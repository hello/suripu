package com.hello.suripu.workers.alarm;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.DataInputProtos;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.ScheduledRingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SmartAlarmLoggerDynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.processors.RingProcessor;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.Metrics;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by pangwu on 9/23/14.
 */
public class AlarmRecordProcessor extends HelloBaseRecordProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(AlarmRecordProcessor.class);
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final ScheduledRingTimeHistoryDAODynamoDB scheduledRingTimeHistoryDAODynamoDB;
    private final SmartAlarmLoggerDynamoDB smartAlarmLoggerDynamoDB;

    private final TrackerMotionDAO trackerMotionDAO;
    private final AlarmWorkerConfiguration configuration;

    private final Histogram recordAge;

    public AlarmRecordProcessor(final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                                final ScheduledRingTimeHistoryDAODynamoDB scheduledRingTimeHistoryDAODynamoDB,
                                final SmartAlarmLoggerDynamoDB smartAlarmLoggerDynamoDB,
                                final TrackerMotionDAO trackerMotionDAO,
                                final AlarmWorkerConfiguration configuration){

        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.scheduledRingTimeHistoryDAODynamoDB = scheduledRingTimeHistoryDAODynamoDB;
        this.trackerMotionDAO = trackerMotionDAO;
        this.smartAlarmLoggerDynamoDB = smartAlarmLoggerDynamoDB;

        this.configuration = configuration;

        // Create a histogram of the ages of records, biased towards newer values.
        this.recordAge = Metrics.defaultRegistry().newHistogram(AlarmRecordProcessor.class, "records", "record-age", true);
    }

    @Override
    public void initialize(String s) {
        LOGGER.info("AlarmRecordProcessor initialized: " + s);
    }

    private Boolean isRecordTooOld(long recordTimestamp) {
        long currentRecordAgeMillis = DateTime.now().getMillis() - recordTimestamp;
        recordAge.update(currentRecordAgeMillis);
        long maxRecordAgeMillis = configuration.getMaximumRecordAgeMinutes() * 60 * 1000;
        return currentRecordAgeMillis > maxRecordAgeMillis;
    }

    @Override
    public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {

        final Set<String> senseIds = new HashSet<String>();
        LOGGER.info("Got {} records.", records.size());
        for (final Record record : records) {
            try {
                final DataInputProtos.BatchPeriodicDataWorker pb = DataInputProtos.BatchPeriodicDataWorker.parseFrom(record.getData().array());

                if(!pb.getData().hasDeviceId() || pb.getData().getDeviceId().isEmpty()) {
                    LOGGER.warn("Found a periodic_data without a device_id {}");
                    continue;
                }

                final String senseId = pb.getData().getDeviceId();

                // If the record is too old, don't process it so that we can catch up to newer messages.
                if (isRecordTooOld(pb.getReceivedAt()) && hasAlarmWorkerDropIfTooOldEnabled(senseId)) continue;

                senseIds.add(senseId);


            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to decode protobuf: {}", e.getMessage());
            }
        }

        LOGGER.info("Processing {} unique senseIds.", senseIds.size());
        for(final String senseId : senseIds) {
            try {
                RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                        this.scheduledRingTimeHistoryDAODynamoDB,
                        this.smartAlarmLoggerDynamoDB,
                        this.trackerMotionDAO,
                        senseId,
                        DateTime.now(),
                        this.configuration.getProcessAheadTimeInMinutes(),
                        this.configuration.getAggregateWindowSizeInMinute(),
                        this.configuration.getLightSleepThreshold(),
                        flipper
                );
            }catch (Exception ex){
                // Currently catching all exceptions, which means that we could checkpoint after a failure.
                LOGGER.error("Update next ring time for sense {} failed at {}, error {}",
                        senseId,
                        DateTime.now(),
                        ex.getMessage());
            }
        }

        LOGGER.info("Successfully updated smart ring time for {} sense", senseIds.size());
        try {
            iRecordProcessorCheckpointer.checkpoint();
        } catch (InvalidStateException e) {
            LOGGER.error("checkpoint {}", e.getMessage());
        } catch (ShutdownException e) {
            LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
        }

        // Optimization in cases where we have very few new messages
        if(records.size() < 5) {
            LOGGER.info("Batch size was small. Sleeping for 10s");
            try {
                Thread.sleep(10000L);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted Thread while sleeping: {}", e.getMessage());
            }
        }
    }

    @Override
    public void shutdown(final IRecordProcessorCheckpointer iRecordProcessorCheckpointer, final ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
        if(shutdownReason == ShutdownReason.TERMINATE) {
            try {
                iRecordProcessorCheckpointer.checkpoint();
            } catch (InvalidStateException e) {
                LOGGER.error(e.getMessage());
            } catch (ShutdownException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }
}
