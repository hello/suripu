package com.hello.suripu.workers.sense;


import com.amazonaws.AmazonClientException;
import com.google.common.collect.Maps;
import com.hello.suripu.api.input.DataInputProtos;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class SenseProcessorUtils {
    private final static Logger LOGGER = LoggerFactory.getLogger(SenseProcessorUtils.class);

    public final static Integer CLOCK_SKEW_TOLERATED_IN_HOURS = 2;

    public static DateTime getSampleTime(final DateTime createdAtRounded, final DataInputProtos.periodic_data periodicData, final Boolean attemptToRecoverSenseReportedTimeStamp) {
        // To validate that the firmware is sending a correct unix timestamp
        // we need to compare it to something immutable, coming from a different clock (server)
        // We can't compare to now because now changes, and if we want to reprocess old data it will be immediately discarded

        final Long timestampMillis = periodicData.getUnixTime() * 1000L;
        final DateTime rawDateTime = new DateTime(timestampMillis, DateTimeZone.UTC).withSecondOfMinute(0).withMillisOfSecond(0);

        // This is intended to check for very specific clock bugs from Sense
        final DateTime periodicDataSampleDateTime = attemptToRecoverSenseReportedTimeStamp
                ? DateTimeUtil.possiblySanitizeSampleTime(createdAtRounded, rawDateTime, CLOCK_SKEW_TOLERATED_IN_HOURS)
                : rawDateTime;
        return periodicDataSampleDateTime;
    }

    public static Boolean isClockOutOfSync(final DateTime sampleDateTime, final DateTime createdAtRounded) {
        return sampleDateTime.isAfter(createdAtRounded.plusHours(CLOCK_SKEW_TOLERATED_IN_HOURS)) || sampleDateTime.isBefore(createdAtRounded.minusHours(CLOCK_SKEW_TOLERATED_IN_HOURS));
    }

    public static Integer getFirmwareVersion(final DataInputProtos.BatchPeriodicDataWorker batchPeriodicDataWorker, final DataInputProtos.periodic_data periodicData) {
        // Grab FW version from Batch or periodic data for EVT units
        final Integer firmwareVersion =  batchPeriodicDataWorker.getData().hasFirmwareVersion()
                ? batchPeriodicDataWorker.getData().getFirmwareVersion()
                : periodicData.getFirmwareVersion();
        return firmwareVersion;
    }

    public static Map<Long, DateTimeZone> getTimezonesByUser(final String deviceName,
                                                             final DataInputProtos.BatchPeriodicDataWorker batchPeriodicDataWorker,
                                                             final List<Long> accountsList,
                                                             final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                                                             final boolean hasKinesisTimezonesEnabled) {
        final Map<Long, DateTimeZone> map = Maps.newHashMap();
        for (final DataInputProtos.AccountMetadata accountMetadata : batchPeriodicDataWorker.getTimezonesList()) {
            map.put(accountMetadata.getAccountId(), DateTimeZone.forID(accountMetadata.getTimezone()));
        }

        for (final Long accountId : accountsList) {
            if (!map.containsKey(accountId)) {
                LOGGER.warn("Found account_id {} in account_device_map but not in alarm_info for device_id {}", accountId, deviceName);
            }
        }

        // Kinesis, DynamoDB and Postgres have a consistent view of accounts
        // move on
        if (!map.isEmpty() && map.size() == accountsList.size() && hasKinesisTimezonesEnabled) {
            return map;
        }


        // At this point we need to go to dynamoDB
        LOGGER.warn("Querying dynamoDB. One or several timezones not found in Kinesis message for device_id = {}.", deviceName);

        int retries = 2;
        for (int i = 0; i < retries; i++) {
            try {
                final List<UserInfo> userInfoList = mergedUserInfoDynamoDB.getInfo(deviceName);
                for (UserInfo userInfo : userInfoList) {
                    if (userInfo.timeZone.isPresent()) {
                        map.put(userInfo.accountId, userInfo.timeZone.get());
                    }
                }
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

        return map;

    }

    public static DeviceData.Builder periodicDataToDeviceDataBuilder(final DataInputProtos.periodic_data periodicData) {
        return new DeviceData.Builder()
                .withAmbientTemperature(periodicData.getTemperature())
                .withAmbientAirQualityRaw(periodicData.getDust())
                .withAmbientDustVariance(periodicData.getDustVariability())
                .withAmbientDustMin(periodicData.getDustMin())
                .withAmbientDustMax(periodicData.getDustMax())
                .withAmbientHumidity(periodicData.getHumidity())
                .withAmbientLight(periodicData.getLight())
                .withAmbientLightVariance(periodicData.getLightVariability())
                .withAmbientLightPeakiness(periodicData.getLightTonality())
                .withWaveCount(periodicData.hasWaveCount() ? periodicData.getWaveCount() : 0)
                .withHoldCount(periodicData.hasHoldCount() ? periodicData.getHoldCount() : 0)
                .withAudioNumDisturbances(periodicData.hasAudioNumDisturbances() ? periodicData.getAudioNumDisturbances() : 0)
                .withAudioPeakDisturbancesDB(periodicData.hasAudioPeakDisturbanceEnergyDb() ? periodicData.getAudioPeakDisturbanceEnergyDb() : 0)
                .withAudioPeakBackgroundDB(periodicData.hasAudioPeakBackgroundEnergyDb() ? periodicData.getAudioPeakBackgroundEnergyDb() : 0);
    }

}
