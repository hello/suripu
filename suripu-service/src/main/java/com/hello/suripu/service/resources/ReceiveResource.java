package com.hello.suripu.service.resources;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.io.LittleEndianDataInputStream;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.api.input.InputProtos.SimpleSensorBatch;
import com.hello.suripu.core.crypto.CryptoHelper;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.PublicKeyStore;
import com.hello.suripu.core.db.ScoreDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrackerMotionDAODynamoDB;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.KinesisLogger;
import com.hello.suripu.core.models.TempTrackerData;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.service.db.DataExtractor;
import com.hello.suripu.service.db.DeviceDataDAO;
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
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
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
    private final TrackerMotionDAODynamoDB trackerMotionDAODynamoDB;
    private final PublicKeyStore publicKeyStore;

    private final KinesisLogger kinesisLogger;
    private final CryptoHelper cryptoHelper;

    public ReceiveResource(final DeviceDataDAO deviceDataDAO,
                           final DeviceDAO deviceDAO,
                           final ScoreDAO scoreDAO,
                           final TrackerMotionDAO trackerMotionDAO,
                           final TrackerMotionDAODynamoDB trackerMotionDAODynamoDB,
                           final PublicKeyStore publicKeyStore,
                           final KinesisLogger kinesisLogger) {
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;
        this.scoreDAO = scoreDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.trackerMotionDAODynamoDB = trackerMotionDAODynamoDB;
        this.publicKeyStore = publicKeyStore;
        cryptoHelper = new CryptoHelper();
        this.kinesisLogger = kinesisLogger;
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

        final HashSet<DateTime> datesInUploadData = new HashSet<DateTime>();

        for(TempTrackerData tempTrackerData : trackerData) {
            final DateTime originalDateTime = new DateTime(tempTrackerData.timestamp, DateTimeZone.UTC);
            int offsetMillis = -25200000;

            // Get back the local time.
            final DateTime localTime = new DateTime(tempTrackerData.timestamp, DateTimeZone.forOffsetMillis(offsetMillis));
            final DateTime localStartOfDay = localTime.withTimeAtStartOfDay();

            datesInUploadData.add(localStartOfDay);


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

        // Okay, now we get all the dates that updated by this upload, let's sync them into DynamoDB.
        final LinkedList<TrackerMotion> dataToBeSync = new LinkedList<TrackerMotion>();

        for(final DateTime date:datesInUploadData){
            final DateTime startQueryTimestamp = date.withTimeAtStartOfDay();
            final DateTime endQueryTimestamp = startQueryTimestamp.plusHours(23).plusMinutes(59).plusSeconds(59).plusMillis(999);

            final ImmutableList<TrackerMotion> dataForThatDay = this.trackerMotionDAO.getBetween(
                    accessToken.accountId,
                    new DateTime(startQueryTimestamp.getMillis(), DateTimeZone.UTC),
                    new DateTime(endQueryTimestamp.getMillis(), DateTimeZone.UTC)
            );

            dataToBeSync.addAll(dataForThatDay);
        }

        try {
            this.trackerMotionDAODynamoDB.setTrackerMotions(accessToken.accountId, dataToBeSync);
        }catch (AmazonServiceException ase){
            LOGGER.error("Sync data to DynamoDB failed {}", ase.getErrorMessage());
            return Response.serverError().build();
        }

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

        kinesisLogger.put(batch.getDeviceId(), batch.toByteArray());

        final List<DeviceAccountPair> deviceAccountPairs = deviceDAO.getAccountIdsForDeviceId(batch.getDeviceId());

        if(deviceAccountPairs.isEmpty()) {
            LOGGER.warn("No account found for device_id: {}", batch.getDeviceId());
            LOGGER.warn("{} needs to be registered", batch.getDeviceId());
            return Response.status(Response.Status.BAD_REQUEST).entity("Bad Request").type(MediaType.TEXT_PLAIN_TYPE).build();
        }

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
                    return Response.serverError().entity("Failed parsing device data").build();
                } finally {
                    try {
                        dataInputStream.close();
                    } catch (IOException ioException) {
                        LOGGER.warn("Could close LittleEndianInputStream. Investigate.");
                    }
                }

                LOGGER.debug("ts = {}", timestamp);
                final DateTime dateTime = new DateTime(timestamp, DateTimeZone.UTC);
                final DateTime rounded = new DateTime(
                        dateTime.getYear(),
                        dateTime.getMonthOfYear(),
                        dateTime.getDayOfMonth(),
                        dateTime.getHourOfDay(),
                        dateTime.getMinuteOfHour(),
                        DateTimeZone.UTC
                );

                for(DeviceAccountPair pair : deviceAccountPairs) {
                    try {
                        // TODO: FINAL VERSION WILL HAVE TO QUERY FROM DB
                        deviceDataDAO.insert(pair.internalDeviceId, pair.accountId, rounded, offsetMillis, temp, light, humidity, airQuality);

                    } catch (UnableToExecuteStatementException exception) {
                        Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
                        if (!matcher.find()) {
                            LOGGER.error(exception.getMessage());
                            return Response.serverError().build();
                        }

                        LOGGER.warn("Duplicate entry for {} ({}) with ts = {} and account_id = {}",
                                pair.internalDeviceId,
                                batch.getDeviceId(),
                                rounded,
                                pair.accountId
                        );
                    }
                }
            }


            if(sample.hasSoundAmplitude()) {
                final Long sampleTimestamp = sample.getTimestamp();
                final DateTime dateTimeSample = new DateTime(sampleTimestamp, DateTimeZone.UTC);
                for(DeviceAccountPair pair : deviceAccountPairs) {
                    try {
                        deviceDataDAO.insertSound(pair.internalDeviceId, sample.getSoundAmplitude(), dateTimeSample, offsetMillis);
                        LOGGER.debug("Sound timestamp = {}", sampleTimestamp);
                    } catch (UnableToExecuteStatementException exception) {
                        Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
                        if (!matcher.find()) {
                            LOGGER.error(exception.getMessage());
                            return Response.serverError().build();
                        }
                        LOGGER.warn("Duplicate sound entry for {} with ts = {} and account_id = {}", pair.internalDeviceId, dateTimeSample, pair.accountId);
                    }
                }
            }

        }

        return Response.ok().build();
    }
}
