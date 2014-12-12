package com.hello.suripu.workers.sense;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.base.Optional;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.DataInputProtos;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.MergedAlarmInfoDynamoDB;
import com.hello.suripu.core.models.AlarmInfo;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import com.hello.suripu.workers.utils.ActiveDevicesTracker;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.annotation.Timed;
import com.yammer.metrics.core.Meter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class SenseSaveProcessor extends HelloBaseRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(SenseSaveProcessor.class);

    public final static Integer CLOCK_SKEW_TOLERATED_IN_HOURS = 2;
    private final DeviceDAO deviceDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final MergedAlarmInfoDynamoDB mergedInfoDynamoDB;
    private final ActiveDevicesTracker activeDevicesTracker;

    private final Meter messagesProcessed;
    private final Meter batchSaved;

    public SenseSaveProcessor(final DeviceDAO deviceDAO, final MergedAlarmInfoDynamoDB mergedInfoDynamoDB, final DeviceDataDAO deviceDataDAO, final ActiveDevicesTracker activeDevicesTracker) {
        this.deviceDAO = deviceDAO;
        this.mergedInfoDynamoDB = mergedInfoDynamoDB;
        this.deviceDataDAO = deviceDataDAO;
        this.activeDevicesTracker = activeDevicesTracker;
        this.messagesProcessed = Metrics.defaultRegistry().newMeter(SenseSaveProcessor.class, "messages", "messages-processed", TimeUnit.SECONDS);
        this.batchSaved = Metrics.defaultRegistry().newMeter(SenseSaveProcessor.class, "batch", "batch-saved", TimeUnit.SECONDS);
    }

    @Override
    public void initialize(String s) {

    }

    @Timed
    @Override
    public void processRecords(List<Record> records, IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        final LinkedHashMap<String, LinkedList<DeviceData>> deviceDataGroupedByDeviceId = new LinkedHashMap<>();

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

            final String deviceName = batchPeriodicDataWorker.getData().getDeviceId();

            if(!deviceDataGroupedByDeviceId.containsKey(deviceName)){
                deviceDataGroupedByDeviceId.put(deviceName, new LinkedList<DeviceData>());
            }

            final LinkedList<DeviceData> dataForDevice = deviceDataGroupedByDeviceId.get(deviceName);


            final List<DeviceAccountPair> deviceAccountPairs = deviceDAO.getAccountIdsForDeviceId(deviceName);

            // We should not have too many accounts with more than two accounts paired to a sense
            // warn if it is the case
            if(deviceAccountPairs.size() > 2) {
                LOGGER.warn("Found too many pairs ({}) for device = {}", deviceAccountPairs.size(), deviceName);
            }

            final long timestampMillis = batchPeriodicDataWorker.getReceivedAt();
            final DateTime roundedDateTime = new DateTime(timestampMillis, DateTimeZone.UTC).withSecondOfMinute(0).withMillisOfSecond(0);
            if(roundedDateTime.isAfter(DateTime.now().plusHours(CLOCK_SKEW_TOLERATED_IN_HOURS)) || roundedDateTime.isBefore(DateTime.now().minusHours(CLOCK_SKEW_TOLERATED_IN_HOURS))) {
                LOGGER.error("The clock for device {} is not within reasonable bounds (2h)", batchPeriodicDataWorker.getData().getDeviceId());
                LOGGER.error("Current time = {}, received time = {}", DateTime.now(), roundedDateTime);
                continue;
            }


            // This is the default timezone.
            final List<AlarmInfo> deviceAccountInfoFromMergeTable = new ArrayList<>();

            try {
                deviceAccountInfoFromMergeTable.addAll(this.mergedInfoDynamoDB.getInfo(deviceName));  // get everything by one hit
            } catch (AmazonClientException exception) {
                LOGGER.error("Failed getting info from DynamoDB for device = {}", deviceName);
                continue;
            }


            for(final DataInputProtos.periodic_data periodicData : batchPeriodicDataWorker.getData().getDataList()) {

                for (final DeviceAccountPair pair : deviceAccountPairs) {
                    Optional<DateTimeZone> timeZoneOptional = Optional.absent();
                    for(final AlarmInfo alarmInfo:deviceAccountInfoFromMergeTable){
                        if(alarmInfo.accountId == pair.accountId){
                            if(alarmInfo.timeZone.isPresent()){
                                timeZoneOptional = alarmInfo.timeZone;
                            }else{
                                LOGGER.warn("No timezone for device {} account {}", deviceName, alarmInfo.accountId);
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

                    final DeviceData.Builder builder = new DeviceData.Builder()
                            .withAccountId(pair.accountId)
                            .withDeviceId(pair.internalDeviceId)
                            .withAmbientTemperature(periodicData.getTemperature())
                            .withAmbientAirQuality(periodicData.getDust(), periodicData.getFirmwareVersion())
                            .withAmbientAirQualityRaw(periodicData.getDust())
                            .withAmbientDustVariance(periodicData.getDustVariability())
                            .withAmbientDustMin(periodicData.getDustMin())
                            .withAmbientDustMax(periodicData.getDustMax())
                            .withAmbientHumidity(periodicData.getHumidity())
                            .withAmbientLight(periodicData.getLight())
                            .withAmbientLightVariance(periodicData.getLightVariability())
                            .withAmbientLightPeakiness(periodicData.getLightTonality())
                            .withOffsetMillis(userTimeZone.getOffset(roundedDateTime))
                            .withDateTimeUTC(roundedDateTime)
                            .withFirmwareVersion(periodicData.getFirmwareVersion())
                            .withWaveCount(periodicData.hasWaveCount() ? periodicData.getWaveCount() : 0)
                            .withHoldCount(periodicData.hasHoldCount() ? periodicData.getHoldCount() : 0);

                    final DeviceData deviceData = builder.build();
                    dataForDevice.add(deviceData);
                }
            }
            activeSenses.put(deviceName, batchPeriodicDataWorker.getReceivedAt());

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


        try {
            iRecordProcessorCheckpointer.checkpoint();
        } catch (InvalidStateException e) {
            LOGGER.error("checkpoint {}", e.getMessage());
        } catch (ShutdownException e) {
            LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
        }

        activeDevicesTracker.trackSenses(activeSenses);
    }

    @Override
    public void shutdown(IRecordProcessorCheckpointer iRecordProcessorCheckpointer, ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
    }
}
