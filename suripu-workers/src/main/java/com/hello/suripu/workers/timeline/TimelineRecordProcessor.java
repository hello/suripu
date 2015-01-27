package com.hello.suripu.workers.timeline;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.base.Optional;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.RingTimeDAODynamoDB;
import com.hello.suripu.core.db.TimelineDAODynamoDB;
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
public class TimeLineRecordProcessor extends HelloBaseRecordProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(TimeLineRecordProcessor.class);
    private final TimelineProcessor timelineProcessor;
    private final TimeLineWorkerConfiguration configuration;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final RingTimeDAODynamoDB ringTimeDAODynamoDB;
    private final TimelineDAODynamoDB timelineDAODynamoDB;
    private final KeyStore pillKeyStore;

    public TimeLineRecordProcessor(final TimelineProcessor timelineProcessor,
                                   final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                                   final RingTimeDAODynamoDB ringTimeDAODynamoDB,
                                   final KeyStore pillKeyStore,
                                   final TimelineDAODynamoDB timelineDAODynamoDB,
                                   final TimeLineWorkerConfiguration configuration){

        this.timelineProcessor = timelineProcessor;
        this.configuration = configuration;
        this.pillKeyStore = pillKeyStore;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.ringTimeDAODynamoDB = ringTimeDAODynamoDB;
        this.timelineDAODynamoDB = timelineDAODynamoDB;

    }

    @Override
    public void initialize(String s) {
        LOGGER.info("Time line processor initialized: " + s);
    }

    @Override
    public void processRecords(final List<Record> list, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        final HashMap<Long, Set<DateTime>> accountTargetDatesMap = new HashMap<>();

        for (final Record record : list) {
            try {
                final InputProtos.PillDataKinesis data = InputProtos.PillDataKinesis.parseFrom(record.getData().array());
                final Optional<byte[]> decryptionKey = pillKeyStore.get(data.getPillId());
                //TODO: Get the actual decryption key.
                if(!decryptionKey.isPresent()) {
                    LOGGER.error("Missing decryption key for pill: {}", data.getPillId());
                    continue;
                }

                if(data.hasEncryptedData()){
                    if(!accountTargetDatesMap.containsKey(data.getAccountIdLong())){
                        accountTargetDatesMap.put(data.getAccountIdLong(), new HashSet<DateTime>());
                    }

                    final DateTime targetDateLocalUTC = new DateTime(data.getTimestamp(), DateTimeZone.UTC).plusMillis(data.getOffsetMillis()).withTimeAtStartOfDay();
                    accountTargetDatesMap.get(data.getAccountIdLong()).add(targetDateLocalUTC);
                }

            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to decode protobuf: {}", e.getMessage());
            } catch (IllegalArgumentException e) {
                LOGGER.error("Failed to decrypted pill data {}, error: {}", record.getData().array(), e.getMessage());
            }
        }

        for(final Long accountId:accountTargetDatesMap.keySet()){
            for(final DateTime targetDateLocalUTC:accountTargetDatesMap.get(accountId)) {
                final List<Timeline> timelines = this.timelineProcessor.retrieveTimelines(accountId,
                        targetDateLocalUTC.toString(DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT)));

                try{
                    this.timelineDAODynamoDB.setTimelinesForDate(accountId, targetDateLocalUTC, timelines);
                    LOGGER.info("Timeline at {} saved for account {}.", targetDateLocalUTC, accountId);
                }catch (AmazonServiceException aex){
                    LOGGER.error("AWS error, save timeline for account {} failed, error {}", accountId, aex.getMessage());
                }catch (Exception ex){
                    LOGGER.error("Save timeline for account {} failed, error {}", accountId, ex.getMessage());
                }
            }
        }
    }

    @Override
    public void shutdown(final IRecordProcessorCheckpointer iRecordProcessorCheckpointer, final ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
    }
}
