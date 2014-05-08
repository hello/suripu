package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.google.common.io.LittleEndianDataInputStream;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.api.input.InputProtos.SimpleSensorBatch;
import com.hello.suripu.core.Event;
import com.hello.suripu.core.Score;
import com.hello.suripu.core.TrackerMotion;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.EventDAO;
import com.hello.suripu.core.db.PublicKeyStore;
import com.hello.suripu.core.db.ScoreDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.service.Util;
import com.hello.suripu.service.db.DataExtractor;
import com.hello.suripu.service.db.DeviceDataDAO;
import com.sun.jersey.core.util.Base64;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
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
    private final PublicKeyStore publicKeyStore;
    private final EventDAO eventDAO;

    public ReceiveResource(final DeviceDataDAO deviceDataDAO,
                           final DeviceDAO deviceDAO,
                           final ScoreDAO scoreDAO,
                           final TrackerMotionDAO trackerMotionDAO,
                           final PublicKeyStore publicKeyStore,
                           final EventDAO eventDAO) {
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;
        this.scoreDAO = scoreDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.publicKeyStore = publicKeyStore;
        this.eventDAO = eventDAO;
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

        final X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decode(publicKeyBase64Encoded));
        try {
            final KeyFactory kf = KeyFactory.getInstance("RSA");
            final PublicKey publicKeyFromDataStore = kf.generatePublic(spec);

            final Signature signature = Signature.getInstance("SHA512WithRSA");
            signature.initVerify(publicKeyFromDataStore);
            // TODO : agree on device data that is signed;
            signature.update(batch.getSamples(0).getDeviceData().toByteArray());

            if(!signature.verify(batch.getSamples(0).getDeviceDataSignature().toByteArray())) {
                System.out.println("Did not recognize the signature bailing");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            return Response.ok().build();

        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("{}", e);
        } catch (InvalidKeySpecException e) {
            LOGGER.error("{}", e);
        } catch (SignatureException e) {
            LOGGER.error("{}", e);
        } catch (InvalidKeyException e) {
            LOGGER.error("{}", e);
        }

        return Response.serverError().build();
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

        final List<TrackerMotion> trackerMotions = trackerMotionDAO.getLast(10, accessToken.accountId);
        final InputProtos.TrackerDataBatch.TrackerData firstUploadedData = batch.getSamples(0);
        final LinkedList<TrackerMotion> buffer = new LinkedList<TrackerMotion>();
        double average = 0.0;

        if(trackerMotions.size() > 0) {
            final DateTime roundedTimeForFirstUploadedData = Util.roundTimestampToMinuteUTC(firstUploadedData.getTimestamp());
            if (roundedTimeForFirstUploadedData.getMillis() - trackerMotions.get(0).dateTime.getMillis() <
                    10 * 60 * 1000) {
                for(int i = trackerMotions.size() - 1; i >= 0; i--) {
                    buffer.add(trackerMotions.get(i));
                }
            }

        }



        for(InputProtos.TrackerDataBatch.TrackerData datum:batch.getSamplesList()){
            DateTime roundedTimeUTC = Util.roundTimestampToMinuteUTC(datum.getTimestamp());
            if(buffer.size() > 0){
                average = Util.getAverageSVM(buffer);  // Don't blame me, I know this is 10 times slower.

                try {
                    if (average > 0) {
                        if (datum.getSvmNoGravity() > average * 2d) {
                            // TODO: add event to DB
                            this.eventDAO.create(accessToken.accountId,
                                    Event.Type.MOTION.getValue(),
                                    roundedTimeUTC,
                                    roundedTimeUTC.plusMinutes(1),
                                    datum.getOffsetMillis());
                        }
                    } else {
                        if (datum.getSvmNoGravity() > average / 2d) {
                            // TODO: add event to DB
                            this.eventDAO.create(accessToken.accountId,
                                    Event.Type.MOTION.getValue(),
                                    roundedTimeUTC,
                                    roundedTimeUTC.plusMinutes(1),
                                    datum.getOffsetMillis());
                        }

                    }
                }catch (UnableToExecuteStatementException ex){
                    Matcher matcher = PG_UNIQ_PATTERN.matcher(ex.getMessage());

                    if(!matcher.find()) {
                        LOGGER.error(ex.getMessage());
                    }else {

                        LOGGER.warn("Duplicate event for account {}, type {} with start time = {}",
                                accessToken.accountId,
                                Event.Type.MOTION,
                                roundedTimeUTC);
                    }
                }
            }



            try{
                TrackerMotion trackerMotion = DataExtractor.normalizeAndSave(datum, accessToken, trackerMotionDAO);

                buffer.add(trackerMotion);
                if(buffer.size() > 10){
                    buffer.removeFirst();
                }

            } catch (UnableToExecuteStatementException exception) {
                Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());

                if(!matcher.find()) {
                    LOGGER.error(exception.getMessage());
                    return Response.serverError().build();
                }

                LOGGER.warn("Duplicate entry for account {}, tracker {} with ts = {}",
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
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    public Response receiveSimpleData(
            @Valid InputProtos.SimpleSensorBatch batch,
            @Scope({OAuthScope.SENSORS_BASIC}) AccessToken accessToken) {

        // TODO : remove this after alpha testing

        try {
            deviceDAO.registerDevice(accessToken.accountId, batch.getDeviceId());
        } catch (UnableToExecuteStatementException exception) {
            Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
            if (!matcher.find()) {
                LOGGER.error(exception.getMessage());
                return Response.serverError().build();
            }
            LOGGER.warn("Duplicate entry for account_id: {} with device_id = {}", accessToken.accountId, batch.getDeviceId());
        }

        // TODO : END REMOVE



        final Optional<Long> deviceIdOptional = deviceDAO.getDeviceForAccountId(accessToken.accountId, batch.getDeviceId());
        if(!deviceIdOptional.isPresent()) {
            LOGGER.warn("DeviceId: {} was not found", batch.getDeviceId());
            return Response.status(Response.Status.BAD_REQUEST).entity("Bad Request").type(MediaType.TEXT_PLAIN_TYPE).build();
        }

        final ArrayList<Integer> lightSamples = new ArrayList<Integer>(batch.getSamplesCount());
        final ArrayList<Integer> tempSamples = new ArrayList<Integer>(batch.getSamplesCount());
        final ArrayList<Integer> humiditySamples = new ArrayList<Integer>(batch.getSamplesCount());
        final ArrayList<Integer> airQualitySamples = new ArrayList<Integer>(batch.getSamplesCount());

        final ArrayList<DateTime> dateTimes = new ArrayList<DateTime>();

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
                    LOGGER.debug("timestamp = {}", timestamp);
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

                try {
                    deviceDataDAO.insert(deviceIdOptional.get(), rounded, offsetMillis, temp, light, humidity, airQuality);
                } catch (UnableToExecuteStatementException exception) {
                    Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
                    if (!matcher.find()) {
                        LOGGER.error(exception.getMessage());
                        return Response.serverError().build();
                    }
                    LOGGER.warn("Duplicate entry for {} with ts = {}", deviceIdOptional.get(), rounded);
                }

                // TODO: refactor this, this is ugly
                lightSamples.add(light);
                tempSamples.add(temp);
                humiditySamples.add(humidity);
                airQualitySamples.add(airQuality);
                dateTimes.add(rounded);
            }


            if(sample.hasSoundAmplitude()) {
                final Long sampleTimestamp = sample.getTimestamp();
                final DateTime dateTimeSample = new DateTime(sampleTimestamp, DateTimeZone.UTC);
                try {
                    deviceDataDAO.insertSound(deviceIdOptional.get(), sample.getSoundAmplitude(), dateTimeSample, offsetMillis);
                } catch (UnableToExecuteStatementException exception) {
                    Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
                    if (!matcher.find()) {
                        LOGGER.error(exception.getMessage());
                        return Response.serverError().build();
                    }
                    LOGGER.warn("Duplicate entry for {} with ts = {}", deviceIdOptional.get(), dateTimeSample);
                }
            }

        }


        int lightScore = scoreLight(lightSamples);
        int tempScore = scoreTemperatures(tempSamples);
        int humidityScore = scoreHumidity(humiditySamples);
        int airQualityScore = scoreAirQuality(airQualitySamples);

        final Score score = new Score.Builder()
                .withAccountId(accessToken.accountId)
                .withLight(lightScore)
                .withTemperature(tempScore)
                .withHumidity(humidityScore)
                .withAirQuality(airQualityScore)
                .withSound(0)
                .build();
        scoreDAO.insertScore(score);

        return Response.ok().build();
    }

    private int scoreLight(List<Integer> items) {
        // TODO : REAL SCORING
        if(items.isEmpty()) {
            return 0;
        }

        int sum = 0;
        for(Integer i : items) {
            sum += i;
        }

        return Math.round(sum / items.size());
    }

    private int scoreHumidity(List<Integer> items) {
        // TODO : REAL SCORING
        if(items.isEmpty()) {
            return 0;
        }

        int sum = 0;
        for(Integer i : items) {
            sum += i;
        }

        return Math.round(sum / items.size());
    }

    private int scoreAirQuality(List<Integer> items) {
        // TODO : REAL SCORING
        if(items.isEmpty()) {
            return 0;
        }

        int sum = 0;
        for(Integer i : items) {
            sum += i;
        }

        return Math.round(sum / items.size());
    }

    private int scoreTemperatures(List<Integer> items) {
        // TODO : REAL SCORING
        if(items.isEmpty()) {
            return 0;
        }

        int sum = 0;
        for(Integer i : items) {
            sum += i;
        }

        return Math.round(sum / items.size());
    }
}
