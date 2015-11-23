package com.hello.suripu.workers.sense;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.DataInputProtos;
import com.hello.suripu.core.db.DeviceDataIngestDAO;
import com.hello.suripu.core.db.DeviceReadDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.annotation.Timed;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

// WARNING ALL CHANGES HAVE TO BE REPLICATED TO SenseSaveDDBProcessor
// TODO Burn this worker to the ground once everything has switched to DynamoDB
public class SenseSaveProcessor extends HelloBaseRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(SenseSaveProcessor.class);

    public final static Integer CLOCK_SKEW_TOLERATED_IN_HOURS = 2;
    private final static Integer MIN_UPTIME_IN_SECONDS_FOR_CACHING = 24 * 3600;
    private final DeviceReadDAO deviceDAO;
    private final DeviceDataIngestDAO deviceDataDAO;
    private final MergedUserInfoDynamoDB mergedInfoDynamoDB;
    private final SensorsViewsDynamoDB sensorsViewsDynamoDB;
    private final Integer maxRecords;
    private final boolean updateLastSeen;

    private final Meter messagesProcessed;
    private final Meter batchSaved;
    private final Meter batchSaveFailures;
    private final Meter clockOutOfSync;
    private final Timer fetchTimezones;
    private final Meter capacity;


    private String shardId = "";
    private final Random random;
    private final LoadingCache<String, List<DeviceAccountPair>> dbCache;

    public SenseSaveProcessor(final DeviceReadDAO deviceDAO, final MergedUserInfoDynamoDB mergedInfoDynamoDB, final DeviceDataIngestDAO deviceDataDAO, final SensorsViewsDynamoDB sensorsViewsDynamoDB, final Integer maxRecords, final boolean updateLastSeen) {
        this.deviceDAO = deviceDAO;
        this.mergedInfoDynamoDB = mergedInfoDynamoDB;
        this.deviceDataDAO = deviceDataDAO;
        this.sensorsViewsDynamoDB =  sensorsViewsDynamoDB;
        this.maxRecords = maxRecords;
        this.updateLastSeen = updateLastSeen;

        this.messagesProcessed = Metrics.defaultRegistry().newMeter(deviceDataDAO.name(), "messages", "messages-processed", TimeUnit.SECONDS);
        this.batchSaved = Metrics.defaultRegistry().newMeter(deviceDataDAO.name(), "batch", "batch-saved", TimeUnit.SECONDS);
        this.batchSaveFailures = Metrics.defaultRegistry().newMeter(deviceDataDAO.name(), "batch-failure", "batch-save-failure", TimeUnit.SECONDS);
        this.clockOutOfSync = Metrics.defaultRegistry().newMeter(deviceDataDAO.name(), "clock", "clock-out-of-sync", TimeUnit.SECONDS);
        this.fetchTimezones = Metrics.defaultRegistry().newTimer(deviceDataDAO.name(), "fetch-timezones");
        this.capacity = Metrics.defaultRegistry().newMeter(deviceDataDAO.name(), "capacity", "capacity", TimeUnit.SECONDS);


        random = new Random(System.currentTimeMillis());
        final int jitter = random.nextInt(4);
        LOGGER.info("Jitter: {}", jitter);
        this.dbCache  = CacheBuilder.newBuilder()
                .maximumSize(20000)
                .expireAfterWrite(4 + jitter, TimeUnit.MINUTES)
                .recordStats()
                .build(
                        new CacheLoader<String, List<DeviceAccountPair>>() {
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
        final LinkedList<DeviceData> deviceDataList = new LinkedList<>();

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

            final String deviceName = batchPeriodicDataWorker.getData().getDeviceId();

            final List<DeviceAccountPair> deviceAccountPairs = Lists.newArrayList();

            if(!flipper.deviceFeatureActive(FeatureFlipper.WORKER_PG_CACHE, deviceName, Collections.EMPTY_LIST) || batchPeriodicDataWorker.getUptimeInSecond() < MIN_UPTIME_IN_SECONDS_FOR_CACHING) {
                deviceAccountPairs.addAll(deviceDAO.getAccountIdsForDeviceId(deviceName));
            } else {
                deviceAccountPairs.addAll(dbCache.getUnchecked(deviceName));
            }


            // We should not have too many accounts with more than two accounts paired to a sense
            // warn if it is the case
            if(deviceAccountPairs.size() > 2) {
                LOGGER.warn("Found too many pairs ({}) for device = {}", deviceAccountPairs.size(), deviceName);
            }

            // Compare Postgres views with DynamoDB views.
            final List<Long> accounts = Lists.newArrayList();
            for (final DeviceAccountPair deviceAccountPair : deviceAccountPairs) {
                accounts.add(deviceAccountPair.accountId);
            }

            final Map<Long, DateTimeZone> timezonesByUser = SenseProcessorUtils.getTimezonesByUser(
                    deviceName, batchPeriodicDataWorker, accounts, mergedInfoDynamoDB, hasKinesisTimezonesEnabled(deviceName));

            if(timezonesByUser.isEmpty()) {
                LOGGER.warn("Device {} is not stored in DynamoDB or doesn't have any accounts linked.", deviceName);
            } else { // track only for sense paired to accounts
                activeSenses.put(deviceName, batchPeriodicDataWorker.getReceivedAt());
            }


            //LOGGER.info("Protobuf message {}", TextFormat.shortDebugString(batchPeriodicDataWorker));

            for(final DataInputProtos.periodic_data periodicData : batchPeriodicDataWorker.getData().getDataList()) {

                final long createdAtTimestamp = batchPeriodicDataWorker.getReceivedAt();
                final DateTime createdAtRounded = new DateTime(createdAtTimestamp, DateTimeZone.UTC);

                final DateTime periodicDataSampleDateTime = SenseProcessorUtils.getSampleTime(createdAtRounded, periodicData, attemptToRecoverSenseReportedTimeStamp(deviceName));

                if(SenseProcessorUtils.isClockOutOfSync(periodicDataSampleDateTime, createdAtRounded)) {
                    LOGGER.error("The clock for device {} is not within reasonable bounds (2h)", batchPeriodicDataWorker.getData().getDeviceId());
                    LOGGER.error("Created time = {}, sample time = {}, now = {}", createdAtRounded, periodicDataSampleDateTime, DateTime.now());
                    clockOutOfSync.mark();
                    continue;
                }

                final Integer firmwareVersion = SenseProcessorUtils.getFirmwareVersion(batchPeriodicDataWorker, periodicData);

                for (final DeviceAccountPair pair : deviceAccountPairs) {
                    if(!timezonesByUser.containsKey(pair.accountId)) {
                        LOGGER.warn("No timezone info for account {} paired with device {}, account may already unpaired with device but merge table not updated.",
                                pair.accountId,
                                deviceName);
                        continue;
                    }

                    final DateTimeZone userTimeZone = timezonesByUser.get(pair.accountId);


                    final DeviceData.Builder builder = SenseProcessorUtils.periodicDataToDeviceDataBuilder(periodicData)
                            .withAccountId(pair.accountId)
                            .withDeviceId(pair.internalDeviceId)
                            .withExternalDeviceId(pair.externalDeviceId)
                            .withOffsetMillis(userTimeZone.getOffset(periodicDataSampleDateTime))
                            .withDateTimeUTC(periodicDataSampleDateTime)
                            .withFirmwareVersion(firmwareVersion);

                    final DeviceData deviceData = builder.build();

                    if(hasLastSeenViewDynamoDBEnabled(deviceName)) {
                        lastSeenDeviceData.put(deviceName, deviceData);
                    }

                    deviceDataList.add(deviceData);
                }
            }
        }


        try {
            int inserted = deviceDataDAO.batchInsertAll(deviceDataList);

            if(inserted == deviceDataList.size()) {
                LOGGER.trace("Batch saved {} data to DB", inserted);
            }else{
                LOGGER.warn("Batch save failed, save {} data using itemize insert.", inserted);
            }

            batchSaved.mark(inserted);
            batchSaveFailures.mark(deviceDataList.size() - inserted);
        } catch (Exception exception) {
            LOGGER.error("Error saving data from {} to {}, {} data discarded",
                    deviceDataList.getFirst().dateTimeUTC,
                    deviceDataList.getLast().dateTimeUTC,  // I love linkedlist
                    deviceDataList.size());
        }

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

        if(!lastSeenDeviceData.isEmpty() && this.updateLastSeen) {
            sensorsViewsDynamoDB.saveLastSeenDeviceData(lastSeenDeviceData);
        }

        final int batchCapacity = Math.round(records.size() / (float) maxRecords * 100.0f) ;
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

}
