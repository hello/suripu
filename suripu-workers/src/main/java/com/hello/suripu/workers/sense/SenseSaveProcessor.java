package com.hello.suripu.workers.sense;

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
import com.hello.suripu.core.util.DeviceIdUtil;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class SenseSaveProcessor extends HelloBaseRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(SenseSaveProcessor.class);

    private final DeviceDAO deviceDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final MergedAlarmInfoDynamoDB mergedInfoDynamoDB;

    public SenseSaveProcessor(final DeviceDAO deviceDAO, final MergedAlarmInfoDynamoDB mergedInfoDynamoDB, final DeviceDataDAO deviceDataDAO) {
        this.deviceDAO = deviceDAO;
        this.mergedInfoDynamoDB = mergedInfoDynamoDB;
        this.deviceDataDAO = deviceDataDAO;
    }

    @Override
    public void initialize(String s) {

    }

    @Override
    public void processRecords(List<Record> records, IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        final LinkedHashMap<String, LinkedList<DeviceData>> deviceDataGroupedByDeviceId = new LinkedHashMap<>();

        for(final Record record : records) {
            DataInputProtos.periodic_data periodicData;
            try {
                periodicData = DataInputProtos.periodic_data.parseFrom(record.getData().array());
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed parsing protobuf: {}", e.getMessage());
                LOGGER.error("Moving to next record");
                continue;
            }


            final Optional<String> deviceIdOptional = DeviceIdUtil.getMorpheusId(periodicData);
            if(!deviceIdOptional.isPresent()){
                LOGGER.error("Cannot get morpheus id. Skipping");
                continue;
            }

            final String deviceName = deviceIdOptional.get();

            if(!deviceDataGroupedByDeviceId.containsKey(deviceName)){
                deviceDataGroupedByDeviceId.put(deviceName, new LinkedList<DeviceData>());
            }

            final LinkedList<DeviceData> dataForDevice = deviceDataGroupedByDeviceId.get(deviceName);

            final List<DeviceAccountPair> deviceAccountPairs = deviceDAO.getAccountIdsForDeviceId(deviceName);

            // We should not have too many accounts with more than two accounts paired to a sense
            // warn if it is the case
            if(deviceAccountPairs.size() > 2) {
                LOGGER.warn("Found {} pairs for device = {}", deviceAccountPairs.size(), deviceName);
            }

            long timestampMillis = periodicData.getUnixTime() * 1000L;
            final DateTime roundedDateTime = new DateTime(timestampMillis, DateTimeZone.UTC).withSecondOfMinute(0).withMillisOfSecond(0);
            // This is the default timezone.
            final List<AlarmInfo> deviceAccountInfoFromMergeTable = this.mergedInfoDynamoDB.getInfo(deviceName);  // get everything by one hit

            for (final DeviceAccountPair pair : deviceAccountPairs) {
                Optional<DateTimeZone> timeZoneOptional = Optional.absent();
                for(final AlarmInfo alarmInfo:deviceAccountInfoFromMergeTable){
                    if(alarmInfo.accountId == pair.accountId){
                        if(alarmInfo.timeZone.isPresent()){
                            timeZoneOptional = alarmInfo.timeZone;
                        }else{
                            LOGGER.warn("No timezone for device {} account {}", deviceName, alarmInfo.accountId);
                        }
                    }
                }

                /*
                // This will kill the provision
                try {
                    // TODO: Get the timezone for current user.
                    timeZoneOptional = mergedInfoDynamoDB.getTimezone(pair.externalDeviceId, pair.accountId);
                    if(!timeZoneOptional.isPresent()) {
                        LOGGER.warn("Did not find Timezone for Sense {} and account: {}", pair.externalDeviceId, pair.internalDeviceId);
                        continue;
                    }
                } catch (AmazonServiceException awsException) {
                    // I guess this endpoint should never bail out?
                    LOGGER.error("AWS error when retrieving user timezone for account {} and Sense", pair.accountId, pair.externalDeviceId);
                    continue;
                }
                */

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

                /*try {
                    deviceDataDAO.insert(deviceData);
                    LOGGER.trace("Data saved to DB: {}", TextFormat.shortDebugString(periodicData));
                } catch (UnableToExecuteStatementException exception) {
                    final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(exception.getMessage());
                    if (!matcher.find()) {
                        LOGGER.error("Unknown error saving to DB: {}", exception.getMessage());
                    }

                    LOGGER.warn("Duplicate device sensor value for account_id = {}, time: {}", pair.accountId, roundedDateTime);
                }*/
            }
        }

        for(final String deviceId: deviceDataGroupedByDeviceId.keySet()){
            final LinkedList<DeviceData> data = deviceDataGroupedByDeviceId.get(deviceId);
            if(data.size() == 0){
                continue;
            }

            try {
                deviceDataDAO.batchInsert(data.iterator());
                LOGGER.info("{} Data saved to DB for device {}", data.size(), deviceId);
            } catch (UnableToExecuteStatementException exception) {
                LOGGER.error("Error sensor value for device {} from {} to {}, {} data discarded",
                        deviceId,
                        data.getFirst().dateTimeUTC,
                        data.getLast().dateTimeUTC,  // I love linkedlist
                        data.size());
            }
        }

        try {
            iRecordProcessorCheckpointer.checkpoint();
        } catch (InvalidStateException e) {
            LOGGER.error("checkpoint {}", e.getMessage());
        } catch (ShutdownException e) {
            LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
        }

    }

    @Override
    public void shutdown(IRecordProcessorCheckpointer iRecordProcessorCheckpointer, ShutdownReason shutdownReason) {

    }
}
