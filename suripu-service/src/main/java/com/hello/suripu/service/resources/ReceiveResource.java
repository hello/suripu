package com.hello.suripu.service.resources;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.protobuf.TextFormat;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.crypto.CryptoHelper;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.MergedAlarmInfoDynamoDB;
import com.hello.suripu.core.db.PublicKeyStore;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import com.hello.suripu.core.models.AlarmInfo;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.TempTrackerData;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.RingProcessor;
import com.hello.suripu.core.util.DeviceIdUtil;
import com.hello.suripu.service.SignedMessage;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Path("/in")
public class ReceiveResource {

    private static final Pattern PG_UNIQ_PATTERN = Pattern.compile("ERROR: duplicate key value violates unique constraint \"(\\w+)\"");
    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiveResource.class);
    private static final int OFFSET_MILLIS = -25200000;

    private final DeviceDataDAO deviceDataDAO;
    private final DeviceDAO deviceDAO;
    private final PublicKeyStore publicKeyStore;
    private final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB;

    private final KinesisLoggerFactory kinesisLoggerFactory;
    private final CryptoHelper cryptoHelper;
    private final Boolean debug;

    @Context
    HttpServletRequest request;

    private final LoadingCache<String, Optional<byte[]>> cache;

    public ReceiveResource(final DeviceDataDAO deviceDataDAO,
                           final DeviceDAO deviceDAO,
                           final PublicKeyStore publicKeyStore,
                           final KinesisLoggerFactory kinesisLoggerFactory,

                           final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB,
                           final Boolean debug) {
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;

        this.publicKeyStore = publicKeyStore;
        cryptoHelper = new CryptoHelper();
        this.kinesisLoggerFactory = kinesisLoggerFactory;

        this.mergedAlarmInfoDynamoDB = mergedAlarmInfoDynamoDB;

        this.debug = debug;

        CacheLoader<String, Optional<byte[]>> loader = new CacheLoader<String, Optional<byte[]>>() {
            public Optional<byte[]> load(String key) {
                return publicKeyStore.get(key);
            }
        };

         cache = CacheBuilder.newBuilder().build(loader);
    }

    @POST
    @Timed
    @Path("/temp/tracker")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public void sendTempData(
            @Valid List<TempTrackerData> trackerData,
            @Scope({OAuthScope.API_INTERNAL_DATA_WRITE}) AccessToken accessToken) {

        if(trackerData.size() == 0){
            LOGGER.info("Account {} tries to upload empty payload.", accessToken.accountId);
            return;
        }

        final List<DeviceAccountPair> pairs = deviceDAO.getTrackerIds(accessToken.accountId);

        final Map<String, Long> pairsLookup = new HashMap<String, Long>(pairs.size());
        for (DeviceAccountPair pair: pairs) {
            pairsLookup.put(pair.externalDeviceId, pair.internalDeviceId);
        }

        if(pairs.isEmpty()) {
            LOGGER.warn("No tracker registered for account = {}", accessToken.accountId);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }

        if(pairs.size() > 1) {
            LOGGER.warn("Too many trackers ({}) for account = {}", pairs.size(), accessToken.accountId);
        }

        for(final TempTrackerData tempTrackerData : trackerData) {

            final Long trackerId = pairsLookup.get(tempTrackerData.trackerId);
            if(trackerId == null) {
                LOGGER.warn("TrackerId {} is not paired to account: {}", tempTrackerData.trackerId, accessToken.accountId);
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
            }

            // add to kinesis - 1 sample per min
            if (tempTrackerData.value > 0) {
                final String pillID = trackerId.toString();

                final InputProtos.PillDataKinesis pillKinesisData = InputProtos.PillDataKinesis.newBuilder()
                        .setAccountIdLong(accessToken.accountId)
                        .setPillIdLong(trackerId)
                        .setTimestamp(tempTrackerData.timestamp)
                        .setValue(TrackerMotion.Utils.rawToMilliMS2((long) tempTrackerData.value)) // in milli-g
                        .setOffsetMillis(this.OFFSET_MILLIS)
                        .build();

                final byte[] pillDataBytes = pillKinesisData.toByteArray();
                final DataLogger dataLogger = kinesisLoggerFactory.get(QueueName.PILL_DATA);
                final String sequenceNumber = dataLogger.put(pillID, pillDataBytes);
                LOGGER.debug("Pill Data added to Kinesis with sequenceNumber = {}", sequenceNumber);
            }
        }
    }

    @POST
    @Path("/morpheus/pb2")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Timed
    public byte[] morpheusProtobufReceiveEncrypted(final byte[] body) {
        final SignedMessage signedMessage = SignedMessage.parse(body);
        InputProtos.periodic_data data = null;

        try {
            data = InputProtos.periodic_data.parseFrom(signedMessage.body);
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

        // TODO: Fetch key from Datastore
        final byte[] keyBytes = "1234567891234567".getBytes();
        final Optional<SignedMessage.Error> error = signedMessage.validateWithKey(keyBytes);

        if(error.isPresent()) {
            LOGGER.error(error.get().message);
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity((debug) ? error.get().message : "bad request")
                    .type(MediaType.TEXT_PLAIN_TYPE).build()
            );
        }

        // TODO: Warning, since we query dynamoDB based on user input, the user can generate a lot of
        // requests to break our bank(Assume that Dynamo DB never goes down).
        // May be we should somehow cache these data to reduce load & cost.
        final List<DeviceAccountPair> deviceAccountPairs = deviceDAO.getAccountIdsForDeviceId(deviceName);
        final List<AlarmInfo> alarmInfoList = this.mergedAlarmInfoDynamoDB.getInfo(deviceName);  // get alarm related info from DynamoDB "cache".
        RingTime nextRingTimeFromWorker = RingTime.createEmpty();

        LOGGER.debug("Found {} pairs", deviceAccountPairs.size());

        long timestampMillis = data.getUnixTime() * 1000L;
        final DateTime roundedDateTime = new DateTime(timestampMillis, DateTimeZone.UTC).withSecondOfMinute(0);
        // This is the default timezone.
        DateTimeZone userTimeZone = DateTimeZone.forID("America/Los_Angeles");

        // ********************* Pill Data Storage ****************************
        if(data.getPillsCount() > 0){
            final List<InputProtos.periodic_data.pill_data> pillList = data.getPillsList();
            for(final InputProtos.periodic_data.pill_data pill:pillList){

                final String pillId = pill.getDeviceId();
                final Optional<DeviceAccountPair> internalPillPairingMap = this.deviceDAO.getInternalPillId(pillId);
                if(!internalPillPairingMap.isPresent()){
                    LOGGER.warn("Cannot find internal pill id for pill {}", pillId);
                    continue;
                }

                final InputProtos.PillDataKinesis.Builder pillKinesisDataBuilder = InputProtos.PillDataKinesis.newBuilder();

                pillKinesisDataBuilder.setAccountIdLong(internalPillPairingMap.get().accountId)
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

                if(pill.hasMotionDataEncrypted()){
                    pillKinesisDataBuilder.setEncryptedData(pill.getMotionDataEncrypted());
                }

                if(pill.hasMotionData()){
                    long amplitude = pill.getMotionData();
                    if(amplitude < 0){
                        // Java is signed
                        amplitude += 0xFFFFFFFF;
                    }
                    pillKinesisDataBuilder.setValue(TrackerMotion.Utils.rawToMilliMS2(amplitude));
                }

                final byte[] pillDataBytes = pillKinesisDataBuilder.build().toByteArray();
                final DataLogger dataLogger = kinesisLoggerFactory.get(QueueName.PILL_DATA);
                final String sequenceNumber = dataLogger.put(internalPillPairingMap.get().internalDeviceId.toString(),  // WTF?
                        pillDataBytes);
                LOGGER.debug("Pill Data added to Kinesis with sequenceNumber = {}", sequenceNumber);

            }
        }

        // ********************* Morpheus Data and Alarm processing **************************
        // Here comes to a discussion: Shall we loop over the device_id:account_id relation based on the
        // DynamoDB "cache" or PostgresSQL accout_device_map table?
        // Looping over DynamoDB can save a hit to the PostgresSQL every minute for every Morpheus,
        // but may run into data consistency issue if the alarm_info table is not update correctly.
        // Looping over the PostgresSQL table can avoid the issue of data consistency, since everything
        // is checked on actual device:account pair. However, this might bring in availability concern.
        //
        // For now, I rely on the actual device_account_map table instead of the dynamo DB cache.
        // Because I think it will make the implementation much simpler. If the PostgreSQL is down, sensor data
        // will not be stored anyway.
        //
        // Pang, 09/26/2014
        for (final DeviceAccountPair pair : deviceAccountPairs) {
            try {
                // Get the timezone for current user.
                for(final AlarmInfo alarmInfo:alarmInfoList){
                    if(alarmInfo.accountId == pair.accountId){
                        if(alarmInfo.timeZone.isPresent()){
                            userTimeZone = alarmInfo.timeZone.get();
                        }else{
                            LOGGER.warn("User {} on device {} time zone not set.", pair.accountId, alarmInfo.deviceId);
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
                }
            }catch (AmazonServiceException awsException){
                // I guess this endpoint should never bail out?
                LOGGER.error("AWS error when retrieving user timezone for account {}", pair.accountId);
            }

            final DeviceData.Builder builder = new DeviceData.Builder()
                    .withAccountId(pair.accountId)
                    .withDeviceId(pair.internalDeviceId)
                    .withAmbientTemperature(data.getTemperature())
                    .withAmbientAirQuality(data.getDust(), data.getFirmwareVersion())
                    .withAmbientAirQualityRaw(data.getDust())
                    .withAmbientDustVariance(data.getDustVariability())
                    .withAmbientDustMin(data.getDustMin())
                    .withAmbientDustMax(data.getDustMax())
                    .withAmbientHumidity(data.getHumidity())
                    .withAmbientLight(data.getLight())
                    .withAmbientLightVariance(data.getLightVariability())
                    .withAmbientLightPeakiness(data.getLightTonality())
                    .withOffsetMillis(userTimeZone.getOffset(roundedDateTime))
                    .withDateTimeUTC(roundedDateTime);

            final DeviceData deviceData = builder.build();

            try {
                deviceDataDAO.insert(deviceData);
                LOGGER.info("Data saved to DB: {}", TextFormat.shortDebugString(data));
            } catch (UnableToExecuteStatementException exception) {
                final Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
                if (!matcher.find()) {
                    LOGGER.error(exception.getMessage());
                    throw new WebApplicationException(
                            Response.status(Response.Status.BAD_REQUEST)
                                    .entity(exception.getMessage())
                                    .type(MediaType.TEXT_PLAIN_TYPE)
                                    .build());
                }

                LOGGER.warn("Duplicate device sensor value for account_id = {}, time: {}", pair.accountId, roundedDateTime);
            }


            final DataLogger dataLogger = kinesisLoggerFactory.get(QueueName.MORPHEUS_DATA);
            final byte[] morpheusDataInBytes = data.toByteArray();
            final String shardingKey = deviceName;

            final String sequenceNumber = dataLogger.put(shardingKey, morpheusDataInBytes);

        }


        final InputProtos.SyncResponse.Builder responseBuilder = InputProtos.SyncResponse.newBuilder();
        final RingTime nextRegularRingTime = RingProcessor.getNextRegularRingTime(alarmInfoList,
                deviceName,
                DateTime.now());

        RingTime replyRingTime = nextRegularRingTime;
        // Now the ring time for different users is sorted, get the nearest one.
        if(nextRegularRingTime.equals(nextRingTimeFromWorker)) {
            replyRingTime = nextRingTimeFromWorker;
        }

        long nextRingTimestamp = replyRingTime.actualRingTimeUTC;

        LOGGER.debug("Next ring time: {}", new DateTime(nextRingTimestamp, userTimeZone));
        int ringDurationInMS = 30 * DateTimeConstants.MILLIS_PER_SECOND;  // TODO: make this flexible so we can adjust based on user preferences.

        final InputProtos.SyncResponse.Alarm.Builder alarmBuilder = InputProtos.SyncResponse.Alarm.newBuilder()
                .setStartTime((int) (nextRingTimestamp / DateTimeConstants.MILLIS_PER_SECOND))
                .setEndTime((int) ((nextRingTimestamp + ringDurationInMS) / DateTimeConstants.MILLIS_PER_SECOND));

        for(int i = 0; i < replyRingTime.soundIds.length; i++){
            alarmBuilder.setRingtoneIds(i, replyRingTime.soundIds[i]);
        }

        responseBuilder.setAlarm(alarmBuilder.build());


        final InputProtos.SyncResponse syncResponse = responseBuilder.build();

        LOGGER.debug("Len pb = {}", syncResponse.toByteArray().length);

        final Optional<byte[]> signedResponse = SignedMessage.sign(syncResponse.toByteArray(), keyBytes);
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
