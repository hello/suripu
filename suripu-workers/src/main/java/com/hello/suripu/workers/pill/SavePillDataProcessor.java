package com.hello.suripu.workers.pill;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.PillViewsDynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import com.hello.suripu.workers.utils.ActiveDevicesTracker;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SavePillDataProcessor extends HelloBaseRecordProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(SavePillDataProcessor.class);

    private final TrackerMotionDAO trackerMotionDAO;
    private final int batchSize;
    private final PillHeartBeatDAO pillHeartBeatDAO;
    private final KeyStore pillKeyStore;
    private final DeviceDAO deviceDAO;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final ActiveDevicesTracker activeDevicesTracker;
    private final PillViewsDynamoDB pillViewsDynamoDB;


    private final Meter messagesProcessed;
    private final Meter batchSaved;

    public SavePillDataProcessor(final TrackerMotionDAO trackerMotionDAO,
                                 final int batchSize,
                                 final PillHeartBeatDAO pillHeartBeatDAO,
                                 final KeyStore pillKeyStore,
                                 final DeviceDAO deviceDAO,
                                 final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                                 final ActiveDevicesTracker activeDevicesTracker,
                                 final PillViewsDynamoDB pillViewsDynamoDB) {
        this.trackerMotionDAO = trackerMotionDAO;
        this.batchSize = batchSize;
        this.pillHeartBeatDAO = pillHeartBeatDAO;
        this.pillKeyStore = pillKeyStore;
        this.deviceDAO = deviceDAO;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.activeDevicesTracker = activeDevicesTracker;
        this.pillViewsDynamoDB = pillViewsDynamoDB;

        this.messagesProcessed = Metrics.defaultRegistry().newMeter(SavePillDataProcessor.class, "messages", "messages-processed", TimeUnit.SECONDS);
        this.batchSaved = Metrics.defaultRegistry().newMeter(SavePillDataProcessor.class, "batch", "batch-saved", TimeUnit.SECONDS);
    }

    @Override
    public void initialize(String s) {
    }

    @Override
    public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        LOGGER.debug("Size = {}", records.size());

        // parse kinesis records
        final ArrayList<TrackerMotion> trackerData = new ArrayList<>(records.size());
        final Map<String, Long> activePills = Maps.newHashMap();
        for (final Record record : records) {
            try {
                final SenseCommandProtos.batched_pill_data batched_pill_data = SenseCommandProtos.batched_pill_data.parseFrom(record.getData().array());
                for(final SenseCommandProtos.pill_data data : batched_pill_data.getPillsList()) {

                    final Long pillTs = data.getTimestamp() * 1000L;
                    activePills.put(data.getDeviceId(), pillTs);

                    final Optional<byte[]> decryptionKey = pillKeyStore.get(data.getDeviceId());
                    //TODO: Get the actual decryption key.
                    if(!decryptionKey.isPresent()) {
                        LOGGER.error("Missing decryption key for pill: {}", data.getDeviceId());
                        continue;
                    }

                    final Optional<DeviceAccountPair> optionalPair = deviceDAO.getInternalPillId(data.getDeviceId());
                    if(!optionalPair.isPresent()) {
                        LOGGER.error("Missing pairing in account tracker map for pill: {}", data.getDeviceId());
                        continue;
                    }

                    final DeviceAccountPair pair = optionalPair.get();
                    final Optional<UserInfo> userInfoOptional = mergedUserInfoDynamoDB.getInfo(batched_pill_data.getDeviceId(), pair.accountId);

                    if(!userInfoOptional.isPresent()) {
                        LOGGER.error("Missing UserInfo for account: {} and pill_id = {} and sense_id = {}", pair.accountId, pair.externalDeviceId, batched_pill_data.getDeviceId());
                        continue;
                    }

                    final UserInfo userInfo = userInfoOptional.get();
                    final Optional<DateTimeZone> timeZoneOptional = userInfo.timeZone;
                    if(!timeZoneOptional.isPresent()) {
                        LOGGER.error("No timezone for account {} with pill_id = {}", pair.accountId, pair.externalDeviceId);
                        continue;
                    }


                    if(data.hasMotionDataEntrypted()){
                        try {
                            final TrackerMotion trackerMotion = TrackerMotion.create(data, pair, timeZoneOptional.get(), decryptionKey.get());
                            trackerData.add(trackerMotion);
                            LOGGER.debug("Tracker Data added for batch insert for pill_id = {}", pair.externalDeviceId);
                        } catch (TrackerMotion.InvalidEncryptedPayloadException exception) {
                            LOGGER.error("Fail to decrypt tracker motion payload for pill {}, account {}", pair.externalDeviceId, pair.accountId);
                        }
                    }


                    if(data.hasBatteryLevel()){
                        final int batteryLevel = data.getBatteryLevel();
                        final int upTimeInSeconds = data.getUptime();
                        final int firmwareVersion = data.getFirmwareVersion();
                        final Long ts = data.getTimestamp() * 1000L;
                        final DateTime lastUpdated = new DateTime(ts, DateTimeZone.UTC);
                        LOGGER.info("Received heartbeat for pill_id {}, last_updated {}", pair.externalDeviceId, lastUpdated);
                        pillHeartBeatDAO.silentInsert(pair.internalDeviceId, batteryLevel, upTimeInSeconds, firmwareVersion, lastUpdated);
                        // Best effort saving of the last seen HB
                        pillViewsDynamoDB.update(data.getDeviceId(), upTimeInSeconds, firmwareVersion, batteryLevel, lastUpdated);
                    }
                }
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to decode protobuf: {}", e.getMessage());
            } catch (IllegalArgumentException e) {
                LOGGER.error("Failed to decrypted pill data {}, error: {}", record.getData().array(), e.getMessage());
            }
        }


        if (trackerData.size() > 0) {
            LOGGER.info("About to batch insert: {} tracker motion samples", trackerData.size());
            this.trackerMotionDAO.batchInsertTrackerMotionData(trackerData, this.batchSize);
            batchSaved.mark(trackerData.size());
            LOGGER.info("Finished batch insert: {} tracker motion samples", trackerData.size());
            try {
                iRecordProcessorCheckpointer.checkpoint();
                LOGGER.info("Successful checkpoint.");
            } catch (InvalidStateException e) {
                LOGGER.error("checkpoint {}", e.getMessage());
            } catch (ShutdownException e) {
                LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
            }
        }
        activeDevicesTracker.trackPills(activePills);
        messagesProcessed.mark(records.size());
    }

    @Override
    public void shutdown(final IRecordProcessorCheckpointer iRecordProcessorCheckpointer, final ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
        if(shutdownReason == ShutdownReason.TERMINATE) {
            LOGGER.warn("Got Termintate. Attempting to checkpoint.");
            try {
                iRecordProcessorCheckpointer.checkpoint();
                LOGGER.warn("Checkpoint successful.");
            } catch (InvalidStateException e) {
                LOGGER.error(e.getMessage());
            } catch (ShutdownException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }
}
