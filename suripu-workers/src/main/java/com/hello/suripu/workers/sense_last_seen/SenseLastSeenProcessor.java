package com.hello.suripu.workers.sense_last_seen;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.DataInputProtos;
import com.hello.suripu.core.db.WifiInfoDAO;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.WifiInfo;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;


public class SenseLastSeenProcessor extends HelloBaseRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(SenseLastSeenProcessor.class);

    public final static Integer CLOCK_SKEW_TOLERATED_IN_HOURS = 2;
    private final static Integer MIN_UPTIME_IN_SECONDS_FOR_CACHING = 24 * 3600;
    private final static Integer WIFI_INFO_BATCH_MAX_SIZE = 25;

    private final Integer maxRecords;
    private final WifiInfoDAO wifiInfoDAO;

    private final Meter messagesProcessed;
    private final Meter batchSaved;
    private final Meter clockOutOfSync;
    private final Timer fetchTimezones;
    private final Meter capacity;

    private Map<String, WifiInfo> wifiInfoPerBatch = Maps.newHashMap();
    private Map<String, WifiInfo> wifiInfoHistory = Maps.newHashMap();


    private String shardId = "";
    private Random random;
    private LoadingCache<String, List<DeviceAccountPair>> dbCache;

    public SenseLastSeenProcessor(final Integer maxRecords, final WifiInfoDAO wifiInfoDAO) {
        this.maxRecords = maxRecords;
        this.wifiInfoDAO = wifiInfoDAO;

        this.messagesProcessed = Metrics.defaultRegistry().newMeter(SenseLastSeenProcessor.class, "messages", "messages-processed", TimeUnit.SECONDS);
        this.batchSaved = Metrics.defaultRegistry().newMeter(SenseLastSeenProcessor.class, "batch", "batch-saved", TimeUnit.SECONDS);
        this.clockOutOfSync = Metrics.defaultRegistry().newMeter(SenseLastSeenProcessor.class, "clock", "clock-out-of-sync", TimeUnit.SECONDS);
        this.fetchTimezones = Metrics.defaultRegistry().newTimer(SenseLastSeenProcessor.class, "fetch-timezones");
        this.capacity = Metrics.defaultRegistry().newMeter(SenseLastSeenProcessor.class, "capacity", "capacity", TimeUnit.SECONDS);

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

            //LOGGER.info("Protobuf message {}", TextFormat.shortDebugString(batchPeriodicDataWorker));
            collectWifiInfo(batchPeriodicDataWorker.getData());

        }

        trackWifiInfo(wifiInfoPerBatch);
        wifiInfoPerBatch.clear();


        messagesProcessed.mark(records.size());

        if(random.nextInt(11) % 10 == 0) {
            final CacheStats stats = dbCache.stats();
            LOGGER.info("{} - Cache hitrate: {}", this.shardId, stats.hitRate());
        }

        // This lets us clear the cache remotely by turning on the feature in FeatureFlipper.
        // DeviceId is not required and thus empty
        if(flipper.deviceFeatureActive(FeatureFlipper.WORKER_CLEAR_ALL_CACHE, "", Collections.EMPTY_LIST)) {
            LOGGER.warn("Clearing all caches");
            dbCache.invalidateAll();
        }

        try {
            iRecordProcessorCheckpointer.checkpoint();
        } catch (InvalidStateException e) {
            LOGGER.error("checkpoint {}", e.getMessage());
        } catch (ShutdownException e) {
            LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
        }


        final int batchCapacity = Math.round(activeSenses.size() / (float) maxRecords * 100.0f) ;
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
            if (connectedSSID.equals(scannedSSID)) {
                if (wifiInfoHistory.containsKey(senseId) && wifiInfoHistory.get(batchedPeriodicData.getDeviceId()).equals(connectedSSID)) {
                    LOGGER.warn("Skip writing because wifi ssid remains unchanged");
                    continue;
                }
                wifiInfoPerBatch.put(
                        batchedPeriodicData.getDeviceId(),
                        WifiInfo.create(senseId, connectedSSID, wifiAccessPoint.getRssi(), new DateTime(batchedPeriodicData.getData(0).getUnixTime() * 1000L, DateTimeZone.UTC))
                );
                wifiInfoHistory.put(
                        batchedPeriodicData.getDeviceId(),
                        WifiInfo.create(senseId, connectedSSID, wifiAccessPoint.getRssi(), new DateTime(batchedPeriodicData.getData(0).getUnixTime() * 1000L, DateTimeZone.UTC))
                );
            }
        }
    }

    public void trackWifiInfo(final Map<String, WifiInfo> wifiInfoPerBatch) {
        List<WifiInfo> wifiInfoList = Lists.newArrayList(wifiInfoPerBatch.values());
        Collections.shuffle(wifiInfoList);

        wifiInfoDAO.putBatch(wifiInfoList.subList(0, Math.min(WIFI_INFO_BATCH_MAX_SIZE, wifiInfoList.size())));
        LOGGER.debug("Tracked wifi info for {} senses {}", wifiInfoPerBatch.size(), wifiInfoPerBatch);
    }
}
