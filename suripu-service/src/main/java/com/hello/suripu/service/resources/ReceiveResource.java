package com.hello.suripu.service.resources;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.api.input.InputProtos.SimpleSensorBatch;
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
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.RingProcessor;
import com.hello.suripu.service.SignedMessage;
import com.yammer.metrics.annotation.Timed;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
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

    // for transforming pill-data counts into acceleration
    private static final double COUNTS_IN_G = Math.pow((4.0  * 9.81)/ 65536.0, 2);
    private static final double GRAVITY = 9.81;

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
                final double trackerValueInG = Math.sqrt(tempTrackerData.value.doubleValue() * this.COUNTS_IN_G) - this.GRAVITY;
                final InputProtos.PillDataKinesis pillKinesisData = InputProtos.PillDataKinesis.newBuilder()
                        .setAccountId(accessToken.accountId.toString())
                        .setPillId(pillID)
                        .setTimestamp(tempTrackerData.timestamp)
                        .setValue((long) (trackerValueInG * 1000)) // in milli-g
                        .setOffsetMillis(this.OFFSET_MILLIS)
                        .build();

                final byte[] pillDataBytes = pillKinesisData.toByteArray();
                final DataLogger dataLogger = kinesisLoggerFactory.get(QueueName.PILL_DATA);
                final String sequenceNumber = dataLogger.put(pillID, pillDataBytes);
                LOGGER.debug("Pill Data added to Kinesis with sequenceNumber = {}", sequenceNumber);
            }
        }
    }

    @PUT
    @Path("pill/{pill_id}")
    @Timed
    public Response savePillData(
            @Scope({OAuthScope.SENSORS_BASIC}) AccessToken accessToken,
            @PathParam("pill_id") String pillID,
            byte[] data) {
        // TODO
        final DataLogger dataLogger = kinesisLoggerFactory.get(QueueName.PILL_DATA);

        final InputProtos.PillData pillData = InputProtos.PillData.newBuilder()
                    .setData(ByteString.copyFrom(data))
                    .setPillId(pillID)
                    .setAccountId(accessToken.accountId.toString())
                    .build();

        final byte[] pillDataBytes = pillData.toByteArray();
        final String shardingKey = pillID;

        final String sequenceNumber = dataLogger.put(shardingKey, pillDataBytes);
        LOGGER.debug("Data persisted to Kinesis with sequenceNumber = {}", sequenceNumber);
        return Response.ok().build();
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


        // get MAC address of morpheus
        final byte[] mac = Arrays.copyOf(data.getMac().toByteArray(), 6);
        final String deviceName = new String(Hex.encodeHex(mac));
        LOGGER.debug("Received valid protobuf {}", deviceName.toString());
        LOGGER.debug("Received protobuf message {}", TextFormat.shortDebugString(data));

        // TODO: Fetch key from Datastore
        final byte[] keyBytes = "1234567891234567".getBytes();
        final Optional<SignedMessage.Error> error = signedMessage.validateWithKey(keyBytes);

        if(error.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity((debug) ? error.get().message : "bad request")
                    .type(MediaType.TEXT_PLAIN_TYPE).build()
            );
        }

        // TODO: Warining, since we query dynamoDB based on user input, the user can generate a lot of
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
                    .withAmbientAirQuality(data.getDust())
                    .withAmbientHumidity(data.getHumidity())
                    .withAmbientLight(data.getLight())
                    .withOffsetMillis(userTimeZone.getOffset(roundedDateTime))
                    .withDateTimeUTC(roundedDateTime);

            final DeviceData deviceData = builder.build();

            try {
                deviceDataDAO.insert(deviceData);
                LOGGER.info("Data saved to DB: {}", data);
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



    @POST
    @Path("/morpheus/pb")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Timed
    public String morpheusProtobufReceive(final byte[] body) {


        InputProtos.periodic_data data = null;

        try {
            data = InputProtos.periodic_data.parseFrom(body);
        } catch (IOException exception) {
            final String errorMessage = String.format("Failed parsing protobuf: %s", exception.getMessage());
            LOGGER.error(errorMessage);

            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity((debug) ? errorMessage : "bad request")
                    .type(MediaType.TEXT_PLAIN_TYPE).build()
            );
        }


        // get MAC address of morpheus
        final byte[] mac = Arrays.copyOf(data.getMac().toByteArray(), 6);
        final String deviceName = new String(Hex.encodeHex(mac));
        LOGGER.debug("Received valid protobuf {}", deviceName.toString());
        LOGGER.debug("Received protobuf message {}", TextFormat.shortDebugString(data));


        final List<DeviceAccountPair> deviceAccountPairs = deviceDAO.getAccountIdsForDeviceId(deviceName.toString());
        LOGGER.debug("Found {} pairs", deviceAccountPairs.size());
        long timestampMillis = data.getUnixTime() * 1000L;
        final DateTime roundedDateTime = new DateTime(timestampMillis, DateTimeZone.UTC).withSecondOfMinute(0);

        for (final DeviceAccountPair pair : deviceAccountPairs) {
            final DeviceData.Builder builder = new DeviceData.Builder()
                    .withAccountId(pair.accountId)
                    .withDeviceId(pair.internalDeviceId)
                    .withAmbientTemperature(data.getTemperature())
                    .withAmbientAirQuality(data.getDust())
                    .withAmbientHumidity(data.getHumidity())
                    .withAmbientLight(data.getLight())
                    .withOffsetMillis(-25200000) //TODO: GET THIS FROM MORPHEUS PAYLOAD
                    .withDateTimeUTC(roundedDateTime);

            final DeviceData deviceData = builder.build();

            try {
                deviceDataDAO.insert(deviceData);
                LOGGER.info("Data saved to DB: {}", data);
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
        }

        return "OK";
    }


    @POST
    @Timed
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    public Response receiveSimpleData(
            @Valid InputProtos.SimpleSensorBatch batch,
            @Scope({OAuthScope.SENSORS_BASIC}) AccessToken accessToken) {

        // the accessToken is only used for upload permission at the moment
        // it will soon be removed and rely on device_id and signature from Morpheus
        // TODO: make transition from access token to signature based happen.

        final String deviceName = batch.getDeviceId(); // protobuf deviceId is really device_name in table
        final List<DeviceAccountPair> deviceAccountPairs = deviceDAO.getAccountIdsForDeviceId(deviceName);

        if(deviceAccountPairs.isEmpty()) {
            LOGGER.warn("No account found for device_id: {}", batch.getDeviceId());
            LOGGER.warn("{} needs to be registered", batch.getDeviceId());
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Bad Request").build());
        }

        // TODO: maybe refactor the protobuf to have a more sensible structure?
        for(final InputProtos.SimpleSensorBatch.SimpleSensorSample sample : batch.getSamplesList()) {

            final int offsetMillis = sample.getOffsetMillis();

            if(sample.hasDeviceData()) {
                byte[] deviceData = sample.getDeviceData().toByteArray();

                final InputStream inputStream = new ByteArrayInputStream(deviceData);
                final LittleEndianDataInputStream dataInputStream = new LittleEndianDataInputStream(inputStream);

                int temp, light, humidity, airQuality;
                DateTime roundedDateTime;

                try {
                    roundedDateTime = new DateTime(dataInputStream.readLong(), DateTimeZone.UTC).withSecondOfMinute(0);

                    temp = dataInputStream.readInt();
                    light = dataInputStream.readInt();
                    humidity = dataInputStream.readInt();
                    airQuality = dataInputStream.readInt();
                }catch(IOException e){
                    LOGGER.error(e.getMessage());
                    throw new WebApplicationException(Response.serverError().entity("Failed parsing device data").build());
                }finally{
                    try {
                        dataInputStream.close();
                    } catch (IOException ioException) {
                        LOGGER.warn("Could not close LittleEndianInputStream. Investigate.");
                    }
                }

                for (final DeviceAccountPair pair : deviceAccountPairs) {
                    final DeviceData.Builder builder = new DeviceData.Builder()
                            .withAccountId(pair.accountId)
                            .withDeviceId(pair.internalDeviceId)
                            .withAmbientTemperature(temp)
                            .withAmbientAirQuality(airQuality)
                            .withAmbientHumidity(humidity)
                            .withAmbientLight(light)
                            .withOffsetMillis(offsetMillis)
                            .withDateTimeUTC(roundedDateTime);

                    final DeviceData data = builder.build();

                    try {
                        deviceDataDAO.insert(data);
                    } catch (UnableToExecuteStatementException exception) {
                        final Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
                        if (!matcher.find()) {
                            LOGGER.error(exception.getMessage());
                            return Response.serverError().build();
                        }

                        LOGGER.warn("Duplicate device sensor value for account_id = {}, time: ",
                                accessToken.accountId, roundedDateTime);

                    }

                }


            }

            saveSoundSample(sample, deviceAccountPairs);
        }


        return Response.ok().build();
    }

    private void saveSoundSample(final SimpleSensorBatch.SimpleSensorSample sample, final List<DeviceAccountPair> deviceAccountPairs) {
        if(sample.hasSoundAmplitude()) {
            final Long sampleTimestamp = sample.getTimestamp();
            final DateTime dateTimeSample = new DateTime(sampleTimestamp, DateTimeZone.UTC);
            final Integer offsetMillis = sample.getOffsetMillis();

            for(final DeviceAccountPair pair : deviceAccountPairs) {
                try {
                    deviceDataDAO.insertSound(pair.internalDeviceId, sample.getSoundAmplitude(), dateTimeSample, offsetMillis);
                } catch (UnableToExecuteStatementException exception) {
                    Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
                    if (!matcher.find()) {
                        LOGGER.error(exception.getMessage());
                        return;
                    }
                    LOGGER.warn("Duplicate sound entry for {} with ts = {} and account_id = {}", pair.internalDeviceId, dateTimeSample, pair.accountId);
                }
            }
        }
    }
}
