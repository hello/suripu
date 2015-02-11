package com.hello.suripu.workers.notifications;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.DataInputProtos;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.models.CurrentRoomState;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.notifications.HelloPushMessage;
import com.hello.suripu.core.notifications.MobilePushNotificationProcessor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PushNotificationsProcessor implements IRecordProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(PushNotificationsProcessor.class);

    private final MobilePushNotificationProcessor mobilePushNotificationProcessor;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;

    public PushNotificationsProcessor(final MobilePushNotificationProcessor mobilePushNotificationProcessor, final MergedUserInfoDynamoDB mergedUserInfoDynamoDB) {
        this.mobilePushNotificationProcessor = mobilePushNotificationProcessor;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
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
            finally {
                try {
                    iRecordProcessorCheckpointer.checkpoint();
                } catch (InvalidStateException e) {
                    e.printStackTrace();
                } catch (ShutdownException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Send push notifications if conditions warrant it and within the hours
     * @param batched_periodic_data
     */
    private void sendMessage(final DataInputProtos.batched_periodic_data batched_periodic_data) {
        final String senseId = batched_periodic_data.getDeviceId();
        if(!"F8A21FD2CDAF2E74".equals(senseId)) {
            return;
        }

        final List<UserInfo> userInfos = mergedUserInfoDynamoDB.getInfo(senseId);
        for(final UserInfo userInfo : userInfos) {
            final Optional<DateTimeZone> dateTimeZoneOptional = userInfo.timeZone;
            if(!dateTimeZoneOptional.isPresent()) {
                LOGGER.warn("No timezone for account: {} paired to Sense: {}", userInfo.accountId, senseId);
                continue;
            }

            final DateTime nowInLocalTimeZone = DateTime.now().withZone(dateTimeZoneOptional.get());
            final Set<Integer> onHours = Sets.newHashSet(19,20);
            if(!onHours.contains(nowInLocalTimeZone.getHourOfDay())) {
                return;
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
                        10);
                final Optional<HelloPushMessage> messageOptional = getMostImportantSensorState(currentRoomState);
                if(messageOptional.isPresent()) {
                    mobilePushNotificationProcessor.push(userInfo.accountId, messageOptional.get());
                }
                return; // only attempt to send one message per batch
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

        if(notificationStates.contains(currentRoomState.light.condition)) {
            return Optional.of(HelloPushMessage.fromSensors(currentRoomState.light.message, Sensor.LIGHT));
        }

        if(notificationStates.contains(currentRoomState.sound.condition)) {
            return Optional.of(HelloPushMessage.fromSensors(currentRoomState.sound.message, Sensor.SOUND));
        }

        return Optional.absent();
    }

    @Override
    public void shutdown(IRecordProcessorCheckpointer iRecordProcessorCheckpointer, ShutdownReason shutdownReason) {

    }
}
