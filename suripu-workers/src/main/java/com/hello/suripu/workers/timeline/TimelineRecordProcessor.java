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
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.RingTimeDAODynamoDB;
import com.hello.suripu.core.db.TimelineDAODynamoDB;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
        final HashMap<String, Set<DateTime>> pillIdTargetDatesMap = new HashMap<>();

        for (final Record record : list) {
            try {
                final SenseCommandProtos.batched_pill_data data = SenseCommandProtos.batched_pill_data.parseFrom(record.getData().array());

                for(final SenseCommandProtos.pill_data pillData:data.getPillsList()) {
                    if (pillData.hasMotionDataEntrypted()) {
                        if (!pillIdTargetDatesMap.containsKey(pillData.getDeviceId())) {
                            pillIdTargetDatesMap.put(pillData.getDeviceId(), new HashSet<DateTime>());
                        }

                        final DateTime targetDateUTC = new DateTime(pillData.getTimestamp() * 1000L, DateTimeZone.UTC);
                        pillIdTargetDatesMap.get(pillData.getDeviceId()).add(targetDateUTC);
                    }
                }
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to decode protobuf: {}", e.getMessage());
            } catch (IllegalArgumentException e) {
                LOGGER.error("Failed to decrypted pill data {}, error: {}", record.getData().array(), e.getMessage());
            }
        }

        for(final String pillId:pillIdTargetDatesMap.keySet()){

            final List<DeviceAccountPair> accountsLinkedWithPill = this.deviceDAO.getLinkedAccountFromPillId(pillId);
            if(accountsLinkedWithPill.size() == 0){
                LOGGER.warn("No account linked with pill {}", pillId);
                continue;
            }

            if(accountsLinkedWithPill.size() > 1){
                LOGGER.warn("{} accounts linked with pill {}, only account {} get the timeline",
                        accountsLinkedWithPill.size(),
                        pillId,
                        accountsLinkedWithPill.get(accountsLinkedWithPill.size() - 1).accountId);
            }

            final long accountId = accountsLinkedWithPill.get(accountsLinkedWithPill.size() - 1).accountId;
            final List<DeviceAccountPair> sensesLinkedWithAccount = this.deviceDAO.getSensesForAccountId(accountId);
            if(sensesLinkedWithAccount.size() == 0){
                LOGGER.warn("No sense linked with account {} from pill {}", accountId, pillId);
                continue;
            }

            if(sensesLinkedWithAccount.size() > 1){
                LOGGER.warn("{} senses linked with account {}, only sense {} got the timeline.",
                        sensesLinkedWithAccount.size(),
                        accountId,
                        sensesLinkedWithAccount.get(sensesLinkedWithAccount.size() - 1).externalDeviceId);
            }

            final String senseId = sensesLinkedWithAccount.get(sensesLinkedWithAccount.size() - 1).externalDeviceId;
            final Optional<DateTimeZone> dateTimeZoneOptional = this.mergedUserInfoDynamoDB.getTimezone(senseId, accountId);

            if(!dateTimeZoneOptional.isPresent()){
                LOGGER.error("No timezone for sense {} account {}", senseId, accountId);
                continue;
            }

            final Set<DateTime> dates = pillIdTargetDatesMap.get(pillId);
            final HashSet<DateTime> targetDatesLocalUTC = new HashSet<>();

            for(final DateTime targetDateUTC:dates) {

                DateTime targetDateLocalUTC = targetDateUTC.plusMillis(
                        dateTimeZoneOptional.get().getOffset(targetDateUTC.getMillis()))
                        .withTimeAtStartOfDay();

                final DateTime targetDateLocalTime = targetDateUTC.withZone(dateTimeZoneOptional.get());
                if(targetDateLocalTime.getHourOfDay() < 20){
                    targetDateLocalUTC = targetDateLocalUTC.minusDays(1);
                }

                targetDatesLocalUTC.add(targetDateLocalUTC);

            }

            for(final DateTime targetDateLocalUTC:targetDatesLocalUTC){
                final List<Timeline> timelines = this.timelineProcessor.retrieveTimelines(accountId,
                        targetDateLocalUTC.toString(DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT)));

                try{
                    this.timelineDAODynamoDB.saveTimelinesForDate(accountId, targetDateLocalUTC, timelines);
                    LOGGER.info("Timeline at {} saved for account {}.", targetDateLocalUTC, accountId);
                }catch (AmazonServiceException aex){
                    LOGGER.error("AWS error, save timeline for account {} failed, error {}", accountId, aex.getMessage());
                }catch (Exception ex){
                    LOGGER.error("Save timeline for account {} failed, error {}", accountId, ex.getMessage());
                }
            }
        }

        try {
            iRecordProcessorCheckpointer.checkpoint();
        } catch (InvalidStateException e) {
            LOGGER.error("checkpoint {}", e.getMessage());
        } catch (ShutdownException e) {
            LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
        }
    }

    @Override
    public void shutdown(final IRecordProcessorCheckpointer iRecordProcessorCheckpointer, final ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
    }
}
