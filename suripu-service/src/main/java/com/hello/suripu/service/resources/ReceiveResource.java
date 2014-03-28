package com.hello.suripu.service.resources;

import com.google.common.io.LittleEndianDataInputStream;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.api.input.InputProtos;
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

    public ReceiveResource(EventDAO eventDAO) {
        this.eventDAO = eventDAO;
    }

    @POST
    @Timed
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    public Response receiveSimpleData(@Valid InputProtos.SimpleSensorBatch batch) {

        for(InputProtos.SimpleSensorBatch.SimpleSensorSample sample : batch.getSamplesList()) {
            final Long deviceId = Long.parseLong(batch.getDeviceId());

            byte[] deviceData = sample.getDeviceData().toByteArray();
            // TODO: check for length and do not parse if payload has no device data

            final InputStream inputStream = new ByteArrayInputStream(deviceData);
            final LittleEndianDataInputStream dataInputStream = new LittleEndianDataInputStream(inputStream); // Shall we consider the endian?

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
                    dateTime.getMinuteOfHour()
            );

            try {
                eventDAO.insert(deviceId, rounded, temp, light, humidity, airQuality);
            } catch (UnableToExecuteStatementException exception) {
                Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
                if (!matcher.find()) {
                    LOGGER.error(exception.getMessage());
                    return Response.serverError().build();
                }
                LOGGER.warn("Duplicate entry for {} with ts = {}", deviceId, rounded);
            }
        }

        return Response.ok().build();
    }
}
