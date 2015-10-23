package com.hello.suripu.workers.sense.lastSeen;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.DataInputProtos;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.WifiInfoDAO;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.WifiInfo;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import com.hello.suripu.workers.sense.SenseProcessorUtils;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.annotation.Timed;
import com.yammer.metrics.core.Meter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class SenseLastSeenProcessor extends HelloBaseRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(SenseLastSeenProcessor.class);

    private final static Integer WIFI_INFO_BATCH_MAX_SIZE = 25;
    private final static Integer SIGNIFICANT_RSSI_CHANGE = 5;

    private final Integer maxRecords;
    private final WifiInfoDAO wifiInfoDAO;
    private final SensorsViewsDynamoDB sensorsViewsDynamoDB;

    private final Meter messagesProcessed;
    private final Meter capacity;

    private Map<String, WifiInfo> wifiInfoPerBatch = Maps.newHashMap();
    private Map<String, WifiInfo> wifiInfoHistory = Maps.newHashMap();

    private String shardId = "";

    public SenseLastSeenProcessor(final Integer maxRecords, final WifiInfoDAO wifiInfoDAO, final SensorsViewsDynamoDB sensorsViewsDynamoDB) {
        this.maxRecords = maxRecords;
        this.wifiInfoDAO = wifiInfoDAO;
        this.sensorsViewsDynamoDB = sensorsViewsDynamoDB;

        this.messagesProcessed = Metrics.defaultRegistry().newMeter(SenseLastSeenProcessor.class, "messages", "messages-processed", TimeUnit.SECONDS);
        this.capacity = Metrics.defaultRegistry().newMeter(SenseLastSeenProcessor.class, "capacity", "capacity", TimeUnit.SECONDS);
    }

    @Override
    public void initialize(String s) {
        shardId = s;
    }

    @Timed
    @Override
    public void processRecords(List<Record> records, IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        final Map<String, Long> activeSenses = new HashMap<>(records.size());
        final Map<String, DeviceData> lastSeenSenseDataMap = Maps.newHashMap();
        for(final Record record : records) {
            DataInputProtos.BatchPeriodicDataWorker batchPeriodicDataWorker;
            try {
                batchPeriodicDataWorker = DataInputProtos.BatchPeriodicDataWorker.parseFrom(record.getData().array());
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed parsing protobuf: {}", e.getMessage());
                LOGGER.error("Moving to next record");
                continue;
            }

            //LOGGER.info("Protobuf message {}", TextFormat.shortDebugString(batchPeriodicDataWorker));

            final DataInputProtos.batched_periodic_data batchPeriodicData = batchPeriodicDataWorker.getData();

            collectWifiInfo(batchPeriodicData);

            final String senseExternalId = batchPeriodicData.getDeviceId();
            final Optional<DeviceData> lastSeenSenseDataOptional = getSenseData(batchPeriodicDataWorker);
            if (lastSeenSenseDataOptional.isPresent()){
                lastSeenSenseDataMap.put(senseExternalId, lastSeenSenseDataOptional.get());
            }

            activeSenses.put(batchPeriodicData.getDeviceId(), batchPeriodicDataWorker.getReceivedAt());

        }

        trackWifiInfo(wifiInfoPerBatch);
        wifiInfoPerBatch.clear();

        sensorsViewsDynamoDB.saveLastSeenDeviceData(lastSeenSenseDataMap);
        LOGGER.info("Saved last seen for {} senses", lastSeenSenseDataMap.size());

        messagesProcessed.mark(records.size());


        try {
            iRecordProcessorCheckpointer.checkpoint();
        } catch (InvalidStateException e) {
            LOGGER.error("checkpoint {}", e.getMessage());
        } catch (ShutdownException e) {
            LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
        }


        final int batchCapacity = Math.round(activeSenses.size() / (float) maxRecords * 100.0f);
        LOGGER.info("{} - seen device: {}", shardId, activeSenses.size());
        LOGGER.info("{} - capacity: {}%", shardId, batchCapacity);
        capacity.mark(batchCapacity);
    }

    @Override
    public void shutdown(IRecordProcessorCheckpointer iRecordProcessorCheckpointer, ShutdownReason shutdownReason) {

        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
        if(shutdownReason == ShutdownReason.TERMINATE) {
            LOGGER.warn("Going to checkpoint");
            try {
                iRecordProcessorCheckpointer.checkpoint();
                LOGGER.warn("Checkpointed successfully");
            } catch (InvalidStateException e) {
                LOGGER.error(e.getMessage());
            } catch (ShutdownException e) {
                LOGGER.error(e.getMessage());
            }
        }

    }

    private void collectWifiInfo (final DataInputProtos.batched_periodic_data batchedPeriodicData) {
        final String connectedSSID = batchedPeriodicData.hasConnectedSsid() ? batchedPeriodicData.getConnectedSsid() : "unknown_ssid";
        final String senseId = batchedPeriodicData.getDeviceId();
        for (final DataInputProtos.batched_periodic_data.wifi_access_point wifiAccessPoint : batchedPeriodicData.getScanList()) {
            final String scannedSSID = wifiAccessPoint.getSsid();

            // Scans return all seen networks, we want to only grab info of the connected one
            if (!connectedSSID.equals(scannedSSID)) {
                continue;
            }

            // If we have persisted wifi info for a sense since the worker started, then consider skipping if ...
            if (wifiInfoHistory.containsKey(senseId) && wifiInfoHistory.get(senseId).ssid.equals(connectedSSID)) {

                // If the corresponding feature is not turned on, skip writing as we assume rssi won't change
                if (!hasPersistSignificantWifiRssiChangeEnabled(senseId)) {
                    LOGGER.trace("Skip writing because of {}'s unchanged network {}", senseId, connectedSSID);
                    continue;
                }

                // If the corresponding feature is turned on, skip writing unless rssi has changed significantly
                if (!hasSignificantRssiChange(wifiInfoHistory, senseId, wifiAccessPoint.getRssi())) {
                    LOGGER.trace("Skip writing because there is no significant wifi info change for {}'s network {}", senseId, connectedSSID);
                    continue;
                }
            }

            // Otherwise, persist new wifi info and memorize it in history for next iteration reference
            final WifiInfo wifiInfo = WifiInfo.create(senseId, connectedSSID, wifiAccessPoint.getRssi(), new DateTime(batchedPeriodicData.getData(0).getUnixTime() * 1000L, DateTimeZone.UTC));
            wifiInfoPerBatch.put(senseId, wifiInfo);
            wifiInfoHistory.put(senseId, wifiInfo);
        }
    }

    private Optional<DeviceData> getSenseData(final DataInputProtos.BatchPeriodicDataWorker batchPeriodicDataWorker) {
        final String senseExternalId = batchPeriodicDataWorker.getData().getDeviceId();
        final DataInputProtos.periodic_data periodicData = batchPeriodicDataWorker.getData().getDataList().get(batchPeriodicDataWorker.getData().getDataList().size() - 1);

        final long createdAtTimestamp = batchPeriodicDataWorker.getReceivedAt();
        final DateTime createdAtRounded = new DateTime(createdAtTimestamp, DateTimeZone.UTC);

        final DateTime sampleDateTime = SenseProcessorUtils.getSampleTime(
                createdAtRounded, periodicData, attemptToRecoverSenseReportedTimeStamp(senseExternalId)
        );
        final Integer firmwareVersion = SenseProcessorUtils.getFirmwareVersion(batchPeriodicDataWorker, periodicData);
        if (SenseProcessorUtils.isClockOutOfSync(sampleDateTime, createdAtRounded)) {
            LOGGER.error("Clock out of sync Created time = {}, sample time = {}, now = {}", createdAtRounded, sampleDateTime, DateTime.now());
            return Optional.absent();
        }
        return Optional.of(new DeviceData.Builder()
                .withAmbientTemperature(periodicData.getTemperature())
                .withAmbientHumidity(periodicData.getHumidity())
                .withAmbientLight(periodicData.getLight())
                .withAmbientAirQualityRaw(periodicData.getDust())
                .withAudioPeakDisturbancesDB(periodicData.hasAudioPeakDisturbanceEnergyDb() ? periodicData.getAudioPeakDisturbanceEnergyDb() : 0)
                .withFirmwareVersion(firmwareVersion)
                .withDateTimeUTC(sampleDateTime)
                .withAccountId(0L)   // Account ID is not needed for last seen data
                .withDeviceId(0L)    // Sense internal ID is not needed for last seen data
                .withOffsetMillis(0) // Timezone offset is not needed for last seen data
                .build());
    }


    public void trackWifiInfo(final Map<String, WifiInfo> wifiInfoPerBatch) {
        final List<WifiInfo> wifiInfoList = Lists.newArrayList(wifiInfoPerBatch.values());
        Collections.shuffle(wifiInfoList);
        if (wifiInfoList.isEmpty()) {
            return;
        }
        wifiInfoDAO.putBatch(wifiInfoList.subList(0, Math.min(WIFI_INFO_BATCH_MAX_SIZE, wifiInfoList.size())));
        LOGGER.info("Tracked wifi info for {} senses", wifiInfoPerBatch.size());
        LOGGER.trace("Tracked wifi info for senses {}", wifiInfoPerBatch.keySet());
    }

    @VisibleForTesting
    public static Boolean hasSignificantRssiChange(final Map<String, WifiInfo> wifiInfoHistory, final String senseId, final Integer rssi) {
        if (!wifiInfoHistory.containsKey(senseId)){
            return true;
        }
        return Math.abs(wifiInfoHistory.get(senseId).rssi - rssi) >= SIGNIFICANT_RSSI_CHANGE;
    }
}
