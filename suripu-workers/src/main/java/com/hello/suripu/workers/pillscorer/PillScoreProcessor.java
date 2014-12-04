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
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.PillSample;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.processors.PillScoreBatchByRecordsProcessor;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Meter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PillScoreProcessor extends HelloBaseRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(PillScoreProcessor.class);

    private final PillScoreBatchByRecordsProcessor pillProcessor;
    private final Counter messageCounter;
    private final Meter messageMeter;
    private final Meter checkpointMeter;
    private final KeyStore keyStore;
    private final DeviceDAO deviceDAO;
    private final TimeZoneHistoryDAODynamoDB timeZoneHistoryDB;

    private int decodeErrors = 0; // mutable

    public PillScoreProcessor(final SleepScoreDAO sleepScoreDAO, final int dateMinuteBucket, final int checkpointThreshold, final KeyStore keyStore, final DeviceDAO deviceDAO, final TimeZoneHistoryDAODynamoDB timeZoneHistoryDB) {
        this.pillProcessor = new PillScoreBatchByRecordsProcessor(sleepScoreDAO, dateMinuteBucket, checkpointThreshold);
        this.messageCounter = Metrics.defaultRegistry().newCounter(PillScoreProcessor.class, "message_count");
        this.messageMeter = Metrics.defaultRegistry().newMeter(PillScoreProcessor.class, "get-requests", "requests", TimeUnit.SECONDS);
        this.checkpointMeter = Metrics.defaultRegistry().newMeter(PillScoreProcessor.class, "checkpoint_rate", "checkpoints", TimeUnit.SECONDS);
        this.keyStore = keyStore;
        this.deviceDAO = deviceDAO;
        this.timeZoneHistoryDB = timeZoneHistoryDB;
    }

    @Override
    public void initialize(String s) {
    }

    @Override
    public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        LOGGER.debug("Size = {}", records.size());

        // parse kinesis records
        final ListMultimap<Long, PillSample> samples = ArrayListMultimap.create();
        for (final Record record : records) {
            SenseCommandProtos.batched_pill_data batchPilldata;

            try {
                batchPilldata = SenseCommandProtos.batched_pill_data.parseFrom(record.getData().array());
            } catch (InvalidProtocolBufferException exception) {
                final String errorMessage = String.format("Failed parsing protobuf: %s", exception.getMessage());
                LOGGER.error(errorMessage);
                continue;
            }


            for(SenseCommandProtos.pill_data pillData : batchPilldata.getPillsList()) {
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

                final long timestampMillis = pillData.getTimestamp() * 1000L;
                final DateTime roundedDateTime = new DateTime(timestampMillis, DateTimeZone.UTC)
                        .withSecondOfMinute(0)
                        .withMillisOfSecond(0);

                if (roundedDateTime.isAfter(DateTime.now().plusDays(2))) {
                    LOGGER.warn("Pill timestamp is in the future {}", internalPillPairingMap.get().internalDeviceId);
                    continue;
                }

                final TrackerMotion.Builder trackerMotionBuilder = new TrackerMotion.Builder();
                trackerMotionBuilder.withAccountId(internalPillPairingMap.get().accountId);
                trackerMotionBuilder.withTrackerId(internalPillPairingMap.get().internalDeviceId);
                trackerMotionBuilder.withOffsetMillis(getTimezoneForUser(internalPillPairingMap.get().accountId).getOffset(roundedDateTime));
                trackerMotionBuilder.withEncryptedValue(decryptionKey, pillData);

                final TrackerMotion trackerMotion = trackerMotionBuilder.build();

                final Long accountID = trackerMotion.accountId;
                final String pillID = trackerMotion.trackerId.toString();

                final PillSample sample = new PillSample(pillID, roundedDateTime, trackerMotion.value, trackerMotion.offsetMillis);
                samples.put(accountID, sample);
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
    }

    @Override
    public void shutdown(final IRecordProcessorCheckpointer iRecordProcessorCheckpointer, final ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
    }

    // Should this be public for easy testing?
    private DateTimeZone getTimezoneForUser(Long accountId) {
        final DateTimeZone defaultTimezone = DateTimeZone.forID("America/Los_Angeles");
        final Optional<TimeZoneHistory> optional = timeZoneHistoryDB.getCurrentTimeZone(accountId);
        if(optional.isPresent()) {
            return DateTimeZone.forID(optional.get().timeZoneId);
        }
        LOGGER.warn("Account {} does not have a timezone set", accountId);
        return defaultTimezone;
    }
}