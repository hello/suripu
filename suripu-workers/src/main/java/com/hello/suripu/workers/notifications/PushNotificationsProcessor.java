package com.hello.suripu.workers.notifications;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.DataInputProtos;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.CurrentRoomState;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.notifications.HelloPushMessage;
import com.hello.suripu.core.notifications.MobilePushNotificationProcessor;
import com.hello.suripu.core.preferences.AccountPreferencesDynamoDB;
import com.hello.suripu.core.preferences.PreferenceName;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PushNotificationsProcessor extends HelloBaseRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(PushNotificationsProcessor.class);

    private final MobilePushNotificationProcessor mobilePushNotificationProcessor;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;

    private final AccountPreferencesDynamoDB accountPreferencesDynamoDB;
    private final ImmutableSet<Integer> activeHours;
    private final Set<String> sent = Sets.newHashSet();

    public PushNotificationsProcessor(
            final MobilePushNotificationProcessor mobilePushNotificationProcessor,
            final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
            final AccountPreferencesDynamoDB accountPreferencesDynamoDB,
            final Set<Integer> activeHours) {
        this.mobilePushNotificationProcessor = mobilePushNotificationProcessor;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.accountPreferencesDynamoDB = accountPreferencesDynamoDB;
        this.activeHours = ImmutableSet.copyOf(activeHours);
    }

    @Override
    public void initialize(String s) {

    }

    @Override
    public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        for(final Record record : records) {
            DataInputProtos.BatchPeriodicDataWorker batchPeriodicDataWorker;
            try {
                batchPeriodicDataWorker = DataInputProtos.BatchPeriodicDataWorker.parseFrom(record.getData().array());
                sendMessage(batchPeriodicDataWorker.getData());
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed parsing protobuf: {}", e.getMessage());
                LOGGER.error("Moving to next record");
                continue;
            } catch (Exception e) {
                LOGGER.error("{}", e.getMessage());
            }
        }

        // We do not checkpoint since we are using LATEST strategy, only going through new messages
    }


    /**
     * Send push notifications if conditions warrant it and within the hours
     * @param batched_periodic_data
     */
    private void sendMessage(final DataInputProtos.batched_periodic_data batched_periodic_data) {
        final String senseId = batched_periodic_data.getDeviceId();
        final List<UserInfo> userInfos = mergedUserInfoDynamoDB.getInfo(senseId);
        for(final UserInfo userInfo : userInfos) {

            if(!userHasPushNotificationsEnabled(userInfo.accountId)) {
                continue;
            }

            final Optional<DateTimeZone> dateTimeZoneOptional = userInfo.timeZone;
            if(!dateTimeZoneOptional.isPresent()) {
                LOGGER.warn("No timezone for account: {} paired to Sense: {}", userInfo.accountId, senseId);
                continue;
            }

            final DateTime nowInLocalTimeZone = DateTime.now().withZone(dateTimeZoneOptional.get());
            if(!activeHours.contains(nowInLocalTimeZone.getHourOfDay())) {
                continue;
            }

            final String key = String.format("%s-%s", String.valueOf(userInfo.accountId), nowInLocalTimeZone.toString(DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT)));
            if(sent.contains(key)) {
                LOGGER.info("Account {}, already received push notification: {}", userInfo.accountId, key);
                continue;
            }

            if(!accountPreferencesDynamoDB.isEnabled(userInfo.accountId, PreferenceName.PUSH_ALERT_CONDITIONS)) {
                continue;
            }

            // TODO: write to cache to avoid sending multiple notifications
            for(DataInputProtos.periodic_data data: batched_periodic_data.getDataList()) {
                final Long timestampMillis = data.getUnixTime() * 1000L;
                final DateTime roundedDateTime = new DateTime(timestampMillis, DateTimeZone.UTC).withSecondOfMinute(0);
                final DateTime now = DateTime.now(DateTimeZone.UTC);
                final CurrentRoomState currentRoomState = CurrentRoomState.fromRawData(data.getTemperature(), data.getHumidity(), data.getDustMax(), data.getLight(), data.getAudioPeakBackgroundEnergyDb(), data.getAudioPeakDisturbanceEnergyDb(),
                        roundedDateTime.getMillis(),
                        data.getFirmwareVersion(),
                        now,
                        10,
                        Calibration.createDefault(data.getDeviceId())); // TODO: adjust threshold
                final Optional<HelloPushMessage> messageOptional = getMostImportantSensorState(currentRoomState);
                if(messageOptional.isPresent()) {
                    LOGGER.info("Sending push notifications to user: {}. Message: {}", userInfo.accountId, messageOptional.get());
                    mobilePushNotificationProcessor.push(userInfo.accountId, messageOptional.get());
                    sent.add(key);
                }
                break;
            }
        }
    }

    /**
     * Prioritizes conditions alerts based on Sensors
     * @param currentRoomState
     * @return
     */
    private Optional<HelloPushMessage> getMostImportantSensorState(final CurrentRoomState currentRoomState) {
        final HashSet<CurrentRoomState.State.Condition> notificationStates = Sets.newHashSet(CurrentRoomState.State.Condition.ALERT, CurrentRoomState.State.Condition.WARNING);

        if(notificationStates.contains(currentRoomState.temperature.condition)) {
            return Optional.of(HelloPushMessage.fromSensors(currentRoomState.temperature.message, Sensor.TEMPERATURE));
        }

        if(notificationStates.contains(currentRoomState.humidity.condition)) {
            return Optional.of(HelloPushMessage.fromSensors(currentRoomState.humidity.message, Sensor.HUMIDITY));
        }

        return Optional.absent();
    }

    @Override
    public void shutdown(IRecordProcessorCheckpointer iRecordProcessorCheckpointer, ShutdownReason shutdownReason) {

    }
}
