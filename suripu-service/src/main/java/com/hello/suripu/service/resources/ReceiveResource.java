package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.google.protobuf.TextFormat;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.api.audio.AudioControlProtos;
import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.api.input.DataInputProtos;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.api.output.OutputProtos;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.MergedAlarmInfoDynamoDB;
import com.hello.suripu.core.firmware.FirmwareUpdateStore;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.flipper.GroupFlipper;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import com.hello.suripu.core.models.AlarmInfo;
import com.hello.suripu.core.models.CurrentRoomState;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.processors.RingProcessor;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.DeviceIdUtil;
import com.hello.suripu.core.util.RoomConditionUtil;
import com.hello.suripu.service.SignedMessage;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Path("/in")
public class ReceiveResource extends BaseResource {

    @Inject
    RolloutClient featureFlipper;


    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiveResource.class);
    private static final int CLOCK_SKEW_TOLERATED_IN_HOURS = 2;

    private final DeviceDataDAO deviceDataDAO;
    private final DeviceDAO deviceDAO;
    private final KeyStore keyStore;
    private final MergedAlarmInfoDynamoDB mergedInfoDynamoDB;

    private final KinesisLoggerFactory kinesisLoggerFactory;
    private final Boolean debug;

    private final FirmwareUpdateStore firmwareUpdateStore;
    private final GroupFlipper groupFlipper;

    @Context
    HttpServletRequest request;

    public ReceiveResource(final DeviceDataDAO deviceDataDAO,
                           final DeviceDAO deviceDAO,
                           final KeyStore keyStore,
                           final KinesisLoggerFactory kinesisLoggerFactory,
                           final MergedAlarmInfoDynamoDB mergedInfoDynamoDB,
                           final Boolean debug,
                           final FirmwareUpdateStore firmwareUpdateStore,
                           final GroupFlipper groupFlipper) {
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;

        this.keyStore = keyStore;
        this.kinesisLoggerFactory = kinesisLoggerFactory;

        this.mergedInfoDynamoDB = mergedInfoDynamoDB;

        this.debug = debug;

        this.firmwareUpdateStore = firmwareUpdateStore;
        this.groupFlipper = groupFlipper;
    }



    @POST
    @Path("/sense/batch")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Timed
    public byte[] receiveBatchSenseData(final byte[] body) {
        final SignedMessage signedMessage = SignedMessage.parse(body);
        DataInputProtos.batched_periodic_data data = null;

        try {
            data = DataInputProtos.batched_periodic_data.parseFrom(signedMessage.body);
        } catch (IOException exception) {
            final String errorMessage = String.format("Failed parsing protobuf: %s", exception.getMessage());
            LOGGER.error(errorMessage);

            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity((debug) ? errorMessage : "bad request")
                    .type(MediaType.TEXT_PLAIN_TYPE).build()
            );
        }
        LOGGER.debug("Received protobuf message {}", TextFormat.shortDebugString(data));


        LOGGER.debug("Received valid protobuf {}", data.toString());
        LOGGER.debug("Received protobuf message {}", TextFormat.shortDebugString(data));

        final Optional<byte[]> optionalKeyBytes = keyStore.get(data.getDeviceId());
        if(!optionalKeyBytes.isPresent()) {
            LOGGER.error("Failed to get key from key store for device_id = {}", data.getDeviceId());
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Optional<SignedMessage.Error> error = signedMessage.validateWithKey(optionalKeyBytes.get());

        if(error.isPresent()) {
            LOGGER.error(error.get().message);
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity((debug) ? error.get().message : "bad request")
                    .type(MediaType.TEXT_PLAIN_TYPE).build()
            );
        }

        final String ipAddress = (request.getHeader("X-Forwarded-For") == null) ? "" : request.getHeader("X-Forwarded-For");

        final DataInputProtos.BatchPeriodicDataWorker batchPeriodicDataWorkerMessage = DataInputProtos.BatchPeriodicDataWorker.newBuilder()
                .setData(data)
                .setReceivedAt(DateTime.now().getMillis())
                .setIpAddress(ipAddress)
                .build();

        final DataLogger batchSenseDataLogger = kinesisLoggerFactory.get(QueueName.SENSE_SENSORS_DATA);
        batchSenseDataLogger.put(data.getDeviceId(), batchPeriodicDataWorkerMessage.toByteArray());
        return generateSyncResponse(data.getDeviceId(), data.getFirmwareVersion(), optionalKeyBytes.get(), data);
    }


    @Deprecated
    @POST
    @Path("/morpheus/pb2")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Timed
    public byte[] morpheusProtobufReceiveEncrypted(final byte[] body) {
        final SignedMessage signedMessage = SignedMessage.parse(body);
        DataInputProtos.periodic_data data = null;

        try {
            data = DataInputProtos.periodic_data.parseFrom(signedMessage.body);
        } catch (IOException exception) {
            final String errorMessage = String.format("Failed parsing protobuf: %s", exception.getMessage());
            LOGGER.error(errorMessage);

            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity((debug) ? errorMessage : "bad request")
                    .type(MediaType.TEXT_PLAIN_TYPE).build()
            );
        }
        LOGGER.debug("Received protobuf message {}", TextFormat.shortDebugString(data));


        // get MAC address of morpheus
        final Optional<String> deviceIdOptional = DeviceIdUtil.getMorpheusId(data);
        if(!deviceIdOptional.isPresent()){
            LOGGER.error("Cannot get morpheus id");
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity((debug) ? "Cannot get morpheus id" : "bad request")
                    .type(MediaType.TEXT_PLAIN_TYPE).build()
            );
        }


        final String deviceName = deviceIdOptional.get();
        LOGGER.debug("Received valid protobuf {}", deviceName.toString());
        LOGGER.debug("Received protobuf message {}", TextFormat.shortDebugString(data));

        final Optional<byte[]> optionalKeyBytes = keyStore.get(data.getDeviceId());
        if(!optionalKeyBytes.isPresent()) {
            LOGGER.error("Failed to get key from key store for device_id = {}", data.getDeviceId());
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Optional<SignedMessage.Error> error = signedMessage.validateWithKey(optionalKeyBytes.get());

        if(error.isPresent()) {
            LOGGER.error(error.get().message);
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity((debug) ? error.get().message : "bad request")
                    .type(MediaType.TEXT_PLAIN_TYPE).build()
            );
        }


        final DataInputProtos.batched_periodic_data batch = DataInputProtos.batched_periodic_data.newBuilder()
                .addData(data)
                .setDeviceId(data.getDeviceId())
                .setFirmwareVersion(data.getFirmwareVersion())
                .build();


        final String ipAddress = (request.getHeader("X-Forwarded-For") == null) ? "" : request.getHeader("X-Forwarded-For");

        final DataInputProtos.BatchPeriodicDataWorker batchPeriodicDataWorkerMessage = DataInputProtos.BatchPeriodicDataWorker.newBuilder()
                .setData(batch)
                .setReceivedAt(DateTime.now().getMillis())
                .setIpAddress(ipAddress)
                .build();

        // Saving sense data to kinesis
        final DataLogger senseSensorsDataLogger = kinesisLoggerFactory.get(QueueName.SENSE_SENSORS_DATA);
        senseSensorsDataLogger.put(deviceName, batchPeriodicDataWorkerMessage.toByteArray());

        return generateSyncResponse(data.getDeviceId(), data.getFirmwareVersion(), optionalKeyBytes.get(), batch);
    }


    private OutputProtos.SyncResponse.Alarm.Builder getAlarmBuilderFromNextRingTime(final String deviceId, final List<AlarmInfo> alarmInfoFromThatDevice){
        RingTime nextRingTimeFromWorker = RingTime.createEmpty();

        Optional<DateTimeZone> userTimeZoneOptional = Optional.absent();

        //// Start: Try get the user time zone and ring time generated by smart alarm worker /////////
        for(final AlarmInfo alarmInfo:alarmInfoFromThatDevice){

            if(!alarmInfo.deviceId.equals(deviceId)){
                LOGGER.warn("alarm info list contains data not from device {}, got {}", deviceId, alarmInfo.deviceId);
                continue;
            }

            if(alarmInfo.timeZone.isPresent()){
                userTimeZoneOptional = Optional.of(alarmInfo.timeZone.get());
            }else{
                LOGGER.warn("User {} on device {} time zone not set.", alarmInfo.accountId, alarmInfo.deviceId);
                continue;
            }

            if(alarmInfo.ringTime.isPresent()){
                if(alarmInfo.ringTime.get().isEmpty()){
                    continue;
                }

                if(nextRingTimeFromWorker.isEmpty()){
                    nextRingTimeFromWorker = alarmInfo.ringTime.get();
                }else{
                    if(alarmInfo.ringTime.get().actualRingTimeUTC < nextRingTimeFromWorker.actualRingTimeUTC){
                        nextRingTimeFromWorker = alarmInfo.ringTime.get();
                    }
                }
            }
        }

        if(!userTimeZoneOptional.isPresent()){  // No user timezone set, bail out.
            return OutputProtos.SyncResponse.Alarm.newBuilder().setRingOffsetFromNowInSecond(-1);

        }
        //////End: Try get the user time zone and ring time generated by smart alarm worker /////////


        //// Start: Compute next ring time on-the-fly based on alarm templates, just in case the smart alarm worker dead /////
        Optional<RingTime> nextRegularRingTimeOptional = Optional.absent();
        int ringOffsetFromNowInSecond = -1;
        int ringDurationInMS = 30 * DateTimeConstants.MILLIS_PER_SECOND;  // TODO: make this flexible so we can adjust based on user preferences.
        long nextRingTimestamp = 0;

        try {
            nextRegularRingTimeOptional = Optional.of(RingProcessor.getNextRegularRingTime(alarmInfoFromThatDevice,
                    deviceId,
                    DateTime.now()));
        }catch (Exception ex){
            LOGGER.error("Get next regular ring time for device {} failed: {}", deviceId, ex.getMessage());
        }
        //// End: Compute next ring time on the fly based on alarm template, just in case the smart alarm worker dead /////


        //// Start: Decide which ring time to be used: on-the-fly or the one generated by smart alarm worker? //////////
        if(nextRegularRingTimeOptional.isPresent()) {
            // By default, we use the on-the-fly one, because alarm worker may die.
            final RingTime nextRegularRingTime = nextRegularRingTimeOptional.get();

            if(nextRegularRingTime.isEmpty()){  // No alarm at all
                nextRingTimestamp = 0;
                ringOffsetFromNowInSecond = -1;
            }else {
                if (nextRegularRingTime.expectedRingTimeUTC == nextRingTimeFromWorker.expectedRingTimeUTC) {
                    // on-the-fly and ring from worker are from the same alarm, use the one from worker
                    // since it is "smart"
                    nextRingTimestamp = nextRingTimeFromWorker.actualRingTimeUTC;
                }

                if (nextRegularRingTime.expectedRingTimeUTC > nextRingTimeFromWorker.expectedRingTimeUTC) {
                    // We are in the intermediate time gap when:
                    // 1) The last ring generated by alarm worker has been fired, but
                    // 2) the alarm worker not yet receive the next data to trigger processing the next ring.
                    // 3) This is the data to trigger the worker to process the next ring (If it is still alive).
                    //
                    // Use the generated on-the-fly ring time.
                    LOGGER.warn("Ring time in merge table for device {} needs to update.", deviceId);
                    nextRingTimestamp = nextRegularRingTime.expectedRingTimeUTC;
                }

                if(nextRegularRingTime.expectedRingTimeUTC < nextRingTimeFromWorker.expectedRingTimeUTC){
                    // This should never happen.
                    // The generated on-the-fly ring time is older than ring time generated by alarm worker
                    // The system might be restarted between last ring time is generated and now, and have a
                    // wrong system time!
                    // Or there is a bug somewhere!
                    //
                    // In this case we assume the last ring time from worker is correct, use the ring time from worker.
                    LOGGER.error("Next ring time from template is smaller than that from worker, error! device id {}", deviceId);
                    nextRingTimestamp = nextRingTimeFromWorker.actualRingTimeUTC;
                }
            }

            LOGGER.debug("Next ring time: {}", new DateTime(nextRingTimestamp, userTimeZoneOptional.get()));

            if(nextRingTimestamp != 0) {
                ringOffsetFromNowInSecond = (int) ((nextRingTimestamp - DateTime.now().getMillis()) / DateTimeConstants.MILLIS_PER_SECOND);
            }
        }



        final OutputProtos.SyncResponse.Alarm.Builder alarmBuilder = OutputProtos.SyncResponse.Alarm.newBuilder()
                .setStartTime((int) (nextRingTimestamp / DateTimeConstants.MILLIS_PER_SECOND))
                .setEndTime((int) ((nextRingTimestamp + ringDurationInMS) / DateTimeConstants.MILLIS_PER_SECOND))
                .setRingDurationInSecond(ringDurationInMS / DateTimeConstants.MILLIS_PER_SECOND)
                .setRingOffsetFromNowInSecond(ringOffsetFromNowInSecond);

        return alarmBuilder;
    }

    /**
     * Persists data and generates SyncResponse
     * @param deviceName
     * @param firmwareVersion
     * @param encryptionKey
     * @param batch
     * @return
     */
    private byte[] generateSyncResponse(final String deviceName, final int firmwareVersion, final byte[] encryptionKey, final DataInputProtos.batched_periodic_data batch) {
        // TODO: Warning, since we query dynamoDB based on user input, the user can generate a lot of
        // requests to break our bank(Assume that Dynamo DB never goes down).
        // May be we should somehow cache these data to reduce load & cost.

        final List<AlarmInfo> alarmInfoList = new ArrayList<>();

        try {
            alarmInfoList.addAll(this.mergedInfoDynamoDB.getInfo(deviceName));  // get alarm related info from DynamoDB "cache".
        }catch (Exception ex){
            LOGGER.error("Failed to retrieve info from merge info db for device {}: {}", deviceName, ex.getMessage());
        }
        LOGGER.debug("Found {} pairs", alarmInfoList.size());

        // This is the default timezone.
        DateTimeZone userTimeZone = DateTimeZone.forID("America/Los_Angeles");
        final OutputProtos.SyncResponse.Builder responseBuilder = OutputProtos.SyncResponse.newBuilder();


        for(DataInputProtos.periodic_data data : batch.getDataList()) {
            final Long timestampMillis = data.getUnixTime() * 1000L;
            final DateTime roundedDateTime = new DateTime(timestampMillis, DateTimeZone.UTC).withSecondOfMinute(0);
            if(roundedDateTime.isAfter(DateTime.now().plusHours(CLOCK_SKEW_TOLERATED_IN_HOURS)) || roundedDateTime.isBefore(DateTime.now().minusHours(CLOCK_SKEW_TOLERATED_IN_HOURS))) {
                LOGGER.error("The clock for device {} is not within reasonable bounds (2h)", data.getDeviceId());
                LOGGER.error("Current time = {}, received time = {}", DateTime.now(), roundedDateTime);
                // TODO: throw exception?
                // throw new WebApplicationException(Response.Status.BAD_REQUEST);
                continue;
            }

            final CurrentRoomState currentRoomState = CurrentRoomState.fromRawData(data.getTemperature(), data.getHumidity(), data.getDustMax(),
                    roundedDateTime.getMillis(),
                    data.getFirmwareVersion(),
                    DateTime.now(),
                    2);

            responseBuilder.setRoomConditions(
                    OutputProtos.SyncResponse.RoomConditions.valueOf(
                            RoomConditionUtil.getGeneralRoomCondition(currentRoomState).ordinal()));


        }





        // TODO: Fix the IndexOutOfBoundException
//        for(int i = 0; i < replyRingTime.soundIds.length; i++){
//            alarmBuilder.setRingtoneIds(i, replyRingTime.soundIds[i]);
//        }

        responseBuilder.setAlarm(getAlarmBuilderFromNextRingTime(deviceName, alarmInfoList).build());

        final String firmwareFeature = String.format("firmware_release_%s", firmwareVersion);
        final List<String> groups = groupFlipper.getGroups(deviceName);
        if(featureFlipper.deviceFeatureActive(firmwareFeature, deviceName, groups)) {
            LOGGER.debug("Feature is active!");
        }

        // groups take precedence over feature
        if(!groups.isEmpty()) {
            LOGGER.debug("DeviceId {} belongs to groups: {}", deviceName, groups);
            final List<OutputProtos.SyncResponse.FileDownload> fileDownloadList = firmwareUpdateStore.getFirmwareUpdate(deviceName, groups.get(0), firmwareVersion);
            LOGGER.debug("{} files added to syncResponse to be downloaded", fileDownloadList.size());
            responseBuilder.addAllFiles(fileDownloadList);
        } else {

            if(featureFlipper.deviceFeatureActive(FeatureFlipper.OTA_RELEASE, deviceName, groups)) {
                LOGGER.debug("Feature release is active!");
                final List<OutputProtos.SyncResponse.FileDownload> fileDownloadList = firmwareUpdateStore.getFirmwareUpdate(deviceName, FeatureFlipper.OTA_RELEASE, firmwareVersion);
                LOGGER.debug("{} files added to syncResponse to be downloaded", fileDownloadList.size());
                responseBuilder.addAllFiles(fileDownloadList);
            }
        }

        final AudioControlProtos.AudioControl.Builder audioControl = AudioControlProtos.AudioControl
                .newBuilder()
                .setAudioCaptureAction(AudioControlProtos.AudioControl.AudioCaptureAction.OFF);

        final DateTime now = DateTime.now(DateTimeZone.forID("America/Los_Angeles"));
        final Boolean audioRecordingWindow = now.getHourOfDay() > 0 && now.getHourOfDay() < 7;

        if(featureFlipper.deviceFeatureActive(FeatureFlipper.AUDIO_CAPTURE, deviceName, groups) && audioRecordingWindow) {
            LOGGER.debug("AUDIO_CAPTURE feature is active for device_id = {}", deviceName);
            audioControl.setAudioCaptureAction(AudioControlProtos.AudioControl.AudioCaptureAction.ON);
        }

        responseBuilder.setAudioControl(audioControl);

        final OutputProtos.SyncResponse syncResponse = responseBuilder.build();

        LOGGER.debug("Len pb = {}", syncResponse.toByteArray().length);

        final Optional<byte[]> signedResponse = SignedMessage.sign(syncResponse.toByteArray(), encryptionKey);
        if(!signedResponse.isPresent()) {
            LOGGER.error("Failed signing message");
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity((debug) ? "Failed signing message" : "server error")
                    .type(MediaType.TEXT_PLAIN_TYPE).build()
            );
        }

        return signedResponse.get();
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

            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity((debug) ? errorMessage : "bad request")
                    .type(MediaType.TEXT_PLAIN_TYPE).build()
            );
        }
        LOGGER.debug("Received for pill protobuf message {}", TextFormat.shortDebugString(batchPilldata));


        final Optional<byte[]> optionalKeyBytes = keyStore.get(batchPilldata.getDeviceId());
        if(!optionalKeyBytes.isPresent()) {
            LOGGER.error("Failed to get key from key store for device_id = {}", batchPilldata.getDeviceId());
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        final Optional<SignedMessage.Error> error = signedMessage.validateWithKey(optionalKeyBytes.get());

        if(error.isPresent()) {
            LOGGER.error(error.get().message);
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity((debug) ? error.get().message : "bad request")
                    .type(MediaType.TEXT_PLAIN_TYPE).build()
            );
        }

        final SenseCommandProtos.batched_pill_data.Builder cleanBatch = SenseCommandProtos.batched_pill_data.newBuilder();
        cleanBatch.setDeviceId(batchPilldata.getDeviceId());

        for(final SenseCommandProtos.pill_data pill : batchPilldata.getPillsList()) {
            final DateTime now = DateTime.now();
            final Long pillTimestamp = pill.getTimestamp() * 1000;
            if(pillTimestamp > now.plusHours(CLOCK_SKEW_TOLERATED_IN_HOURS).getMillis()) {
                LOGGER.warn("Pill data timestamp is too much in the future. now = {}, timestamp = {}", now, pillTimestamp);
                continue;
            }
            cleanBatch.addPills(pill);
        }

        // Put raw pill data into Kinesis
        final DataLogger batchDataLogger = kinesisLoggerFactory.get(QueueName.BATCH_PILL_DATA);
        batchDataLogger.put(batchPilldata.getDeviceId(),  cleanBatch.build().toByteArray());


        // TODO: everything below this is kept for backward compatibility
        // TODO: remove is shortly after we've migrated to new worker

        // This is the default timezone.
        DateTimeZone userTimeZone = DateTimeZone.forID("America/Los_Angeles");
        final String senseId  = batchPilldata.getDeviceId();
        final List<AlarmInfo> alarmInfoList = this.mergedInfoDynamoDB.getInfo(senseId);
        for(final AlarmInfo info:alarmInfoList){
            if(info.timeZone.isPresent()){
                userTimeZone = info.timeZone.get();
            }
        }

        // TODO: MOVE THIS TO THE WORKERS! SURIPU-SERVICE SHOULD NOT BE TALKING TO POSTGRESQL AS MUCH AS POSSIBLE
        // ********************* Pill Data Storage ****************************
        if(cleanBatch.getPillsCount() > 0){
            for(final SenseCommandProtos.pill_data pill: cleanBatch.getPillsList()){

                final String pillId = pill.getDeviceId();
                final Optional<DeviceAccountPair> internalPillPairingMap = this.deviceDAO.getInternalPillId(pillId);

                if(!internalPillPairingMap.isPresent()){
                    LOGGER.warn("Cannot find internal pill id for pill {}", pillId);
                    continue;
                }

                final InputProtos.PillDataKinesis.Builder pillKinesisDataBuilder = InputProtos.PillDataKinesis.newBuilder();

                final long timestampMillis = pill.getTimestamp() * 1000L;
                final DateTime roundedDateTime = new DateTime(timestampMillis, DateTimeZone.UTC)
                        .withSecondOfMinute(0);

                pillKinesisDataBuilder.setAccountIdLong(internalPillPairingMap.get().accountId)
                        .setPillId(pillId)
                        .setPillIdLong(internalPillPairingMap.get().internalDeviceId)
                        .setTimestamp(roundedDateTime.getMillis())
                        .setOffsetMillis(userTimeZone.getOffset(roundedDateTime));



                if(pill.hasBatteryLevel()){
                    pillKinesisDataBuilder.setBatteryLevel(pill.getBatteryLevel());
                }

                if(pill.hasFirmwareVersion()){
                    pillKinesisDataBuilder.setFirmwareVersion(pill.getFirmwareVersion());
                }

                if(pill.hasUptime()){
                    pillKinesisDataBuilder.setUpTime(pill.getUptime());
                }

                if(pill.hasMotionDataEntrypted()){
                    pillKinesisDataBuilder.setEncryptedData(pill.getMotionDataEntrypted());
                }


                final byte[] pillDataBytes = pillKinesisDataBuilder.build().toByteArray();
                final DataLogger dataLogger = kinesisLoggerFactory.get(QueueName.PILL_DATA);
                final String sequenceNumber = dataLogger.put(internalPillPairingMap.get().internalDeviceId.toString(),  // WTF?
                        pillDataBytes);
                LOGGER.trace("Pill Data added to Kinesis with sequenceNumber = {}", sequenceNumber);

            }
        }

        final SenseCommandProtos.MorpheusCommand responseCommand = SenseCommandProtos.MorpheusCommand.newBuilder()
                .setType(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PILL_DATA)
                .setVersion(0)
                .build();

        final Optional<byte[]> signedResponse = SignedMessage.sign(responseCommand.toByteArray(), optionalKeyBytes.get());
        if(!signedResponse.isPresent()) {
            LOGGER.error("Failed signing message");
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity((debug) ? "Failed signing message" : "server error")
                    .type(MediaType.TEXT_PLAIN_TYPE).build()
            );
        }

        return signedResponse.get();
    }
}
