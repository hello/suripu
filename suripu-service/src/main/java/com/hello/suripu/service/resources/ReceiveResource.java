package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.google.common.io.LittleEndianDataInputStream;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.service.db.EventDAO;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/in")
public class ReceiveResource {

    private static final Pattern PG_UNIQ_PATTERN = Pattern.compile("ERROR: duplicate key value violates unique constraint \"(\\w+)\"");
    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiveResource.class);

    private final EventDAO eventDAO;
    private final DeviceDAO deviceDAO;

    public ReceiveResource(final EventDAO eventDAO, final DeviceDAO deviceDAO) {
        this.eventDAO = eventDAO;
        this.deviceDAO = deviceDAO;
    }

    @POST
    @Timed
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    public Response receiveSimpleData(
            @Valid InputProtos.SimpleSensorBatch batch,
            @Scope({OAuthScope.SENSORS_BASIC}) AccessToken accessToken) {


        final Optional<Long> deviceIdOptional = deviceDAO.getDeviceForAccountId(batch.getDeviceId());
        if(!deviceIdOptional.isPresent()) {
            LOGGER.warn("DeviceId: {} was not found", batch.getDeviceId());
            return Response.status(Response.Status.BAD_REQUEST).entity("Bad Request").type(MediaType.TEXT_PLAIN_TYPE).build();
        }
        for(InputProtos.SimpleSensorBatch.SimpleSensorSample sample : batch.getSamplesList()) {


            byte[] deviceData = sample.getDeviceData().toByteArray();
            // TODO: check for length and do not parse if payload has no device data

            final InputStream inputStream = new ByteArrayInputStream(deviceData);
            final LittleEndianDataInputStream dataInputStream = new LittleEndianDataInputStream(inputStream);

            final int offsetMillis = 0;


            int temp, light, humidity, airQuality; // Should be double?
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
            }

            LOGGER.debug("ts = {}", timestamp);
            final DateTime dateTime = new DateTime(timestamp, DateTimeZone.UTC);
            final DateTime rounded = new DateTime(
                    dateTime.getYear(),
                    dateTime.getMonthOfYear(),
                    dateTime.getDayOfMonth(),
                    dateTime.getHourOfDay(),
                    dateTime.getMinuteOfHour(),
                    DateTimeZone.UTC // The two time zone need to be aligned.
            );

            try {
                eventDAO.insert(deviceIdOptional.get(), rounded, offsetMillis, temp, light, humidity, airQuality);
            } catch (UnableToExecuteStatementException exception) {
                Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
                if (!matcher.find()) {
                    LOGGER.error(exception.getMessage());
                    return Response.serverError().build();
                }
                LOGGER.warn("Duplicate entry for {} with ts = {}", deviceIdOptional.get(), rounded);
            }
        }

        return Response.ok().build();
    }
}
