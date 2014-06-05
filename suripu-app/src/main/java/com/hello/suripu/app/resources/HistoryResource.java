package com.hello.suripu.app.resources;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.app.utils.DataType;
import com.hello.suripu.app.utils.GetTrackerDataResponse;
import com.hello.suripu.core.db.TrackerMotionDAODynamoDB;
import com.hello.suripu.core.db.util.DateTimeFormatString;
import com.hello.suripu.core.models.GroupedRecord;
import com.hello.suripu.core.models.Record;
import com.hello.suripu.core.models.SoundRecord;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.TimeSerieDAO;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/history")
public class HistoryResource {


    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryResource.class);
    private final TimeSerieDAO timeSerieDAO; // Any reason put different things together?
    private final DeviceDAO deviceDAO;
    private final TrackerMotionDAODynamoDB trackerMotionDAODynamoDB;

    public HistoryResource(final TimeSerieDAO timeSerieDAO,
                           final DeviceDAO deviceDAO,
                           final TrackerMotionDAODynamoDB trackerMotionDAODynamoDB) {
        this.timeSerieDAO = timeSerieDAO;
        this.deviceDAO = deviceDAO;
        this.trackerMotionDAODynamoDB = trackerMotionDAODynamoDB;
    }

    @GET
    @Timed
    @Path("/{days}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Record> getRecords(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @PathParam("days") final Integer numDays) {
        LOGGER.debug("asking for {} days of recent history", numDays);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final DateTime then = now.minusDays(60);

        final Optional<Long> deviceId = deviceDAO.getByAccountId(accessToken.accountId);
        LOGGER.debug("Account id = {}", accessToken.accountId);
        if(!deviceId.isPresent()) {
            throw new WebApplicationException(404);
        }
        LOGGER.debug("device = {}", deviceId.get());
        final ImmutableList<Record> records = timeSerieDAO.getHistoricalData(deviceId.get(), then, now);
        LOGGER.debug("Found {} records in DB", records.size());
        return records;
    }

    @Deprecated
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecordsBetween(
            @Scope({OAuthScope.API_INTERNAL_DATA_READ}) final AccessToken accessToken,
            @QueryParam("from") final Long from,
            @QueryParam("to") final Long to,
            @QueryParam("data_type") final DataType type) {

        if(from == null || to == null){
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if(Math.abs(to - from) > 3 * 24 * 60 * 60 * 1000){
            // Just don't allow a big query
            return Response.status(Response.Status.BAD_REQUEST).build();
        }


        switch (type){
            case MOTION:
                final ImmutableList<TrackerMotion> trackerMotions = this.timeSerieDAO.getTrackerDataBetween(accessToken.accountId,
                        new DateTime(from, DateTimeZone.UTC),
                        new DateTime(to, DateTimeZone.UTC));
                return Response.ok().entity(trackerMotions).build();
            case SOUND:

                // FIXME: The device Id is no longer needed, should query everything based on account id.
                final Optional<Long> deviceId = deviceDAO.getByAccountId(accessToken.accountId);
                if(!deviceId.isPresent()) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }

                final ImmutableList<SoundRecord> soundRecords = this.timeSerieDAO.getSoundDataBetween(deviceId.get(),
                        new DateTime(from, DateTimeZone.UTC),
                        new DateTime(to, DateTimeZone.UTC));
                return Response.ok().entity(soundRecords).build();
            default:
                return Response.status(Response.Status.BAD_REQUEST).build();

        }

    }


    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public GetTrackerDataResponse getTrackerDataForDates(@Valid final List<String> dateStrings, @Scope({OAuthScope.API_INTERNAL_DATA_READ}) final AccessToken accessToken) {

        if(dateStrings.size() > 31 || dateStrings.size() == 0){
            // Just don't allow a big query
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }

        final Map<DateTime, String> dateToStringMapping = new HashMap<DateTime, String>();
        for(final String dateString:dateStrings){
            try {
                dateToStringMapping.put(DateTime.parse(dateString, DateTimeFormat.forPattern(DateTimeFormatString.FORMAT_TO_DAY)), dateString);
            }catch (IllegalArgumentException iae){
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
            }
        }

        try {
            // Now we verified that the params passed by user are valid.
            final ImmutableMap<DateTime, List<TrackerMotion>> result = this.trackerMotionDAODynamoDB.getTrackerMotionForDates(accessToken.accountId, dateToStringMapping.keySet());
            final Map<String, List<TrackerMotion>> convertedResult = new HashMap<String, List<TrackerMotion>>();
            for (final DateTime date : result.keySet()) {
                if (dateToStringMapping.containsKey(date)) {
                    convertedResult.put(dateToStringMapping.get(date), result.get(date));
                }
            }

            final GetTrackerDataResponse response = new GetTrackerDataResponse(convertedResult);
            return response;
        }catch (AmazonServiceException ase){
            LOGGER.error("Amazon Service error: {}", ase.getErrorMessage());
            throw new WebApplicationException(Response.serverError().build());
        }

    }


    @GET
    @Timed
    @Path("/grouped/{num_days}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupedSensorResults(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken token,
            @PathParam("num_days") final Integer numDays) {

        Optional<Long> optionalDeviceId = deviceDAO.getByAccountId(token.accountId);
        if(!optionalDeviceId.isPresent()) {
            LOGGER.warn("Device not found for account = {}", token.accountId);
            return Response.status(Response.Status.NOT_FOUND).entity("not found").type(MediaType.TEXT_PLAIN).build();
        }

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final DateTime then = now.minusDays(numDays);

        final ImmutableList<Record> records = timeSerieDAO.getHistoricalData(optionalDeviceId.get(), then, now);

        final GroupedRecord groupedRecord = GroupedRecord.fromRecords(records);
        return Response.ok().entity(groupedRecord).build();
    }
}
