package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.google.protobuf.TextFormat;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.api.audio.AudioControlProtos;
import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.api.input.DataInputProtos;
import com.hello.suripu.api.output.OutputProtos;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.firmware.FirmwareUpdateStore;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.flipper.GroupFlipper;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.CurrentRoomState;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.processors.OTAProcessor;
import com.hello.suripu.core.processors.RingProcessor;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.HelloHttpHeader;
import com.hello.suripu.core.util.RoomConditionUtil;
import com.hello.suripu.service.SignedMessage;
import com.hello.suripu.service.configuration.OTAConfiguration;
import com.hello.suripu.service.configuration.SenseUploadConfiguration;
import com.hello.suripu.service.models.UploadSettings;
import com.librato.rollout.RolloutClient;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


@Path("/in")
public class ReceiveResource extends BaseResource {

    @Inject
    RolloutClient featureFlipper;


    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiveResource.class);
    private static final int CLOCK_SKEW_TOLERATED_IN_HOURS = 2;
    private static final String LOCAL_OFFICE_IP_ADDRESS = "199.87.82.114";

    private final KeyStore keyStore;
    private final MergedUserInfoDynamoDB mergedInfoDynamoDB;
    private final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB;

    private final KinesisLoggerFactory kinesisLoggerFactory;
    private final Boolean debug;

    private final FirmwareUpdateStore firmwareUpdateStore;
    private final GroupFlipper groupFlipper;
    private final SenseUploadConfiguration senseUploadConfiguration;
    private final OTAConfiguration otaConfiguration;

    @Context
    HttpServletRequest request;

    public ReceiveResource(final KeyStore keyStore,
                           final KinesisLoggerFactory kinesisLoggerFactory,
                           final MergedUserInfoDynamoDB mergedInfoDynamoDB,
                           final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB,
                           final Boolean debug,
                           final FirmwareUpdateStore firmwareUpdateStore,
                           final GroupFlipper groupFlipper,
                           final SenseUploadConfiguration senseUploadConfiguration,
                           final OTAConfiguration otaConfiguration) {

        this.keyStore = keyStore;
        this.kinesisLoggerFactory = kinesisLoggerFactory;

        this.mergedInfoDynamoDB = mergedInfoDynamoDB;
        this.ringTimeHistoryDAODynamoDB = ringTimeHistoryDAODynamoDB;

        this.debug = debug;

        this.firmwareUpdateStore = firmwareUpdateStore;
        this.groupFlipper = groupFlipper;
        this.senseUploadConfiguration = senseUploadConfiguration;
        this.otaConfiguration = otaConfiguration;
    }



    @POST
    @Path("/sense/batch")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Timed
    public byte[] receiveBatchSenseData(final byte[] body) {


        final SignedMessage signedMessage = SignedMessage.parse(body);
        DataInputProtos.batched_periodic_data data = null;

        String debugSenseId = this.request.getHeader(HelloHttpHeader.SENSE_ID);
        if(debugSenseId == null){
            debugSenseId = "";
        }

        LOGGER.info("DebugSenseId device_id = {}", debugSenseId);

        try {
            data = DataInputProtos.batched_periodic_data.parseFrom(signedMessage.body);
        } catch (IOException exception) {
            final String errorMessage = String.format("Failed parsing protobuf for deviceId = %s : %s", debugSenseId, exception.getMessage());
            LOGGER.error(errorMessage);
            return plainTextError(Response.Status.BAD_REQUEST, "bad request");
        }
        LOGGER.debug("Received protobuf message {}", TextFormat.shortDebugString(data));


        LOGGER.debug("Received valid protobuf {}", data.toString());
        LOGGER.debug("Received protobuf message {}", TextFormat.shortDebugString(data));

        if(data.getDeviceId() == null || data.getDeviceId().isEmpty()){
            LOGGER.error("Empty device id");
            return plainTextError(Response.Status.BAD_REQUEST, "empty device id");
        }


        final String deviceId = data.getDeviceId();
        final List<String> groups = groupFlipper.getGroups(deviceId);

        final String ipAddress = getIpAddress(request);

        if(OTAProcessor.isPCH(ipAddress) && !(featureFlipper.deviceFeatureActive(FeatureFlipper.PCH_SPECIAL_OTA, deviceId, groups))){
            // return 202 to not confuse provisioning script with correct test key
            LOGGER.info("IP {} is from PCH. Return HTTP 202", ipAddress);
            return plainTextError(Response.Status.ACCEPTED, "");
        }

        final Optional<byte[]> optionalKeyBytes= getKey(deviceId, groups, ipAddress);

        if(!optionalKeyBytes.isPresent()) {
            LOGGER.error("Failed to get key from key store for device_id = {}", data.getDeviceId());
            return plainTextError(Response.Status.BAD_REQUEST, "");
        }

        final Optional<SignedMessage.Error> error = signedMessage.validateWithKey(optionalKeyBytes.get());

        if(error.isPresent()) {
            LOGGER.error(error.get().message);
            return plainTextError(Response.Status.UNAUTHORIZED, "");
        }


        final DataInputProtos.BatchPeriodicDataWorker batchPeriodicDataWorkerMessage = DataInputProtos.BatchPeriodicDataWorker.newBuilder()
                .setData(data)
                .setReceivedAt(DateTime.now().getMillis())
                .setIpAddress(ipAddress)
                .setUptimeInSecond(data.getUptimeInSecond())
                .build();

        try {
            final DataLogger batchSenseDataLogger = kinesisLoggerFactory.get(QueueName.SENSE_SENSORS_DATA);
            batchSenseDataLogger.put(data.getDeviceId(), batchPeriodicDataWorkerMessage.toByteArray());
        } catch (Exception e) {
            LOGGER.error("Failed to insert into batch sensors kinesis stream: {}", e.getMessage());
        }

        final String tempSenseId = data.hasDeviceId() ? data.getDeviceId() : debugSenseId;
        return generateSyncResponse(tempSenseId, data.getFirmwareVersion(), optionalKeyBytes.get(), data);
    }


    public static OutputProtos.SyncResponse.Builder setPillColors(final List<UserInfo> userInfoList,
                                                                  final OutputProtos.SyncResponse.Builder syncResponseBuilder){
        final ArrayList<OutputProtos.SyncResponse.PillSettings> pillSettings = new ArrayList<>();
        for(final UserInfo userInfo:userInfoList){
            if(userInfo.pillColor.isPresent()){
                pillSettings.add(userInfo.pillColor.get());
            }
        }

        for(int i = pillSettings.size() - 1; i >= 0 && i >= pillSettings.size() - 2; i--){
            syncResponseBuilder.addPillSettings(pillSettings.get(i));
        }

        return syncResponseBuilder;
    }

    /**
     * Persists data and generates SyncResponse
     * @param deviceName
     * @param firmwareVersion
     * @param encryptionKey
     * @param batch
     * @return
     */
    private byte[] generateSyncResponse(final String deviceName,
                                        final int firmwareVersion,
                                        final byte[] encryptionKey,
                                        final DataInputProtos.batched_periodic_data batch) {
        // TODO: Warning, since we query dynamoDB based on user input, the user can generate a lot of
        // requests to break our bank(Assume that Dynamo DB never goes down).
        // May be we should somehow cache these data to reduce load & cost.

        final List<UserInfo> userInfoList = new ArrayList<>();

        try {
            userInfoList.addAll(this.mergedInfoDynamoDB.getInfo(deviceName));  // get alarm related info from DynamoDB "cache".
        }catch (Exception ex){
            LOGGER.error("Failed to retrieve info from merge info db for device {}: {}", deviceName, ex.getMessage());
        }
        LOGGER.debug("Found {} pairs", userInfoList.size());

        final OutputProtos.SyncResponse.Builder responseBuilder = OutputProtos.SyncResponse.newBuilder();


        for(int i = 0; i < batch.getDataCount(); i ++) {
            final DataInputProtos.periodic_data data = batch.getData(i);
            final Long timestampMillis = data.getUnixTime() * 1000L;
            final DateTime roundedDateTime = new DateTime(timestampMillis, DateTimeZone.UTC).withSecondOfMinute(0);
            if(roundedDateTime.isAfter(DateTime.now().plusHours(CLOCK_SKEW_TOLERATED_IN_HOURS)) || roundedDateTime.isBefore(DateTime.now().minusHours(CLOCK_SKEW_TOLERATED_IN_HOURS))) {
                LOGGER.error("The clock for device {} is not within reasonable bounds (2h), current time = {}, received time = {}",
                        data.getDeviceId(),
                        DateTime.now(),
                        roundedDateTime
                        );
                // TODO: throw exception?
                continue;
            }

            // only compute the sate for the most recent conditions
            if(i == batch.getDataCount() -1) {

                final CurrentRoomState currentRoomState = CurrentRoomState.fromRawData(data.getTemperature(), data.getHumidity(), data.getDustMax(), data.getLight(), data.getAudioPeakBackgroundEnergyDb(), data.getAudioPeakDisturbanceEnergyDb(),
                        roundedDateTime.getMillis(),
                        data.getFirmwareVersion(),
                        DateTime.now(),
                        2);

                responseBuilder.setRoomConditions(
                        OutputProtos.SyncResponse.RoomConditions.valueOf(
                                RoomConditionUtil.getGeneralRoomCondition(currentRoomState).ordinal()));

            }
        }

        final Optional<DateTimeZone> userTimeZone = getUserTimeZone(userInfoList);
        final List<String> groups = groupFlipper.getGroups(deviceName);

        if(userTimeZone.isPresent()) {
            final RingTime nextRingTime = RingProcessor.getNextRingTimeForSense(deviceName, userInfoList, DateTime.now());

            // WARNING: now must generated after getNextRingTimeForSense, because that function can take a long time.
            final DateTime now = Alarm.Utils.alignToMinuteGranularity(DateTime.now().withZone(userTimeZone.get()));

            // Start generate protobuf for alarm
            int ringOffsetFromNowInSecond = -1;
            int ringDurationInMS = 2 * DateTimeConstants.MILLIS_PER_MINUTE;
            if (!nextRingTime.isEmpty()) {
                ringOffsetFromNowInSecond = (int) ((nextRingTime.actualRingTimeUTC - now.getMillis()) / DateTimeConstants.MILLIS_PER_SECOND);
                if(ringOffsetFromNowInSecond < 0){
                    // The ring time process took too much time, force the alarm take off immediately
                    ringOffsetFromNowInSecond = 1;
                }
            }

            int soundId = 0;
            if (nextRingTime.soundIds != null && nextRingTime.soundIds.length > 0) {
                soundId = (int) nextRingTime.soundIds[0];
            }
            final OutputProtos.SyncResponse.Alarm.Builder alarmBuilder = OutputProtos.SyncResponse.Alarm.newBuilder()
                    .setStartTime((int) (nextRingTime.actualRingTimeUTC / DateTimeConstants.MILLIS_PER_SECOND))
                    .setEndTime((int) ((nextRingTime.actualRingTimeUTC + ringDurationInMS) / DateTimeConstants.MILLIS_PER_SECOND))
                    .setRingDurationInSecond(ringDurationInMS / DateTimeConstants.MILLIS_PER_SECOND)
                    .setRingtoneId(soundId)
                    .setRingtonePath(Alarm.Utils.getSoundPathFromSoundId(soundId))
                    .setRingOffsetFromNowInSecond(ringOffsetFromNowInSecond);
            responseBuilder.setAlarm(alarmBuilder.build());
            // End generate protobuf for alarm
            
            //Perform all OTA checks and compute the update file list (if necessary)
            final List<OutputProtos.SyncResponse.FileDownload> fileDownloadList = computeOTAFileList(deviceName, groups, userTimeZone.get(), batch);
            if(!fileDownloadList.isEmpty()) {
                responseBuilder.addAllFiles(fileDownloadList);
            }

            final AudioControlProtos.AudioControl.Builder audioControl = AudioControlProtos.AudioControl
                    .newBuilder()
                    .setAudioCaptureAction(AudioControlProtos.AudioControl.AudioCaptureAction.ON)
                    .setAudioSaveFeatures(AudioControlProtos.AudioControl.AudioCaptureAction.OFF)
                    .setAudioSaveRawData(AudioControlProtos.AudioControl.AudioCaptureAction.OFF);

            if (featureFlipper.deviceFeatureActive(FeatureFlipper.ALWAYS_ON_AUDIO, deviceName, groups)) {
                audioControl.setAudioCaptureAction(AudioControlProtos.AudioControl.AudioCaptureAction.ON);
                audioControl.setAudioSaveFeatures(AudioControlProtos.AudioControl.AudioCaptureAction.ON);
                audioControl.setAudioSaveRawData(AudioControlProtos.AudioControl.AudioCaptureAction.ON);
            }


            if (featureFlipper.deviceFeatureActive(FeatureFlipper.ALWAYS_OTA_RELEASE, deviceName, groups) || groups.contains("chris-dev")) {
                responseBuilder.setBatchSize(1);
            } else {

                final int uploadCycle = computeNextUploadInterval(nextRingTime, now, senseUploadConfiguration);
                responseBuilder.setBatchSize(uploadCycle);

            }

            if(shouldWriteRingTimeHistory(now, nextRingTime, responseBuilder.getBatchSize())){
                this.ringTimeHistoryDAODynamoDB.setNextRingTime(deviceName, nextRingTime, now);
            }

            LOGGER.info("{} batch size set to {}", deviceName, responseBuilder.getBatchSize());
            responseBuilder.setAudioControl(audioControl);
            setPillColors(userInfoList, responseBuilder);
        }else{
            LOGGER.error("NO TIMEZONE IS A BIG DEAL.");
            final List<OutputProtos.SyncResponse.FileDownload> fileDownloadList = computeOTAFileList(deviceName, groups, DateTimeZone.UTC, batch);
            if(!fileDownloadList.isEmpty()) {
                responseBuilder.addAllFiles(fileDownloadList);
            }
        }


        final OutputProtos.SyncResponse syncResponse = responseBuilder.build();

        LOGGER.debug("Len pb = {}", syncResponse.toByteArray().length);

        final Optional<byte[]> signedResponse = SignedMessage.sign(syncResponse.toByteArray(), encryptionKey);
        if(!signedResponse.isPresent()) {
            LOGGER.error("Failed signing message");
            return plainTextError(Response.Status.INTERNAL_SERVER_ERROR, "");
        }

        return signedResponse.get();
    }

    public static boolean shouldWriteRingTimeHistory(final DateTime now, final RingTime nextRingTime, final int uploadIntervalInMinutes){
        return now.plusMinutes(uploadIntervalInMinutes).isBefore(nextRingTime.actualRingTimeUTC) == false &&  // now + upload_cycle >= next_ring
                now.isAfter(nextRingTime.actualRingTimeUTC) == false &&
                nextRingTime.isEmpty() == false;
    }


    public static int computeNextUploadInterval(final RingTime nextRingTime, final DateTime now, final SenseUploadConfiguration senseUploadConfiguration){
        int uploadInterval = 1;
        final Long userNextAlarmTimestamp = nextRingTime.expectedRingTimeUTC; // This must be expected time, not actual.
        // Alter upload cycles based on date-time
        uploadInterval = UploadSettings.computeUploadIntervalPerUserPerSetting(now, senseUploadConfiguration);

        // Boost upload cycle based on expected alarm deadline.
        final Integer adjustedUploadInterval = UploadSettings.adjustUploadIntervalInMinutes(now.getMillis(), uploadInterval, userNextAlarmTimestamp);
        if(adjustedUploadInterval < uploadInterval){
            uploadInterval = adjustedUploadInterval;
        }

        // Prolong upload cycle so Sense can safely pass ring time
        uploadInterval = computePassRingTimeUploadInterval(nextRingTime, now, uploadInterval);

        /*if(uploadInterval > senseUploadConfiguration.getLongInterval()){
            uploadInterval = senseUploadConfiguration.getLongInterval();
        }*/

        return uploadInterval;
    }

    public static boolean isNextUploadCrossRingBound(final RingTime nextRingTime, final DateTime now){
        final int ringTimeOffsetFromNowMillis = (int)(nextRingTime.actualRingTimeUTC - now.getMillis());
        return nextRingTime.isEmpty() == false &&
                ringTimeOffsetFromNowMillis <= 2 * DateTimeConstants.MILLIS_PER_MINUTE &&
                ringTimeOffsetFromNowMillis > 0;
    }

    public static int computePassRingTimeUploadInterval(final RingTime nextRingTime, final DateTime now, final int adjustedUploadCycle){
        final int ringTimeOffsetFromNowMillis = (int)(nextRingTime.actualRingTimeUTC - now.getMillis());
        if(isNextUploadCrossRingBound(nextRingTime, now)){
            final int uploadCycleThatPassRingTime = ringTimeOffsetFromNowMillis / DateTimeConstants.MILLIS_PER_MINUTE + 2;
            return uploadCycleThatPassRingTime;
        }

        return adjustedUploadCycle;
    }


    @POST
    @Path("/pill")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Timed
    public byte[] onPillBatchProtobufReceived(final byte[] body) {
        final SignedMessage signedMessage = SignedMessage.parse(body);
        SenseCommandProtos.batched_pill_data batchPilldata = null;

        try {
            batchPilldata = SenseCommandProtos.batched_pill_data.parseFrom(signedMessage.body);
        } catch (IOException exception) {
            final String errorMessage = String.format("Failed parsing protobuf: %s", exception.getMessage());
            LOGGER.error(errorMessage);
            return plainTextError(Response.Status.BAD_REQUEST, "");
        }
        LOGGER.debug("Received for pill protobuf message {}", TextFormat.shortDebugString(batchPilldata));


        final Optional<byte[]> optionalKeyBytes = keyStore.get(batchPilldata.getDeviceId());
        if(!optionalKeyBytes.isPresent()) {
            LOGGER.error("Failed to get key from key store for device_id = {}", batchPilldata.getDeviceId());
            return plainTextError(Response.Status.BAD_REQUEST, "");
        }
        final Optional<SignedMessage.Error> error = signedMessage.validateWithKey(optionalKeyBytes.get());

        if(error.isPresent()) {
            LOGGER.error("Failed validating signature with key: {}", error.get().message);
            return plainTextError(Response.Status.UNAUTHORIZED, "");
        }

        final SenseCommandProtos.batched_pill_data.Builder cleanBatch = SenseCommandProtos.batched_pill_data.newBuilder();
        cleanBatch.setDeviceId(batchPilldata.getDeviceId());

        for(final SenseCommandProtos.pill_data pill : batchPilldata.getPillsList()) {
            final DateTime now = DateTime.now();
            final Long pillTimestamp = pill.getTimestamp() * 1000L;
            if(pillTimestamp > now.plusHours(CLOCK_SKEW_TOLERATED_IN_HOURS).getMillis()) {
                LOGGER.warn("Pill data timestamp from {} is too much in the future. now = {}, timestamp = {}",
                        pill.getDeviceId(),
                        now,
                        new DateTime(pillTimestamp, DateTimeZone.UTC));
                continue;
            }
            cleanBatch.addPills(pill);
        }

        // Put raw pill data into Kinesis
        final DataLogger batchDataLogger = kinesisLoggerFactory.get(QueueName.BATCH_PILL_DATA);
        batchDataLogger.put(batchPilldata.getDeviceId(),  cleanBatch.build().toByteArray());


        final SenseCommandProtos.MorpheusCommand responseCommand = SenseCommandProtos.MorpheusCommand.newBuilder()
                .setType(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PILL_DATA)
                .setVersion(0)
                .build();

        final Optional<byte[]> signedResponse = SignedMessage.sign(responseCommand.toByteArray(), optionalKeyBytes.get());
        if(!signedResponse.isPresent()) {
            LOGGER.error("Failed signing message");
            return plainTextError(Response.Status.INTERNAL_SERVER_ERROR, "");
        }

        return signedResponse.get();
    }

    private Optional<DateTimeZone> getUserTimeZone(List<UserInfo> userInfoList) {
        for(final UserInfo info: userInfoList){
            if(info.timeZone.isPresent()){
                return info.timeZone;
            }
        }
        return Optional.absent();
    }


    /**
     * Performs all OTA availability checks and produces an update file list
     * @param deviceID
     * @param deviceGroups
     * @param userTimeZone
     * @param batchData
     * @return
     */
    private List<OutputProtos.SyncResponse.FileDownload> computeOTAFileList(final String deviceID,
                                                                            final List<String> deviceGroups,
                                                                            final DateTimeZone userTimeZone,
                                                                            final DataInputProtos.batched_periodic_data batchData) {
        final int currentFirmwareVersion = batchData.getFirmwareVersion();
        final int uptimeInSeconds = (batchData.hasUptimeInSecond()) ? batchData.getUptimeInSecond() : -1;
        final DateTime currentDTZ = DateTime.now().withZone(userTimeZone);
        final DateTime startOTAWindow = new DateTime(userTimeZone).withHourOfDay(otaConfiguration.getStartUpdateWindowHour()).withMinuteOfHour(0);
        final DateTime endOTAWindow = new DateTime(userTimeZone).withHourOfDay(otaConfiguration.getEndUpdateWindowHour()).withMinuteOfHour(0);
        final Set<String> alwaysOTAGroups = otaConfiguration.getAlwaysOTAGroups();
        final Integer deviceUptimeDelay = otaConfiguration.getDeviceUptimeDelay();
        final Boolean alwaysOTA = (featureFlipper.deviceFeatureActive(FeatureFlipper.ALWAYS_OTA_RELEASE, deviceID, deviceGroups));
        final String ipAddress = getIpAddress(request);
        final boolean pchOTA = ((featureFlipper.deviceFeatureActive(FeatureFlipper.PCH_SPECIAL_OTA, deviceID, deviceGroups) && OTAProcessor.isPCH(ipAddress)));

        //Provides for an in-office override feature that allows OTA (ignores checks) provided the IP is our office IP.
        if (featureFlipper.deviceFeatureActive(FeatureFlipper.OFFICE_ONLY_OVERRIDE, deviceID, deviceGroups)) {
            if (ipAddress.equals(LOCAL_OFFICE_IP_ADDRESS) && !deviceGroups.isEmpty()) {
                final String updateGroup = deviceGroups.get(0);
                LOGGER.info("Office OTA Override for DeviceId {}", deviceID, deviceGroups);
                final List<OutputProtos.SyncResponse.FileDownload> fileDownloadList = firmwareUpdateStore.getFirmwareUpdate(updateGroup, currentFirmwareVersion, false);
                LOGGER.info("{} files added to syncResponse for OTA of '{}' to DeviceId {}", fileDownloadList.size(), updateGroup, deviceID);
                return fileDownloadList;
            } else {
                return Collections.emptyList();
            }
        }

        if(pchOTA) {
            LOGGER.debug("PCH Special OTA for device: {}", deviceID);
            final List<OutputProtos.SyncResponse.FileDownload> fileDownloadList = firmwareUpdateStore.getFirmwareUpdate(FeatureFlipper.OTA_RELEASE, currentFirmwareVersion, true);
            LOGGER.info("{} files added to syncResponse for PCH Special OTA of 'release' to DeviceId {}", fileDownloadList.size(), deviceID);
            return fileDownloadList;
        }

        final boolean canOTA = OTAProcessor.canDeviceOTA(deviceID, deviceGroups, alwaysOTAGroups, deviceUptimeDelay, uptimeInSeconds, currentDTZ, startOTAWindow, endOTAWindow, alwaysOTA, ipAddress);

        if(canOTA) {

            // groups take precedence over feature
            if (!deviceGroups.isEmpty()) {
                final String updateGroup = deviceGroups.get(0);
                LOGGER.debug("DeviceId {} belongs to groups: {}", deviceID, deviceGroups);
                final List<OutputProtos.SyncResponse.FileDownload> fileDownloadList = firmwareUpdateStore.getFirmwareUpdate(updateGroup, currentFirmwareVersion, false);//TODO: Create a better way of knowing which group the device will belong to
                LOGGER.info("{} files added to syncResponse for OTA of '{}' to DeviceId {}", fileDownloadList.size(), updateGroup, deviceID);
                return fileDownloadList;
            } else {
                if (featureFlipper.deviceFeatureActive(FeatureFlipper.OTA_RELEASE, deviceID, deviceGroups)) {
                    LOGGER.debug("Feature 'release' is active for device: {}", deviceID);
                    final List<OutputProtos.SyncResponse.FileDownload> fileDownloadList = firmwareUpdateStore.getFirmwareUpdate(FeatureFlipper.OTA_RELEASE, currentFirmwareVersion, false);
                    LOGGER.info("{} files added to syncResponse for OTA of 'release' to DeviceId {}", fileDownloadList.size(), deviceID);
                    return fileDownloadList;
                }
            }
        }
        return Collections.emptyList();
    }

    public Optional<byte[]> getKey(String deviceId, List<String> groups, String ipAddress) {

        if (KeyStoreDynamoDB.DEFAULT_FACTORY_DEVICE_ID.equals(deviceId) &&
                featureFlipper.deviceFeatureActive(FeatureFlipper.OFFICE_ONLY_OVERRIDE, deviceId, groups)) {
            if (ipAddress.equals(LOCAL_OFFICE_IP_ADDRESS)) {
                return keyStore.get(deviceId);
            } else {
                return keyStore.getStrict(deviceId);
            }
        }
        return keyStore.get(deviceId);

    }
}
