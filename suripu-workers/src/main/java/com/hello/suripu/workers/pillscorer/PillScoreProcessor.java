package com.hello.suripu.workers.pillscorer;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.PillSample;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.processors.PillScoreBatchByRecordsProcessor;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import com.hello.suripu.workers.utils.ActiveDevicesTracker;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Meter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PillScoreProcessor extends HelloBaseRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(PillScoreProcessor.class);

    private final PillScoreBatchByRecordsProcessor pillProcessor;
    private final Counter messageCounter;
    private final Meter messageMeter;
    private final Meter checkpointMeter;
    private final KeyStore keyStore;
    private final DeviceDAO deviceDAO;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final ActiveDevicesTracker activeDevicesTracker;

    private int decodeErrors = 0; // mutable

    public PillScoreProcessor(final SleepScoreDAO sleepScoreDAO, final int dateMinuteBucket, final int checkpointThreshold, final KeyStore keyStore, final DeviceDAO deviceDAO, final MergedUserInfoDynamoDB mergedUserInfoDynamoDB, final ActiveDevicesTracker activeDevicesTracker) {
        this.pillProcessor = new PillScoreBatchByRecordsProcessor(sleepScoreDAO, dateMinuteBucket, checkpointThreshold);
        this.messageCounter = Metrics.defaultRegistry().newCounter(PillScoreProcessor.class, "message_count");
        this.messageMeter = Metrics.defaultRegistry().newMeter(PillScoreProcessor.class, "get-requests", "requests", TimeUnit.SECONDS);
        this.checkpointMeter = Metrics.defaultRegistry().newMeter(PillScoreProcessor.class, "checkpoint_rate", "checkpoints", TimeUnit.SECONDS);
        this.keyStore = keyStore;
        this.deviceDAO = deviceDAO;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.activeDevicesTracker = activeDevicesTracker;
    }

    @Override
    public void initialize(String s) {
    }

    @Override
    public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        LOGGER.debug("Size = {}", records.size());

        // parse kinesis records
        final ListMultimap<Long, PillSample> samples = ArrayListMultimap.create();
        final Map<String, Long> activePills = new HashMap<>(records.size());
        for (final Record record : records) {
            SenseCommandProtos.batched_pill_data batchPilldata;

            try {
                batchPilldata = SenseCommandProtos.batched_pill_data.parseFrom(record.getData().array());
            } catch (InvalidProtocolBufferException exception) {
                final String errorMessage = String.format("Failed parsing protobuf: %s", exception.getMessage());
                LOGGER.error(errorMessage);
                continue;
            }

            for(final SenseCommandProtos.pill_data pillData : batchPilldata.getPillsList()) {
                final long timestampMillis = pillData.getTimestamp() * 1000L;
                final DateTime roundedDateTime = new DateTime(timestampMillis, DateTimeZone.UTC)
                        .withSecondOfMinute(0)
                        .withMillisOfSecond(0);

                if(pillData.hasMotionDataEntrypted()){

                    final Optional<byte[]> optionalKeyBytes = keyStore.get(pillData.getDeviceId());

                    if (!optionalKeyBytes.isPresent()) {
                        LOGGER.warn("Pill {} is not using the data stored in the keystore", pillData.getDeviceId());
                        continue;
                    }

                    final byte[] decryptionKey = optionalKeyBytes.get();

                    final Optional<DeviceAccountPair> internalPillPairingMap = this.deviceDAO.getInternalPillId(pillData.getDeviceId());

                    if(!internalPillPairingMap.isPresent()){
                        LOGGER.warn("Cannot find internal pill id for pill {}", pillData.getDeviceId());
                        continue;
                    }



                    if (roundedDateTime.isAfter(DateTime.now().plusDays(2))) {
                        LOGGER.warn("Pill timestamp is in the future {}", internalPillPairingMap.get().internalDeviceId);
                        continue;
                    }

                    final Optional<DateTimeZone> dateTimeZoneOptional = getTimezoneForUser(batchPilldata.getDeviceId(), internalPillPairingMap.get().accountId);
                    if(!dateTimeZoneOptional.isPresent()) {
                        LOGGER.error("Missing timezone for account id: {}  and sense id : {}", internalPillPairingMap.get().accountId, batchPilldata.getDeviceId());
                        continue;
                    }

                    try {
                        final TrackerMotion trackerMotion = TrackerMotion.create(pillData, internalPillPairingMap.get(), dateTimeZoneOptional.get(), decryptionKey);
                        final Long accountID = trackerMotion.accountId;
                        final String pillID = trackerMotion.trackerId.toString();

                        final PillSample sample = new PillSample(pillID, roundedDateTime, trackerMotion.value, trackerMotion.offsetMillis);
                        LOGGER.debug("adding for account {}, pill_id {}, date {}", accountID, pillID, roundedDateTime);
                        samples.put(accountID, sample);

                    } catch (TrackerMotion.InvalidEncryptedPayloadException exception) {
                        LOGGER.error("Fail to decrypt trackerMotion for pill {}, account {}", pillData.getDeviceId(), internalPillPairingMap.get().accountId);
                    }

                }

                activePills.put(pillData.getDeviceId(), roundedDateTime.getMillis());
            }

            this.messageCounter.inc();
            this.messageMeter.mark();
        }

        if (samples.size() > 0) {
            final boolean okayToCheckpoint = this.pillProcessor.processPillRecords(samples);

            if (okayToCheckpoint) {
                LOGGER.debug("going to checkpoint {}", this.pillProcessor.getNumPillRecordsProcessed());
                this.checkpointMeter.mark();

                try {
                    iRecordProcessorCheckpointer.checkpoint();
                } catch (InvalidStateException e) {
                    LOGGER.error("checkpoint {}", e.getMessage());
                } catch (ShutdownException e) {
                    LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
                }
            }

        }

            activeDevicesTracker.trackPills(activePills);
    }

    @Override
    public void shutdown(final IRecordProcessorCheckpointer iRecordProcessorCheckpointer, final ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
    }

    // Should this be public for easy testing?
    private Optional<DateTimeZone> getTimezoneForUser(final String senseId, final Long accountId) {
        final DateTimeZone defaultTimezone = DateTimeZone.forID("America/Los_Angeles");
        final Optional<UserInfo> userInfoOptional = mergedUserInfoDynamoDB.getInfo(senseId, accountId);
        if(userInfoOptional.isPresent()) {
            return userInfoOptional.get().timeZone;
        }

        return Optional.absent();
    }
}