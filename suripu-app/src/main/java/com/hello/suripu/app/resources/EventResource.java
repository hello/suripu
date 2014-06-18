package com.hello.suripu.app.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.db.EventDAODynamoDB;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 6/16/14.
 */
@Path("/events")
public class EventResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventResource.class);
    private final EventDAODynamoDB eventDAODynamoDB;

    public EventResource(final EventDAODynamoDB eventDAODynamoDB){
        this.eventDAODynamoDB = eventDAODynamoDB;
    }

    @Timed
    @GET
    @Path("/{target_date_string}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Event> getEvents(@Scope({OAuthScope.API_INTERNAL_DATA_READ}) final AccessToken token,
                                 @PathParam("target_date_string") final String targetDateString){

        final DateTime targetDate = DateTime.parse(targetDateString, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT));
        validateTargetDate(targetDate);

        final List<Event> events = this.eventDAODynamoDB.getEventsForDate(token.accountId, targetDate);
        return events;
    }


    @Timed
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<Event>> getEvents(@Scope({OAuthScope.API_INTERNAL_DATA_READ}) final AccessToken accessToken,
                                              final List<String> dateStrings){
        final ArrayList<DateTime> targetDates = new ArrayList<DateTime>();
        for (final String dateString:dateStrings){
            final DateTime targetDate = DateTime.parse(dateString, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT));
            targetDates.add(targetDate);
        }

        validateTargetDates(targetDates);

        final ImmutableMap<DateTime, ImmutableList<Event>> dateEventsMap = this.eventDAODynamoDB.getEventsForDates(accessToken.accountId, targetDates);
        final HashMap<String, List<Event>> results = new HashMap<String, List<Event>>();
        for(final DateTime targetDate:dateEventsMap.keySet()){
            results.put(targetDate.toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT), dateEventsMap.get(targetDate));
        }

        return results;

    }

    /**
     * Validate that the target date is withing acceptable date range
     * @param targetDate
     */
    private void validateTargetDate(final DateTime targetDate) {
        if(targetDate.getMillis() > targetDate.plusDays(2).getMillis() || targetDate.getMillis() < DateTimeUtil.MORPHEUS_DAY_ONE.getMillis()){
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }
    }

    /**
     * Bulk validation
     * @param targetDates
     */
    private void validateTargetDates(final List<DateTime> targetDates) {
        for(DateTime targetDate : targetDates) {
            validateTargetDate(targetDate);
        }
    }
}
