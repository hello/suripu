package com.hello.suripu.workers.sense;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.DataInputProtos;
import com.hello.suripu.core.db.CalibrationDynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.FirmwareInfo;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.annotation.Timed;
import com.yammer.metrics.core.Meter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;


public class SenseSaveProcessor extends HelloBaseRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(SenseSaveProcessor.class);

    public final static Integer CLOCK_SKEW_TOLERATED_IN_HOURS = 2;
    private final DeviceDAO deviceDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final MergedUserInfoDynamoDB mergedInfoDynamoDB;
    private final SensorsViewsDynamoDB sensorsViewsDynamoDB;
    private final CalibrationDynamoDB calibrationDynamoDB;

    private final Meter messagesProcessed;
    private final Meter batchSaved;
    private final Meter clockOutOfSync;

    private String shardId = "";
    private Random random;
    private LoadingCache<String, List<DeviceAccountPair>> dbCache;

    public SenseSaveProcessor(final DeviceDAO deviceDAO, final MergedUserInfoDynamoDB mergedInfoDynamoDB, final DeviceDataDAO deviceDataDAO, final SensorsViewsDynamoDB sensorsViewsDynamoDB, final CalibrationDynamoDB calibrationDynamoDB) {
        this.deviceDAO = deviceDAO;
        this.mergedInfoDynamoDB = mergedInfoDynamoDB;
        this.deviceDataDAO = deviceDataDAO;
        this.sensorsViewsDynamoDB =  sensorsViewsDynamoDB;
        this.calibrationDynamoDB = calibrationDynamoDB;

        this.messagesProcessed = Metrics.defaultRegistry().newMeter(SenseSaveProcessor.class, "messages", "messages-processed", TimeUnit.SECONDS);
        this.batchSaved = Metrics.defaultRegistry().newMeter(SenseSaveProcessor.class, "batch", "batch-saved", TimeUnit.SECONDS);
        this.clockOutOfSync = Metrics.defaultRegistry().newMeter(SenseSaveProcessor.class, "clock", "clock-out-of-sync", TimeUnit.SECONDS);

        this.dbCache  = CacheBuilder.newBuilder()
                .maximumSize(20000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(
                        new CacheLoader<String, List<DeviceAccountPair>>() {
                            @Override
                            public List<DeviceAccountPair> load(final String senseId) {
                                return deviceDAO.getAccountIdsForDeviceId(senseId);
                            }
                        });
        random = new Random(System.currentTimeMillis());
    }

    @Override
    public void initialize(String s) {
        shardId = s;
    }

    @Timed
    @Override
    public void processRecords(List<Record> records, IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        final LinkedHashMap<String, LinkedList<DeviceData>> deviceDataGroupedByDeviceId = new LinkedHashMap<>();

        final Map<String, Long> activeSenses = new HashMap<>(records.size());
        final Map<String, FirmwareInfo> seenFirmwares = new HashMap<>(records.size());
        final Map<String, Long> allSeenSenses = new HashMap<>(records.size());

        final Map<String, DeviceData> lastSeenDeviceData = Maps.newHashMap();

        for(final Record record : records) {
            DataInputProtos.BatchPeriodicDataWorker batchPeriodicDataWorker;
            try {
                batchPeriodicDataWorker = DataInputProtos.BatchPeriodicDataWorker.parseFrom(record.getData().array());
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed parsing protobuf: {}", e.getMessage());
                LOGGER.error("Moving to next record");
                continue;
            }

            final String deviceName = batchPeriodicDataWorker.getData().getDeviceId();

            //Logging seen device before attempting account pairing
            allSeenSenses.put(deviceName, batchPeriodicDataWorker.getReceivedAt());

            if(!deviceDataGroupedByDeviceId.containsKey(deviceName)){
                deviceDataGroupedByDeviceId.put(deviceName, new LinkedList<DeviceData>());
            }

            final LinkedList<DeviceData> dataForDevice = deviceDataGroupedByDeviceId.get(deviceName);


            final List<DeviceAccountPair> deviceAccountPairs = Lists.newArrayList();

            if(flipper.deviceFeatureActive(FeatureFlipper.WORKER_PG_CACHE, deviceName, Collections.EMPTY_LIST)) {
                deviceAccountPairs.addAll(dbCache.getUnchecked(deviceName));
            } else {
                deviceAccountPairs.addAll(deviceDAO.getAccountIdsForDeviceId(deviceName));
            }


            // We should not have too many accounts with more than two accounts paired to a sense
            // warn if it is the case
            if(deviceAccountPairs.size() > 2) {
                LOGGER.warn("Found too many pairs ({}) for device = {}", deviceAccountPairs.size(), deviceName);
            }

            // This is the default timezone.
            final List<UserInfo> deviceAccountInfoFromMergeTable = new ArrayList<>();
            int retries = 2;
            for(int i = 0; i < retries; i++) {
                try {
                    deviceAccountInfoFromMergeTable.addAll(this.mergedInfoDynamoDB.getInfo(deviceName));  // get everything by one hit
                    break;
                } catch (AmazonClientException exception) {
                    LOGGER.error("Failed getting info from DynamoDB for device = {}", deviceName);
                }

                try {
                    LOGGER.warn("Sleeping for 1 sec");
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    LOGGER.warn("Thread sleep interrupted");
                }
                retries++;
            }

            if(deviceAccountInfoFromMergeTable.isEmpty()) {
                LOGGER.warn("Device {} is not stored in DynamoDB or doesn't have any accounts linked.", deviceName);
            } else { // track only for sense paired to accounts
                activeSenses.put(deviceName, batchPeriodicDataWorker.getReceivedAt());
            }

            //LOGGER.info("Protobuf message {}", TextFormat.shortDebugString(batchPeriodicDataWorker));

            final Map<String, Calibration> senseCalibrationMap = calibrationDynamoDB.getBatch(deviceDataGroupedByDeviceId.keySet());

            for(final DataInputProtos.periodic_data periodicData : batchPeriodicDataWorker.getData().getDataList()) {

                // To validate that the firmware is sending a correct unix timestamp
                // we need to compare it to something immutable, coming from a different clock (server)
                // We can't compare to now because now changes, and if we want to reprocess old data it will be immediately discarded
                final long createdAtTimestamp = batchPeriodicDataWorker.getReceivedAt();
                final DateTime createdAtRounded = new DateTime(createdAtTimestamp, DateTimeZone.UTC);
                final Long timestampMillis = periodicData.getUnixTime() * 1000L;
                final DateTime periodicDataSampleDateTime = new DateTime(timestampMillis, DateTimeZone.UTC).withSecondOfMinute(0).withMillisOfSecond(0);

                if(periodicDataSampleDateTime.isAfter(createdAtRounded.plusHours(CLOCK_SKEW_TOLERATED_IN_HOURS)) || periodicDataSampleDateTime.isBefore(createdAtRounded.minusHours(CLOCK_SKEW_TOLERATED_IN_HOURS))) {
                    LOGGER.error("The clock for device {} is not within reasonable bounds (2h)", batchPeriodicDataWorker.getData().getDeviceId());
                    LOGGER.error("Created time = {}, sample time = {}, now = {}", createdAtRounded, periodicDataSampleDateTime, DateTime.now());
                    clockOutOfSync.mark();
                    continue;
                }

                // Grab FW version from Batch or periodic data for EVT units
                final Integer firmwareVersion = (batchPeriodicDataWorker.getData().hasFirmwareVersion())
                        ? batchPeriodicDataWorker.getData().getFirmwareVersion()
                        : periodicData.getFirmwareVersion();

                for (final DeviceAccountPair pair : deviceAccountPairs) {
                    Optional<DateTimeZone> timeZoneOptional = Optional.absent();
                    for(final UserInfo userInfo :deviceAccountInfoFromMergeTable){
                        if(userInfo.accountId == pair.accountId){
                            if(userInfo.timeZone.isPresent()){
                                timeZoneOptional = userInfo.timeZone;
                            }else{
                                LOGGER.warn("No timezone for device {} account {}", deviceName, userInfo.accountId);
                                continue;
                            }
                        }
                    }


                    if(!timeZoneOptional.isPresent()){
                        LOGGER.warn("No timezone info for account {} paired with device {}, account may already unpaired with device but merge table not updated.",
                                pair.accountId,
                                deviceName);
                        continue;
                    }

                    final DateTimeZone userTimeZone = timeZoneOptional.get();


                    final Calibration calibration = senseCalibrationMap.get(pair.externalDeviceId);

                    final DeviceData.Builder builder = new DeviceData.Builder()
                            .withAccountId(pair.accountId)
                            .withDeviceId(pair.internalDeviceId)
                            .withAmbientTemperature(periodicData.getTemperature())
                            .withAmbientAirQuality(periodicData.getDust())
                            .withAmbientAirQualityRaw(periodicData.getDust())
                            .withAmbientDustVariance(periodicData.getDustVariability())
                            .withAmbientDustMin(periodicData.getDustMin())
                            .withAmbientDustMax(periodicData.getDustMax())
                            .withAmbientHumidity(periodicData.getHumidity())
                            .withAmbientLight(periodicData.getLight())
                            .withAmbientLightVariance(periodicData.getLightVariability())
                            .withAmbientLightPeakiness(periodicData.getLightTonality())
                            .withOffsetMillis(userTimeZone.getOffset(periodicDataSampleDateTime))
                            .withDateTimeUTC(periodicDataSampleDateTime)
                            .withFirmwareVersion(firmwareVersion)
                            .withWaveCount(periodicData.hasWaveCount() ? periodicData.getWaveCount() : 0)
                            .withHoldCount(periodicData.hasHoldCount() ? periodicData.getHoldCount() : 0)
                            .withAudioNumDisturbances(periodicData.hasAudioNumDisturbances() ? periodicData.getAudioNumDisturbances() : 0)
                            .withAudioPeakDisturbancesDB(periodicData.hasAudioPeakDisturbanceEnergyDb() ? periodicData.getAudioPeakDisturbanceEnergyDb() : 0)
                            .withAudioPeakBackgroundDB(periodicData.hasAudioPeakBackgroundEnergyDb() ? periodicData.getAudioPeakBackgroundEnergyDb() : 0);

                    final DeviceData deviceData = builder.build();

                    if(hasLastSeenViewDynamoDBEnabled(deviceName)) {
                        lastSeenDeviceData.put(deviceName, deviceData);
                    }

                    dataForDevice.add(deviceData);
                }
                //TODO: Eventually break out metrics to their own worker
                seenFirmwares.put(deviceName, new FirmwareInfo(firmwareVersion.toString(), deviceName, timestampMillis));
            }
        }

        for(final String deviceId: deviceDataGroupedByDeviceId.keySet()){
            final LinkedList<DeviceData> data = deviceDataGroupedByDeviceId.get(deviceId);
            if(data.size() == 0){
                continue;
            }

            try {
                int inserted = deviceDataDAO.batchInsertWithFailureFallback(data);

                if(inserted == data.size()) {
                    LOGGER.info("Batch saved {} data to DB for device {}", data.size(), deviceId);
                }else{
                    LOGGER.warn("Batch save failed, save {} data for device {} using itemize insert.", inserted, deviceId);
                }

                batchSaved.mark(inserted);
            } catch (Exception exception) {
                LOGGER.error("Error saving data for device {} from {} to {}, {} data discarded",
                        deviceId,
                        data.getFirst().dateTimeUTC,
                        data.getLast().dateTimeUTC,  // I love linkedlist
                        data.size());
            }
        }

        messagesProcessed.mark(records.size());

        if(random.nextInt() % 10 == 0) {
            final CacheStats stats = dbCache.stats();
            LOGGER.info("Cache hitrate: {}", stats.hitRate());
        }

        // This lets us clear the cache remotely by turning on the feature in FeatureFlipper.
        // DeviceId is not required and thus empty
        if(flipper.deviceFeatureActive(FeatureFlipper.WORKER_CLEAR_ALL_CACHE, "", Collections.EMPTY_LIST)) {
            dbCache.invalidateAll();
        }

        try {
            iRecordProcessorCheckpointer.checkpoint();
        } catch (InvalidStateException e) {
            LOGGER.error("checkpoint {}", e.getMessage());
        } catch (ShutdownException e) {
            LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
        }

        if(!lastSeenDeviceData.isEmpty()) {
            sensorsViewsDynamoDB.saveLastSeenDeviceData(lastSeenDeviceData);
        }

        // Commenting this out, it is causing production failures
        /*
        activeDevicesTracker.trackAllSeenSenses(allSeenSenses);
        activeDevicesTracker.trackSenses(activeSenses);
        activeDevicesTracker.trackFirmwares(seenFirmwares);
        */

        LOGGER.info("{} - seen device: {}", shardId, activeSenses.size());
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
}
