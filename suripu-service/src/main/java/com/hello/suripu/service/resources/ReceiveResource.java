package com.hello.suripu.service.resources;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.base.Optional;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.api.input.InputProtos.SimpleSensorBatch;
import com.hello.suripu.core.configuration.QueueNames;
import com.hello.suripu.core.crypto.CryptoHelper;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.PublicKeyStore;
import com.hello.suripu.core.db.ScoreDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.TempTrackerData;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
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
import java.io.InputStreamReader;
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

    private final DeviceDataDAO deviceDataDAO;
    private final DeviceDAO deviceDAO;
    private final ScoreDAO scoreDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final PublicKeyStore publicKeyStore;

    private final KinesisLoggerFactory kinesisLoggerFactory;
    private final CryptoHelper cryptoHelper;

    public ReceiveResource(final DeviceDataDAO deviceDataDAO,
                           final DeviceDAO deviceDAO,
                           final ScoreDAO scoreDAO,
                           final TrackerMotionDAO trackerMotionDAO,
                           final PublicKeyStore publicKeyStore,
                           final KinesisLoggerFactory kinesisLoggerFactory) {
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;
        this.scoreDAO = scoreDAO;
        this.trackerMotionDAO = trackerMotionDAO;

        this.publicKeyStore = publicKeyStore;
        cryptoHelper = new CryptoHelper();
        this.kinesisLoggerFactory = kinesisLoggerFactory;
    }


    @PUT
    @Timed
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    public Response receiveDevicePayload(@Valid SimpleSensorBatch batch) {

        final Optional<byte[]> optionalPublicKeyBase64Encoded = publicKeyStore.get(batch.getDeviceId());
        if(!optionalPublicKeyBase64Encoded.isPresent()) {
            LOGGER.warn("Public key does not exist");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        final byte[] publicKeyBase64Encoded = optionalPublicKeyBase64Encoded.get();


        // TODO: agree on which part of the data is signed
        final boolean verified = cryptoHelper.validate(
                batch.getSamples(0).getDeviceData().toByteArray(),
                batch.getSamples(0).getDeviceDataSignature().toByteArray(),
                publicKeyBase64Encoded
        );

        if(!verified) {
            // TODO: make distinction server error and malformed request?
            // TODO: let's not give potential attackers too much information
            return Response.serverError().build();
        }

        return Response.ok().build();
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


        final List<DeviceAccountPair> pairs = trackerMotionDAO.getTrackerIds(accessToken.accountId);

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

            final DateTime originalDateTime = new DateTime(tempTrackerData.timestamp, DateTimeZone.UTC);
            int offsetMillis = -25200000;

            final DateTime roundedDateTimeUTC = new DateTime(
                    originalDateTime.getYear(),
                    originalDateTime.getMonthOfYear(),
                    originalDateTime.getDayOfMonth(),
                    originalDateTime.getHourOfDay(),
                    originalDateTime.getMinuteOfHour(),
                    DateTimeZone.UTC
            );

            // Query tracker / user

            final TrackerMotion trackerMotion = new TrackerMotion(
                    0,
                    accessToken.accountId,
                    trackerId,
                    roundedDateTimeUTC.getMillis(),
                    tempTrackerData.value,
                    offsetMillis
            );


            try {
                final Long id = trackerMotionDAO.insertTrackerMotion(trackerMotion);
            } catch (UnableToExecuteStatementException exception) {
                Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
                if (!matcher.find()) {
                    LOGGER.error(exception.getMessage());
                    throw new WebApplicationException(Response.serverError().build());
                }

                LOGGER.warn("Duplicate sensor value for account_id = {}", accessToken.accountId);

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
        final DataLogger dataLogger = kinesisLoggerFactory.get(QueueNames.PILL_DATA);

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
    @Path("/morpheus/pb")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Timed
    public void morpheusProtobufReceive(byte[] body) {

        InputProtos.periodic_data data = null;

        try {
            data = InputProtos.periodic_data.parseFrom(body);
        } catch (IOException exception) {
            LOGGER.error("Failed parsing protobuf: {}", exception.getMessage());
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("bad request").type(MediaType.TEXT_PLAIN_TYPE).build());
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
