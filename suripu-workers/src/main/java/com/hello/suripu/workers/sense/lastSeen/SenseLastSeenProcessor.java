package com.hello.suripu.workers.sense.lastSeen;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.DataInputProtos;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.WifiInfoDAO;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.WifiInfo;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import com.hello.suripu.workers.sense.SenseSaveProcessor;
import com.hello.suripu.workers.sense.SenseSaveUtils;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.annotation.Timed;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
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

    public final static Integer CLOCK_SKEW_TOLERATED_IN_HOURS = 2;
    private final static Integer MIN_UPTIME_IN_SECONDS_FOR_CACHING = 24 * 3600;
    private final static Integer WIFI_INFO_BATCH_MAX_SIZE = 25;

    private final Integer maxRecords;
    private final WifiInfoDAO wifiInfoDAO;
    private final SensorsViewsDynamoDB sensorsViewsDynamoDB;
    private final DeviceDAO deviceDAO;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;

    private final Meter messagesProcessed;
    private final Meter capacity;
    private final Timer fetchTimezones;

    private Map<String, WifiInfo> wifiInfoPerBatch = Maps.newHashMap();
    private Map<String, String> wifiInfoHistory = Maps.newHashMap();
    private Map<String, DeviceData> deviceDataPerBatch = Maps.newHashMap();


    private String shardId = "";
    private LoadingCache<String, List<DeviceAccountPair>> dbCache;

    public SenseLastSeenProcessor(final Integer maxRecords, final WifiInfoDAO wifiInfoDAO, final SensorsViewsDynamoDB sensorsViewsDynamoDB, final DeviceDAO deviceDAO, final MergedUserInfoDynamoDB mergedUserInfoDynamoDB) {
        this.maxRecords = maxRecords;
        this.wifiInfoDAO = wifiInfoDAO;
        this.sensorsViewsDynamoDB = sensorsViewsDynamoDB;
        this.deviceDAO = deviceDAO;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;

        this.messagesProcessed = Metrics.defaultRegistry().newMeter(SenseLastSeenProcessor.class, "messages", "messages-processed", TimeUnit.SECONDS);
        this.capacity = Metrics.defaultRegistry().newMeter(SenseLastSeenProcessor.class, "capacity", "capacity", TimeUnit.SECONDS);
        this.fetchTimezones = Metrics.defaultRegistry().newTimer(SenseSaveProcessor.class, "fetch-timezones");
        this.dbCache  = CacheBuilder.newBuilder()
            .maximumSize(20000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build(new CacheLoader<String, List<DeviceAccountPair>>() {
                @Override
                public List<DeviceAccountPair> load(final String senseId) {
                    return deviceDAO.getAccountIdsForDeviceId(senseId);
                }
            });
    }

    @Override
    public void initialize(String s) {
        shardId = s;
    }

    @Timed
    @Override
    public void processRecords(List<Record> records, IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        final Map<String, Long> activeSenses = new HashMap<>(records.size());

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
            collectDeviceData(batchPeriodicDataWorker);
            activeSenses.put(batchPeriodicData.getDeviceId(), batchPeriodicDataWorker.getReceivedAt());

        }

        trackWifiInfo(wifiInfoPerBatch);
        trackDeviceData(deviceDataPerBatch);
        wifiInfoPerBatch.clear();
        deviceDataPerBatch.clear();


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
            if (!connectedSSID.equals(scannedSSID)) {
                continue;
            }
            if (wifiInfoHistory.containsKey(senseId) && wifiInfoHistory.get(senseId).equals(connectedSSID)) {
                LOGGER.trace("Skip writing because wifi ssid remains unchanged for {} : {}", senseId, connectedSSID);
                continue;
            }
            wifiInfoPerBatch.put(
                    senseId,
                    WifiInfo.create(senseId, connectedSSID, wifiAccessPoint.getRssi(), new DateTime(batchedPeriodicData.getData(0).getUnixTime() * 1000L, DateTimeZone.UTC))
            );
            wifiInfoHistory.put(senseId, connectedSSID);
        }
    }

    private void collectDeviceData(final DataInputProtos.BatchPeriodicDataWorker batchPeriodicDataWorker) {
        final List<DeviceAccountPair> senseAccountPairs = Lists.newArrayList();
        final List<Long> accountIds = Lists.newArrayList();
        final String senseExternalId = batchPeriodicDataWorker.getData().getDeviceId();
        for (final DeviceAccountPair senseAccountPair : senseAccountPairs) {
            accountIds.add(senseAccountPair.accountId);
        }
        final Map<Long, DateTimeZone> timezonesByUser = SenseSaveUtils.getTimezonesByUser(senseExternalId, batchPeriodicDataWorker, accountIds, fetchTimezones, mergedUserInfoDynamoDB);

        if(timezonesByUser.isEmpty()) {
            LOGGER.warn("Sense {} is not stored in DynamoDB or doesn't have any accounts linked.", senseExternalId);
        }

        final long createdAtTimestamp = batchPeriodicDataWorker.getReceivedAt();
        final DateTime createdAtRounded = new DateTime(createdAtTimestamp, DateTimeZone.UTC);

        final Integer firmwareVersion = batchPeriodicDataWorker.getData().hasFirmwareVersion() ? batchPeriodicDataWorker.getData().getFirmwareVersion() : batchPeriodicDataWorker.getData().getFirmwareVersion();
        if (batchPeriodicDataWorker.getUptimeInSecond() < MIN_UPTIME_IN_SECONDS_FOR_CACHING) {
            senseAccountPairs.addAll(deviceDAO.getAccountIdsForDeviceId(batchPeriodicDataWorker.getData().getDeviceId()));
        }
        else {
            senseAccountPairs.addAll(dbCache.getUnchecked(batchPeriodicDataWorker.getData().getDeviceId()));
        }


        for(final DataInputProtos.periodic_data periodicData : batchPeriodicDataWorker.getData().getDataList()) {
            final Long timestampMillis = periodicData.getUnixTime() * 1000L;
            final DateTime rawDateTime = new DateTime(timestampMillis, DateTimeZone.UTC).withSecondOfMinute(0).withMillisOfSecond(0);
            final DateTime periodicDataSampleDateTime = attemptToRecoverSenseReportedTimeStamp(senseExternalId)
                    ? DateTimeUtil.possiblySanitizeSampleTime(createdAtRounded, rawDateTime, CLOCK_SKEW_TOLERATED_IN_HOURS)
                    : rawDateTime;

            for (final DeviceAccountPair senseAccountPair : senseAccountPairs) {
                if(!timezonesByUser.containsKey(senseAccountPair.accountId)) {
                    LOGGER.warn("No timezone info for account {} paired with device {}, account may already unpaired with device but merge table not updated.",
                            senseAccountPair.accountId,
                            senseExternalId);
                    continue;
                }

                final DateTimeZone userTimeZone = timezonesByUser.get(senseAccountPair.accountId);

                final DeviceData deviceData = new DeviceData.Builder()
                        .withAccountId(senseAccountPair.accountId)
                        .withDeviceId(senseAccountPair.internalDeviceId)
                        .withAmbientTemperature(periodicData.getTemperature())
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
                        .withAudioPeakBackgroundDB(periodicData.hasAudioPeakBackgroundEnergyDb() ? periodicData.getAudioPeakBackgroundEnergyDb() : 0)
                        .build();
                deviceDataPerBatch.put(senseExternalId, deviceData);
            }
        }
    }


    public void trackWifiInfo(final Map<String, WifiInfo> wifiInfoPerBatch) {
        final List<WifiInfo> wifiInfoList = Lists.newArrayList(wifiInfoPerBatch.values());
        Collections.shuffle(wifiInfoList);
        if (wifiInfoList.isEmpty()) {
            return;
        }
        wifiInfoDAO.putBatch(wifiInfoList.subList(0, Math.min(WIFI_INFO_BATCH_MAX_SIZE, wifiInfoList.size())));
        LOGGER.debug("Tracked wifi info for {} senses {}", wifiInfoPerBatch.size(), wifiInfoPerBatch);
    }


    private void trackDeviceData(Map<String, DeviceData> deviceDataPerBatch) {
        sensorsViewsDynamoDB.saveLastSeenDeviceData(deviceDataPerBatch);
    }

}
