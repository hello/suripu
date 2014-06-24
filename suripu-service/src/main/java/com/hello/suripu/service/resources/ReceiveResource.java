package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.protobuf.ByteString;
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
import com.hello.suripu.core.models.BatchSensorData;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.TempTrackerData;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.service.db.DataExtractor;
import com.yammer.metrics.annotation.Timed;
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
import java.util.ArrayList;
import java.util.List;
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
    @Path("/tracker")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    public Response receiveTrackerData(
            @Valid InputProtos.TrackerDataBatch batch,
            @Scope({OAuthScope.SENSORS_BASIC}) AccessToken accessToken){

        if(batch.getSamplesCount() == 0){
            LOGGER.warn("Empty payload.");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }


        for(final InputProtos.TrackerDataBatch.TrackerData datum:batch.getSamplesList()){

            try{
                DataExtractor.normalizeAndSave(datum, accessToken, trackerMotionDAO);
            } catch (UnableToExecuteStatementException exception) {
                Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());

                if(!matcher.find()) {
                    LOGGER.error(exception.getMessage());
                    return Response.serverError().build();
                }

                LOGGER.warn("Duplicate tracker data for account {}, tracker {} with ts = {}",
                        accessToken.accountId,
                        datum.getTrackerId(),
                        new DateTime(datum.getTimestamp(), DateTimeZone.forOffsetMillis(datum.getOffsetMillis()))
                        );
            }


        }

        return Response.ok().build();


    }

    @POST
    @Timed
    @Path("/temp/tracker")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public Response sendTempData(
            @Valid List<TempTrackerData> trackerData,
            @Scope({OAuthScope.API_INTERNAL_DATA_WRITE}) AccessToken accessToken) {

        if(trackerData.size() == 0){
            LOGGER.info("Account {} tries to upload empty payload.", accessToken.accountId);
            return Response.ok().build();
        }


        for(final TempTrackerData tempTrackerData : trackerData) {
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

            try {
                final Long id = trackerMotionDAO.insertTrackerMotion(accessToken.accountId,
                        tempTrackerData.trackerId,
                        tempTrackerData.value,
                        roundedDateTimeUTC,
                        offsetMillis // OH YEAH THIS IS CALIFORNIA. OBVIOUSLY NOT VALID FOR ANYONE OUTSIDE THE OFFICE
                );
            } catch (UnableToExecuteStatementException exception) {
                Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
                if (!matcher.find()) {
                    LOGGER.error(exception.getMessage());
                    return Response.serverError().build();
                }

                LOGGER.warn("Duplicate sensor value for account_id = {}", accessToken.accountId);
            }

        }

        return Response.ok().build();
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
    @Timed
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    public Response receiveSimpleData(
            @Valid InputProtos.SimpleSensorBatch batch,
            @Scope({OAuthScope.SENSORS_BASIC}) AccessToken accessToken) {

        // the accessToken is only used for upload permission at the moment
        // it will soon be removed and rely on device_id and signature from Morpheus
        // TODO: make transition from access token to signature based happen.

        final List<DeviceAccountPair> deviceAccountPairs = deviceDAO.getAccountIdsForDeviceId(batch.getDeviceId());

        if(deviceAccountPairs.isEmpty()) {
            LOGGER.warn("No account found for device_id: {}", batch.getDeviceId());
            LOGGER.warn("{} needs to be registered", batch.getDeviceId());
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Bad Request").build());
        }

        final ArrayList<Integer> tempSamples = new ArrayList<Integer>(batch.getSamplesCount());
        final ArrayList<Integer> lightSamples = new ArrayList<Integer>(batch.getSamplesCount());
        final ArrayList<Integer> humiditySamples = new ArrayList<Integer>(batch.getSamplesCount());
        final ArrayList<Integer> airQualitySamples = new ArrayList<Integer>(batch.getSamplesCount());

        final ArrayList<Long> timestamps = new ArrayList<Long>(batch.getSamplesCount());
        final ArrayList<Integer> offsetMillisSamples = new ArrayList<Integer>(batch.getSamplesCount());


        // TODO: maybe refactor the protobuf to have a more sensible structure?
        for(InputProtos.SimpleSensorBatch.SimpleSensorSample sample : batch.getSamplesList()) {

            final int offsetMillis = sample.getOffsetMillis();

            if(sample.hasDeviceData()) {


                byte[] deviceData = sample.getDeviceData().toByteArray();

                final InputStream inputStream = new ByteArrayInputStream(deviceData);
                final LittleEndianDataInputStream dataInputStream = new LittleEndianDataInputStream(inputStream);

                int temp, light, humidity, airQuality;
                long timestamp;

                try {
                    timestamp = dataInputStream.readLong();
                    LOGGER.debug("Device timestamp = {}", timestamp);
                    temp = dataInputStream.readInt();
                    light = dataInputStream.readInt();
                    humidity = dataInputStream.readInt();
                    airQuality = dataInputStream.readInt();

                } catch (IOException e) {
                    LOGGER.error(e.getMessage());
                    throw new WebApplicationException(Response.serverError().entity("Failed parsing device data").build());
                } finally {
                    try {
                        dataInputStream.close();
                    } catch (IOException ioException) {
                        LOGGER.warn("Could not close LittleEndianInputStream. Investigate.");
                    }
                }

                tempSamples.add(temp);
                lightSamples.add(light);
                humiditySamples.add(humidity);
                airQualitySamples.add(airQuality);
                timestamps.add(timestamp);
                offsetMillisSamples.add(offsetMillis);
            }

            saveSoundSample(sample, deviceAccountPairs);
        }

        final DateTime dateTime = new DateTime(timestamps.get(0), DateTimeZone.UTC);
        final DateTime rounded = new DateTime(
                dateTime.getYear(),
                dateTime.getMonthOfYear(),
                dateTime.getDayOfMonth(),
                dateTime.getHourOfDay(),
                dateTime.getMinuteOfHour(),
                DateTimeZone.UTC
        );

        final BatchSensorData deviceBatch = new BatchSensorData.Builder()
                .withAccountId(accessToken.accountId)
                .withDeviceId(batch.getDeviceId())
                .withAmbientTemp(tempSamples)
                .withAmbientAirQuality(airQualitySamples)
                .withAmbientHumidity(humiditySamples)
                .withAmbientLight(lightSamples)
                .withDateTime(rounded)
                .withOffsetMillis(offsetMillisSamples.get(0))
                .build();

        try {
            deviceDataDAO.insertBatch(deviceBatch);
        } catch (UnableToExecuteStatementException exception) {
            final Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
            if (!matcher.find()) {
                LOGGER.error(exception.getMessage());
                return Response.serverError().build();
            }
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
                    LOGGER.debug("Sound timestamp = {}", sampleTimestamp);
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
